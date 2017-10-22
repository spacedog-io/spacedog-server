package io.spacedog.watchdog;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;

import io.spacedog.client.ShareEndpoint.Share;
import io.spacedog.client.ShareEndpoint.ShareList;
import io.spacedog.client.ShareEndpoint.ShareMeta;
import io.spacedog.client.SpaceDog;
import io.spacedog.http.SpaceRequest;
import io.spacedog.http.SpaceTest;
import io.spacedog.model.DataPermission;
import io.spacedog.model.ShareSettings;
import io.spacedog.utils.SpaceHeaders;

public class ShareResourceTestOncePerDay extends SpaceTest {

	private static final String FILE_CONTENT = "This is a test file!";

	@Test
	public void shareWithDefaultSettings() throws IOException {

		// prepare
		prepareTest(false);
		SpaceDog superadmin = resetTestBackend();
		SpaceDog guest = SpaceDog.backend(superadmin);
		SpaceDog vince = createTempDog(superadmin, "vince");
		SpaceDog fred = createTempDog(superadmin, "fred");

		// only admin can get all shared locations
		guest.get("/1/shares").go(403);
		vince.get("/1/shares").go(403);

		// this account is brand new, no shared files
		assertEquals(0, superadmin.shares().list().shares.length);

		// anonymous users are not allowed to share files
		guest.post("/1/shares").go(403);

		// vince shares a small png file
		byte[] pngBytes = Resources.toByteArray(//
				Resources.getResource(this.getClass(), "tweeter.png"));

		ShareMeta pngMeta = vince.shares().upload(pngBytes, "tweeter.png");

		// admin lists all shared files should return tweeter.png path only
		ShareList list = superadmin.shares().list();
		assertEquals(1, list.shares.length);
		assertEquals(pngMeta.id, list.shares[0].id);

		// anonymous gets png share with its id
		Share png = guest.shares().get(pngMeta.id);
		assertEquals("image/png", png.contentType);
		assertEquals(vince.id(), png.owner);
		assertEquals(pngMeta.etag, png.etag);

		assertTrue(Arrays.equals(pngBytes, png.content));

		// anonymous gets png share with its location
		byte[] downloadedBytes = guest.get(pngMeta.location).go(200)//
				// .assertHeaderEquals("gzip", SpaceHeaders.CONTENT_ENCODING)//
				.assertHeaderEquals("image/png", SpaceHeaders.CONTENT_TYPE)//
				.assertHeaderEquals(vince.id(), SpaceHeaders.SPACEDOG_OWNER)//
				.assertHeaderEquals(pngMeta.etag, SpaceHeaders.ETAG)//
				.asBytes();

		assertTrue(Arrays.equals(pngBytes, downloadedBytes));

		// anonymous gets png share through S3 direct access
		// with wrong etag
		downloadedBytes = SpaceRequest.get(pngMeta.s3)//
				.addHeader(SpaceHeaders.IF_NONE_MATCH, "XXX")//
				.go(200)//
				.assertHeaderEquals("image/png", SpaceHeaders.CONTENT_TYPE)//
				.assertHeaderEquals('"' + pngMeta.etag + '"', SpaceHeaders.ETAG)//
				.asBytes();

		assertTrue(Arrays.equals(pngBytes, downloadedBytes));

		// anonymous gets png share through S3 direct access
		// with correct etag to get 304 Not Modified
		SpaceRequest.get(pngMeta.s3)//
				.addHeader(SpaceHeaders.IF_NONE_MATCH, pngMeta.etag)//
				.go(304);

		// share small text file
		ShareMeta txtMeta = fred.shares().upload(FILE_CONTENT.getBytes(), "text.txt");

		// superadmin sets share list size to 1
		superadmin.shares().listSize(1);

		// list all shared files should return 2 paths
		// superadmin gets first share page with only one path
		list = superadmin.shares().list();
		assertEquals(1, list.shares.length);
		Set<String> all = Sets.newHashSet(list.shares[0].id);

		// superadmin gets second (and last) share page with only one path
		list = superadmin.shares().list(list.next);
		assertEquals(1, list.shares.length);
		assertNull(list.next);
		all.add(list.shares[0].id);

		// the set should contain both file paths
		Assert.assertTrue(all.contains(pngMeta.id));
		Assert.assertTrue(all.contains(txtMeta.id));

		// download shared text file
		String stringContent = SpaceRequest.get(txtMeta.location).go(200)//
				// .assertHeaderEquals("gzip", SpaceHeaders.CONTENT_ENCODING)//
				.assertHeaderEquals("text/plain", SpaceHeaders.CONTENT_TYPE)//
				.assertHeaderEquals(fred.id(), SpaceHeaders.SPACEDOG_OWNER)//
				.asString();

		assertEquals(FILE_CONTENT, stringContent);

		// download shared text file through direct S3 access
		stringContent = SpaceRequest.get(txtMeta.s3).go(200)//
				.assertHeaderEquals("text/plain", SpaceHeaders.CONTENT_TYPE)//
				.asString();

		Assert.assertEquals(FILE_CONTENT, stringContent);

		// only admin or owner can delete a shared file
		SpaceRequest.delete(txtMeta.location).go(403);
		vince.delete(txtMeta.location).go(403);

		// owner (fred) can delete its own shared file (test.txt)
		fred.shares().delete(txtMeta.id);

		// superadmin sets share list size to 100
		superadmin.shares().listSize(100);

		// superadmin lists shares and
		// it should only return the png file path
		list = superadmin.shares().list();
		assertEquals(1, list.shares.length);
		assertEquals(pngMeta.id, list.shares[0].id);

		// only admin can delete all shared files
		guest.delete("/1/shares").go(403);
		fred.delete("/1/shares").go(403);
		vince.delete("/1/shares").go(403);
		superadmin.delete("/1/shares").go(200)//
				.assertSizeEquals(1, "deleted")//
				.assertContains(TextNode.valueOf(pngMeta.id), "deleted");

		// superadmin lists all shares but there is no more
		assertEquals(0, superadmin.shares().list().shares.length);

		// share small text file
		txtMeta = fred.shares().upload(FILE_CONTENT.getBytes(), "text.txt");

		// admin can delete shared file (test.txt) even if not owner
		// with default share ACL settings
		superadmin.delete(txtMeta.location).go(200);
		assertEquals(0, superadmin.shares().list().shares.length);
	}

	@Test
	public void shareWithCustomSettings() throws IOException {

		// prepare
		prepareTest(false);
		SpaceDog superadmin = resetTestBackend();
		SpaceDog guest = SpaceDog.backend(superadmin);
		SpaceDog vince = createTempDog(superadmin, "vince");
		SpaceDog fred = createTempDog(superadmin, "fred");
		byte[] pngBytes = Resources.toByteArray(//
				Resources.getResource("io/spacedog/watchdog/tweeter.png"));

		// super admin sets custom share permissions
		ShareSettings settings = new ShareSettings();
		settings.enableS3Location = false;
		settings.acl.put("all", Sets.newHashSet(DataPermission.create));
		settings.acl.put("user", Sets.newHashSet(DataPermission.create, DataPermission.read, //
				DataPermission.delete));
		settings.acl.put("admin", Sets.newHashSet(DataPermission.delete_all, DataPermission.search));
		superadmin.settings().save(settings);

		// only admin can get all shared locations
		guest.get("/1/shares").go(403);
		vince.get("/1/shares").go(403);

		// backend contains no shared file
		assertEquals(0, superadmin.shares().list().shares.length);

		// anonymous is allowed to share a file
		ShareMeta guestPngMeta = guest.shares().upload(pngBytes, "guest.png");
		assertNull(guestPngMeta.s3);

		// backend contains 1 shared file
		ShareList list = superadmin.shares().list();
		assertEquals(1, list.shares.length);
		assertEquals(guestPngMeta.id, list.shares[0].id);

		// nobody is allowed to read this file
		// but superadmins and superdogs
		SpaceRequest.get(guestPngMeta.location).go(403);
		fred.get(guestPngMeta.location).go(403);
		vince.get(guestPngMeta.location).go(403);
		superadmin.get(guestPngMeta.location).go(200);

		// nobody is allowed to read this file but super admins
		// since they got delete_all permission
		SpaceRequest.delete(guestPngMeta.location).go(403);
		fred.delete(guestPngMeta.location).go(403);
		vince.delete(guestPngMeta.location).go(403);
		superadmin.delete(guestPngMeta.location).go(200);

		// backend contains no shared file
		assertEquals(0, superadmin.shares().list().shares.length);

		// vince is allowed to share a file
		ShareMeta vincePngMeta = vince.shares().upload(pngBytes, "vince.png");
		assertNull(vincePngMeta.s3);

		// backend contains 1 shared file
		list = superadmin.shares().list();
		assertEquals(1, list.shares.length);
		assertEquals(vincePngMeta.id, list.shares[0].id);

		// nobody is allowed to read this file
		// but vince the owner (and superadmin)
		SpaceRequest.get(vincePngMeta.location).go(403);
		fred.get(vincePngMeta.location).go(403);
		vince.get(vincePngMeta.location).go(200);
		superadmin.get(vincePngMeta.location).go(200);

		// nobody is allowed to delete this file
		// but vince the owner (and superadmin)
		SpaceRequest.delete(vincePngMeta.location).go(403);
		fred.delete(vincePngMeta.location).go(403);
		vince.delete(vincePngMeta.location).go(200);

		// backend contains no shared file
		assertEquals(0, superadmin.shares().list().shares.length);
	}

	@Test
	public void testSharingWithContentDispositionAndEscaping() {

		// prepare
		SpaceDog superadmin = resetTestBackend();

		// share file with name that needs escaping
		ShareMeta meta = superadmin.shares().upload(FILE_CONTENT.getBytes(), "un petit text ?");

		// get file from location URI
		// no file extension => no specific content type
		String stringContent = SpaceRequest.get(meta.location).go(200)//
				.assertHeaderEquals("application/octet-stream", SpaceHeaders.CONTENT_TYPE)//
				.asString();

		Assert.assertEquals(FILE_CONTENT, stringContent);

		// get file from location URI with content disposition
		stringContent = SpaceRequest.get(meta.location)//
				.queryParam(WITH_CONTENT_DISPOSITION, true).go(200)//
				.assertHeaderEquals("attachment; filename=\"un petit text ?\"", //
						SpaceHeaders.CONTENT_DISPOSITION)//
				.asString();

		Assert.assertEquals(FILE_CONTENT, stringContent);

		// get file from S3 location URI
		// no file extension => no specific content type
		// by default S3 returns content disposition header if set in metadata
		stringContent = SpaceRequest.get(meta.s3).go(200)//
				.assertHeaderEquals("application/octet-stream", SpaceHeaders.CONTENT_TYPE)//
				.assertHeaderEquals("attachment; filename=\"un petit text ?\"", //
						SpaceHeaders.CONTENT_DISPOSITION)//
				.asString();

		Assert.assertEquals(FILE_CONTENT, stringContent);
	}

}
