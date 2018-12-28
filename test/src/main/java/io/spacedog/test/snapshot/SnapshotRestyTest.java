package io.spacedog.test.snapshot;

import java.net.UnknownHostException;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.file.FileStoreType;
import io.spacedog.client.http.SpaceBackend;
import io.spacedog.client.snapshot.SpaceRepository;
import io.spacedog.client.snapshot.SpaceSnapshot;
import io.spacedog.test.SpaceTest;
import io.spacedog.utils.Json;

public class SnapshotRestyTest extends SpaceTest {

	private static final ObjectNode MY_SETTINGS = Json.object("size", 6);
	private static final String MY_SETTINGS_ID = "mysettings";

	@Test
	public void snapshotAndRestore() throws InterruptedException, UnknownHostException {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearServer();
		SpaceDog snapman = createTempDog(superadmin, "snapman");
		superadmin.credentials().setRole(snapman.id(), "snapman");
		String currentRepoId = SpaceRepository.currentRepositoryId();

		// superadmin creates vince's credentials
		SpaceDog vince = createTempDog(superadmin, "vince");

		// snapman snapshots backend
		// returns 202 since wait for completion false by default
		SpaceSnapshot snap1 = snapman.snapshots().snapshot();
		assertEquals(SpaceBackend.SPACEDOG, snap1.backendId);
		assertEquals(currentRepoId, snap1.repositoryId);
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

		// snapman gets first snapshot by id
		snap1 = snapman.snapshots().get(snap1.id);
		assertEquals(snapshot, snap1);
		assertEquals(Sets.newHashSet("spacedog-credentials-0", //
				"spacedog-log-0"), snap1.indices);

		// superadmin creates fred's credentials
		SpaceDog fred = createTempDog(superadmin, "fred");
		superadmin.settings().save(MY_SETTINGS_ID, MY_SETTINGS);

		// superadmin snapshots backend
		SpaceSnapshot snap2 = superadmin.snapshots().snapshot(true);
		assertEquals(currentRepoId, snap2.repositoryId);
		assertEquals("SUCCESS", snap2.state);
		assertEquals("spacedog", snap2.backendId);
		assertEquals(Sets.newHashSet("spacedog-credentials-0", //
				"spacedog-log-0", "spacedog-settings-0"), snap2.indices);

		// superadmin gets all snapshots
		List<SpaceSnapshot> all = superadmin.snapshots().getAll();
		assertEquals(snap2, all.get(0));
		assertEquals(snap1, all.get(1));

		// superadmin creates nath's credentials
		SpaceDog nath = createTempDog(superadmin, "nath");
		superadmin.settings().delete(MY_SETTINGS_ID);

		// superadmin snapshots backend
		SpaceSnapshot snap3 = superadmin.snapshots().snapshot(true);
		assertEquals(currentRepoId, snap3.repositoryId);
		assertEquals("SUCCESS", snap3.state);
		assertEquals("spacedog", snap3.backendId);
		assertEquals(Sets.newHashSet("spacedog-credentials-0", //
				"spacedog-log-0", "spacedog-settings-0"), snap2.indices);

		// snapman gets first and second latest snapshots
		all = snapman.snapshots().getAll(0, 2);
		assertEquals(2, all.size());
		assertEquals(snap3, all.get(0));
		assertEquals(snap2, all.get(1));

		// snapman gets third latest snapshot
		all = snapman.snapshots().getAll(2, 1);
		assertEquals(1, all.size());
		assertEquals(snap1, all.get(0));

		// snapman is not authorized to restore
		String firstSnapId = snap1.id;
		assertHttpError(403, () -> snapman.snapshots().restoreLatest());
		assertHttpError(403, () -> snapman.snapshots().restore(firstSnapId));

		// superadmin restores oldest snapshot
		superadmin.snapshots().restore(snap1.id, true);

		// fred and nath's credentials are gone
		superadmin.login();
		snapman.login();
		vince.login();
		assertHttpError(401, () -> fred.login());
		assertHttpError(404, () -> superadmin.settings().get(MY_SETTINGS_ID));
		assertHttpError(401, () -> nath.login());

		// superadmin restores second (middle) snapshot
		superadmin.snapshots().restore(snap2.id, true);

		// fred's credentials are back
		superadmin.login();
		snapman.login();
		vince.login();
		fred.login();
		superadmin.settings().get(MY_SETTINGS_ID).equals(MY_SETTINGS);
		assertHttpError(401, () -> nath.login());

		// superadmin restores latest (third) snapshot
		superadmin.snapshots().restoreLatest(true);

		// all credentials are present
		superadmin.login();
		snapman.login();
		vince.login();
		fred.login();
		nath.login();
		assertHttpError(404, () -> superadmin.settings().get(MY_SETTINGS_ID));

		// superdog cleans up all backend indices
		superdog().admin().clearBackend();

		// all credentials are gone
		assertHttpError(401, () -> superadmin.login());
		assertHttpError(401, () -> snapman.login());
		assertHttpError(401, () -> vince.login());
		assertHttpError(401, () -> fred.login());
		assertHttpError(401, () -> nath.login());

		// superdog restores latest (third) snapshot
		superdog().snapshots().restoreLatest(true);

		// all credentials are back
		superadmin.login();
		snapman.login();
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
		assertEquals(snap3, all.get(0));
		assertEquals(snap2, all.get(1));
		assertEquals(snap1, all.get(2));
	}

	@Test
	public void getOpenCloseRepositories() {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearServer();

		// superadmin closes all existing repositories
		superadmin.snapshots().getRepositories(0, 10).stream()//
				.forEach(repo -> superadmin.snapshots().closeRepository(repo.id()));

		// superadmin checks there is no repository anymore
		assertEquals(0, superadmin.snapshots().getRepositories(0, 10).size());

		// superadmin fails to open repository with invalid format
		assertHttpError(400, () -> superadmin.snapshots().openRepository("XXX"));

		// superadmin opens a first repository
		superadmin.snapshots().openRepository("2017-22");

		// superadmin checks open repositories
		List<SpaceRepository> repositories = superadmin.snapshots().getRepositories(0, 10);
		assertEquals(1, repositories.size());
		assertEquals("2017-22", repositories.get(0).id());
		assertEquals(FileStoreType.fs.toElasticRepoType(), repositories.get(0).type());
		assertEquals("true", repositories.get(0).settings().get("compress"));
		assertEquals("spacedog/elastic/2017-22", repositories.get(0).settings().get("location"));

		// superadmin opens a second repository
		superadmin.snapshots().openRepository("2017-33");

		// superadmin checks open repositories
		repositories = superadmin.snapshots().getRepositories(0, 10);
		assertEquals(2, repositories.size());
		assertEquals("2017-33", repositories.get(0).id());
		assertEquals("2017-22", repositories.get(1).id());

		// superadmin closes first repository
		superadmin.snapshots().closeRepository("2017-22");

		// superadmin checks open repositories
		repositories = superadmin.snapshots().getRepositories(0, 10);
		assertEquals(1, repositories.size());
		assertEquals("2017-33", repositories.get(0).id());

		// superadmin closes second repository
		superadmin.snapshots().closeRepository("2017-33");

		// superadmin checks there is no repository anymore
		assertEquals(0, superadmin.snapshots().getRepositories(0, 10).size());
	}
}
