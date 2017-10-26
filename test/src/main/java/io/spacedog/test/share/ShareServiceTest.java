package io.spacedog.test.share;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.common.io.Resources;

import io.spacedog.client.ShareEndpoint.Share;
import io.spacedog.client.ShareEndpoint.ShareList;
import io.spacedog.client.ShareEndpoint.ShareMeta;
import io.spacedog.client.SpaceDog;
import io.spacedog.http.SpaceRequest;
import io.spacedog.http.SpaceTest;
import io.spacedog.model.Permission;
import io.spacedog.model.ShareSettings;
import io.spacedog.utils.Credentials.Type;
import io.spacedog.utils.Json;
import io.spacedog.utils.SpaceHeaders;

public class ShareServiceTest extends SpaceTest {

	private static final String FILE_CONTENT = "This is a test file!";

	@Test
	public void shareWithDefaultSettings() throws IOException {

		// prepare
		prepareTest(false);
		SpaceDog superadmin = resetTestBackend();
		SpaceDog guest = SpaceDog.backend(superadmin);
		SpaceDog vince = createTempDog(superadmin, "vince");
		SpaceDog admin = createTempDog(superadmin, "admin", Type.admin.name());

		// only superadmins can list shares
		guest.get("/1/shares").go(403);
		vince.get("/1/shares").go(403);
		admin.get("/1/shares").go(403);

		// only superadmins can create shares
		guest.post("/1/shares").go(403);
		vince.post("/1/shares").go(403);
		admin.post("/1/shares").go(403);
		ShareMeta shareMeta = superadmin.shares().upload(FILE_CONTENT.getBytes());
		assertNull(shareMeta.s3);

		// only superadmins can read shares
		guest.get("/1/shares/" + shareMeta.id).go(403);
		vince.get("/1/shares/" + shareMeta.id).go(403);
		admin.get("/1/shares/" + shareMeta.id).go(403);
		superadmin.shares().get(shareMeta.id);

		// only superadmins can delete shares
		guest.delete("/1/shares/" + shareMeta.id).go(403);
		vince.delete("/1/shares/" + shareMeta.id).go(403);
		admin.delete("/1/shares/" + shareMeta.id).go(403);
		superadmin.shares().delete(shareMeta.id);
	}

	@Test
	public void shareWithCustomSettings() throws IOException {

		// prepare
		prepareTest(false);
		SpaceDog superadmin = resetTestBackend();
		SpaceDog guest = SpaceDog.backend(superadmin);
		SpaceDog vince = createTempDog(superadmin, "vince");
		SpaceDog fred = createTempDog(superadmin, "fred");

		// superadmin sets custom share permissions
		ShareSettings settings = new ShareSettings();
		settings.enableS3Location = true;
		settings.sharePermissions.put("all", Permission.read)//
				.put("user", Permission.create, Permission.deleteMine);
		superadmin.settings().save(settings);

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

		assertArrayEquals(pngBytes, png.content);

		// anonymous gets png share with its location
		byte[] downloadedBytes = guest.get(pngMeta.location).go(200)//
				// .assertHeaderEquals("gzip", SpaceHeaders.CONTENT_ENCODING)//
				.assertHeaderEquals("image/png", SpaceHeaders.CONTENT_TYPE)//
				.assertHeaderEquals(vince.id(), SpaceHeaders.SPACEDOG_OWNER)//
				.assertHeaderEquals(pngMeta.etag, SpaceHeaders.ETAG)//
				.asBytes();

		assertArrayEquals(pngBytes, downloadedBytes);

		// anonymous gets png share through S3 direct access
		// with wrong etag
		downloadedBytes = SpaceRequest.get(pngMeta.s3)//
				.addHeader(SpaceHeaders.IF_NONE_MATCH, "XXX")//
				.go(200)//
				.assertHeaderEquals("image/png", SpaceHeaders.CONTENT_TYPE)//
				.assertHeaderEquals('"' + pngMeta.etag + '"', SpaceHeaders.ETAG)//
				.asBytes();

		assertArrayEquals(pngBytes, downloadedBytes);

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
		assertTrue(all.contains(pngMeta.id));
		assertTrue(all.contains(txtMeta.id));

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

		assertEquals(FILE_CONTENT, stringContent);

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
	public void shareWithAnotherCustomSettings() throws IOException {

		// prepare
		prepareTest(false);
		SpaceDog superadmin = resetTestBackend();
		SpaceDog guest = SpaceDog.backend(superadmin);
		SpaceDog vince = createTempDog(superadmin, "vince");
		SpaceDog fred = createTempDog(superadmin, "fred");
		byte[] pngBytes = Resources.toByteArray(//
				Resources.getResource(getClass(), "tweeter.png"));

		// superadmin sets custom share permissions
		ShareSettings settings = new ShareSettings();
		settings.enableS3Location = false;
		settings.sharePermissions.put("all", Permission.create)//
				.put("user", Permission.create, Permission.readMine, Permission.deleteMine);
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
		// but superadmins
		SpaceRequest.get(guestPngMeta.location).go(403);
		fred.get(guestPngMeta.location).go(403);
		vince.get(guestPngMeta.location).go(403);
		superadmin.get(guestPngMeta.location).go(200);

		// nobody is allowed to delete this file
		// but superadmins
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
		// but vince the owner and superadmins
		SpaceRequest.get(vincePngMeta.location).go(403);
		fred.get(vincePngMeta.location).go(403);
		vince.get(vincePngMeta.location).go(200);
		superadmin.get(vincePngMeta.location).go(200);

		// nobody is allowed to delete this file
		// but vince the owner and superadmins
		SpaceRequest.delete(vincePngMeta.location).go(403);
		fred.delete(vincePngMeta.location).go(403);
		vince.delete(vincePngMeta.location).go(200);

		// backend contains no shared file
		assertEquals(0, superadmin.shares().list().shares.length);
	}

	@Test
	public void shareWithContentDispositionAndEscaping() {

		// prepare
		SpaceDog superadmin = resetTestBackend();

		// superadmin enables s3 locations
		ShareSettings settings = new ShareSettings();
		settings.enableS3Location = true;
		superadmin.settings().save(settings);

		// share file with name that needs escaping
		ShareMeta meta = superadmin.shares().upload(FILE_CONTENT.getBytes(), "un petit text ?");

		// get file from location URI
		// no file extension => no specific content type
		String stringContent = superadmin.get(meta.location).go(200)//
				.assertHeaderEquals("application/octet-stream", SpaceHeaders.CONTENT_TYPE)//
				.asString();

		Assert.assertEquals(FILE_CONTENT, stringContent);

		// get file from location URI with content disposition
		stringContent = superadmin.get(meta.location)//
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

	@Test
	public void testDownloadManyShares() throws IOException {

		// prepare
		SpaceDog superadmin = resetTestBackend();

		// prepare share settings
		ShareSettings settings = new ShareSettings();
		settings.sharePermissions.put("admin", Permission.readMine, Permission.create);
		superadmin.settings().save(settings);

		// share file with name that needs escaping
		String path1 = superadmin.put("/1/share/toto.txt")//
				.bodyBytes("toto".getBytes())//
				.go(200)//
				.getString("path");

		superadmin.get("/1/share/" + path1).go(200);

		String path2 = superadmin.put("/1/share/titi.txt")//
				.bodyBytes("titi".getBytes())//
				.go(200)//
				.getString("path");

		superadmin.get("/1/share/" + path2).go(200);

		String path3 = superadmin.put("/1/share/tweeter.png")//
				.bodyResource(getClass(), "tweeter.png")//
				.go(200)//
				.getString("path");

		superadmin.get("/1/share/" + path3).go(200);

		// superadmin needs read_all permission to downloads many shares
		superadmin.post("/1/share/_zip")//
				.bodyJson("fileName", "download.zip", //
						"paths", Json.array(path1, path2, path3))//
				.go(403);

		// superadmin updates share settings to allow admin
		// to download multiple shares
		settings.sharePermissions.put("admin", Permission.read, Permission.create);
		superadmin.settings().save(settings);

		// superadmin downloads zip containing specified shares
		byte[] bytes = superadmin.post("/1/share/_zip")//
				.bodyJson("fileName", "download.zip", //
						"paths", Json.array(path1, path2, path3))//
				.go(200)//
				.assertHeaderContains("attachment; filename=\"download.zip\"", //
						SpaceHeaders.CONTENT_DISPOSITION)//
				.asBytes();

		Files.write(bytes, new File(System.getProperty("user.home"), "download.zip"));
	}

}