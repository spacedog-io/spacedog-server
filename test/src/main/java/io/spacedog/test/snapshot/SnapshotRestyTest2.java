package io.spacedog.test.snapshot;

import java.net.UnknownHostException;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.file.FileBucket;
import io.spacedog.client.http.SpaceRequest;
import io.spacedog.client.snapshot.SpaceSnapshot;
import io.spacedog.test.SpaceTest;
import io.spacedog.utils.Json;

public class SnapshotRestyTest2 extends SpaceTest {

	private static final String CATS_BUCKET = "cats";
	private static final String DOGS_BUCKET = "dogs";
	private static final ObjectNode MY_SETTINGS = Json.object("size", 6);
	private static final String MY_SETTINGS_ID = "mysettings";

	// @Test
	public void snapshotAndRestoreMultipleTimes() throws InterruptedException, UnknownHostException {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearServer();
		SpaceDog snapshoter = createTempDog(superadmin, "snapshoter");
		superadmin.credentials().setRole(snapshoter.id(), "snapshoter");

		// superadmin creates vince's credentials
		SpaceDog vince = createTempDog(superadmin, "vince");

		// deletes the current repository to force repo creation by this test
		// use full url to avoid delete by mistake any prod repo
		String repositoryId = DateTime.now().withZone(DateTimeZone.UTC).toString("yyyy-ww");
		String ip = "127.0.0.1"; // InetAddress.getLocalHost().getHostAddress();
		SpaceRequest.delete("/_snapshot/{repoId}")//
				.backend("http://" + ip + ":9200")//
				.routeParam("repoId", repositoryId)//
				.go(200, 404);

		// snapshoter snapshots backend
		// returns 202 since wait for completion false by default
		SpaceSnapshot snap1 = snapshoter.snapshots().snapshot();
		assertEquals(snap1.backendId, "spacedog");
		assertEquals(snap1.repositoryId, repositoryId);
		assertTrue(snap1.id.startsWith(snap1.backendId + "-utc-"));

		// superadmin fails to restore latest snapshot since not yet completed
		assertHttpError(400, () -> superadmin.snapshots().restoreLatest());

		// poll and wait for snapshot to complete
		SpaceSnapshot snapshot = null;
		do {
			// superadmin gets latest snapshot status
			snapshot = superadmin.snapshots().getLatest();
			assertEquals(snap1, snapshot);

			// let server work a bit
			Thread.sleep(100);

		} while (!snapshot.state.equalsIgnoreCase("SUCCESS"));

		// snapshoter gets first snapshot by id
		snap1 = snapshoter.snapshots().get(snap1.id);
		assertEquals(snapshot, snap1);
		assertEquals(Sets.newHashSet("spacedog-credentials-0", //
				"spacedog-log-0"), snap1.indices);

		// superadmin creates credentials, settings and file
		SpaceDog fred = createTempDog(superadmin, "fred");
		superadmin.settings().save(MY_SETTINGS_ID, MY_SETTINGS);
		superadmin.files().setBucket(new FileBucket(CATS_BUCKET));
		superadmin.files().upload(CATS_BUCKET, "/felix", "Felix".getBytes());
		superadmin.files().upload(CATS_BUCKET, "/grosminet", "Grosminet".getBytes());

		// superadmin snapshots backend
		SpaceSnapshot snap2 = superadmin.snapshots().snapshot(true);
		assertEquals(repositoryId, snap2.repositoryId);
		assertEquals("SUCCESS", snap2.state);
		assertEquals("spacedog", snap2.backendId);
		assertEquals(Sets.newHashSet("spacedog-credentials-0", "spacedog-log-0", //
				"spacedog-settings-0", "spacedog-files-cats-0"), snap2.indices);

		// superadmin gets all snapshots
		List<SpaceSnapshot> all = superadmin.snapshots().getAll();
		assertEquals(snap2, all.get(0));
		assertEquals(snap1, all.get(1));

		// superadmin creates nath's credentials
		SpaceDog nath = createTempDog(superadmin, "nath");
		superadmin.settings().delete(MY_SETTINGS_ID);
		superadmin.files().delete(CATS_BUCKET, "/felix");
		superadmin.files().setBucket(new FileBucket(DOGS_BUCKET));
		superadmin.files().upload(DOGS_BUCKET, "/milou", "Milou".getBytes());
		superadmin.files().upload(DOGS_BUCKET, "/pluto", "Pluto".getBytes());

		// superadmin snapshots backend
		SpaceSnapshot snap3 = superadmin.snapshots().snapshot(true);
		assertEquals(repositoryId, snap3.repositoryId);
		assertEquals("SUCCESS", snap3.state);
		assertEquals("spacedog", snap3.backendId);
		assertEquals(Sets.newHashSet("spacedog-credentials-0", "spacedog-log-0", //
				"spacedog-settings-0", "spacedog-files-cats-0", "spacedog-files-dogs-0"), snap3.indices);

		// snapshoter gets first and second latest snapshots
		all = snapshoter.snapshots().getAll(0, 2);
		assertEquals(2, all.size());
		assertEquals(snap3, all.get(0));
		assertEquals(snap2, all.get(1));

		// snapshoter gets third latest snapshot
		all = snapshoter.snapshots().getAll(2, 1);
		assertEquals(1, all.size());
		assertEquals(snap1, all.get(0));

		// snapshoter is not authorized to restore
		String firstSnapId = snap1.id;
		assertHttpError(403, () -> snapshoter.snapshots().restoreLatest());
		assertHttpError(403, () -> snapshoter.snapshots().restore(firstSnapId));

		// superadmin restores oldest snapshot
		superadmin.snapshots().restore(snap1.id, true);

		// fred and nath's credentials are gone
		superadmin.login();
		snapshoter.login();
		vince.login();
		assertHttpError(401, () -> fred.login());
		assertHttpError(401, () -> nath.login());
		assertHttpError(404, () -> superadmin.settings().get(MY_SETTINGS_ID));
		assertHttpError(404, () -> superadmin.files().getBucket(DOGS_BUCKET));
		assertHttpError(404, () -> superadmin.files().getAsByteArray(DOGS_BUCKET, "/pluto"));
		assertHttpError(404, () -> superadmin.files().getAsByteArray(DOGS_BUCKET, "/milou"));
		assertHttpError(404, () -> superadmin.files().getBucket(CATS_BUCKET));
		assertHttpError(404, () -> superadmin.files().getAsByteArray(CATS_BUCKET, "/felix"));
		assertHttpError(404, () -> superadmin.files().getAsByteArray(CATS_BUCKET, "/grosminet"));

		// superadmin restores second (middle) snapshot
		superadmin.snapshots().restore(snap2.id, true);

		// fred's credentials are back
		superadmin.login();
		snapshoter.login();
		vince.login();
		fred.login();
		superadmin.settings().get(MY_SETTINGS_ID).equals(MY_SETTINGS);
		assertHttpError(401, () -> nath.login());
		superadmin.files().getBucket(CATS_BUCKET);
		assertArrayEquals("Felix".getBytes(), superadmin.files().getAsByteArray(CATS_BUCKET, "/felix"));
		assertArrayEquals("Grosminet".getBytes(), superadmin.files().getAsByteArray(CATS_BUCKET, "/grosminet"));
		assertHttpError(404, () -> superadmin.files().getBucket(DOGS_BUCKET));
		assertHttpError(404, () -> superadmin.files().getAsByteArray(DOGS_BUCKET, "/pluto"));
		assertHttpError(404, () -> superadmin.files().getAsByteArray(DOGS_BUCKET, "/milou"));

		// superadmin restores latest (third) snapshot
		superadmin.snapshots().restoreLatest(true);

		// all credentials are present
		superadmin.login();
		snapshoter.login();
		vince.login();
		fred.login();
		nath.login();
		assertHttpError(404, () -> superadmin.settings().get(MY_SETTINGS_ID));
		superadmin.files().getBucket(CATS_BUCKET);
		assertHttpError(404, () -> superadmin.files().getAsByteArray(CATS_BUCKET, "/felix"));
		assertArrayEquals("Grosminet".getBytes(), superadmin.files().getAsByteArray(CATS_BUCKET, "/grosminet"));
		superadmin.files().getBucket(DOGS_BUCKET);
		assertArrayEquals("Pluto".getBytes(), superadmin.files().getAsByteArray(DOGS_BUCKET, "/pluto"));
		assertArrayEquals("Milou".getBytes(), superadmin.files().getAsByteArray(DOGS_BUCKET, "/milou"));

		// superdog cleans up all backend indices
		superdog().admin().clearBackend();

		// all credentials are gone
		assertHttpError(401, () -> superadmin.login());
		assertHttpError(401, () -> snapshoter.login());
		assertHttpError(401, () -> vince.login());
		assertHttpError(401, () -> fred.login());
		assertHttpError(401, () -> nath.login());

		// superdog restores latest (third) snapshot
		superdog().snapshots().restoreLatest(true);

		// all credentials are back
		superadmin.login();
		snapshoter.login();
		vince.login();
		fred.login();
		nath.login();

		// superadmin fails to restore if invalid snapshot id format
		assertHttpError(400, () -> superadmin.snapshots().restore("XXXX"));

		// superadmin fails to get snapshot if not found
		assertHttpError(404, () -> superadmin.snapshots().get("all-utc-2011-01-01-00-00-00-000"));

		// superdog cleans up backend
		superdog().admin().clearBackend();

		// superdog gets all snapshots since not part of backend data
		all = superdog().snapshots().getAll();
		assertEquals(3, all.size());
		assertEquals(snap3, all.get(0));
		assertEquals(snap2, all.get(1));
		assertEquals(snap1, all.get(2));
	}
}
