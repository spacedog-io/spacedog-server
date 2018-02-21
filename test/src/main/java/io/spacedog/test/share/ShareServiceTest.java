package io.spacedog.test.share;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;

import io.spacedog.client.ShareEndpoint.Share;
import io.spacedog.client.ShareEndpoint.ShareList;
import io.spacedog.client.ShareEndpoint.ShareMeta;
import io.spacedog.client.SpaceDog;
import io.spacedog.http.SpaceHeaders;
import io.spacedog.http.SpaceRequest;
import io.spacedog.model.Permission;
import io.spacedog.model.Roles;
import io.spacedog.model.ShareSettings;
import io.spacedog.test.SpaceTest;
import io.spacedog.utils.ClassResources;
import io.spacedog.utils.Json;

public class ShareServiceTest extends SpaceTest {

	private static final String FILE_CONTENT = "This is a test file!";
	private static final byte[] BYTES = "blablabla".getBytes();

	@Test
	public void shareWithDefaultSettings() throws IOException {

		// prepare
		prepareTest(false);
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearRootBackend();
		SpaceDog vince = createTempDog(superadmin, "vince");
		SpaceDog admin = createTempDog(superadmin, "admin", Roles.admin);

		// only superadmins can list shares
		assertHttpError(403, () -> guest.shares().list());
		assertHttpError(403, () -> vince.shares().list());
		assertHttpError(403, () -> admin.shares().list());

		// only superadmins can create shares
		assertHttpError(403, () -> guest.shares().upload(BYTES));
		assertHttpError(403, () -> vince.shares().upload(BYTES));
		assertHttpError(403, () -> admin.shares().upload(BYTES));

		// superadmins creates a shared file
		ShareMeta shareMeta = superadmin.shares().upload(FILE_CONTENT.getBytes());
		assertNull(shareMeta.publicLocation);

		// only superadmins can read shares
		assertHttpError(403, () -> guest.shares().download(shareMeta.id));
		assertHttpError(403, () -> vince.shares().download(shareMeta.id));
		assertHttpError(403, () -> admin.shares().download(shareMeta.id));
		superadmin.shares().download(shareMeta.id);

		// only superadmins can delete shares
		assertHttpError(403, () -> guest.shares().delete(shareMeta.id));
		assertHttpError(403, () -> vince.shares().delete(shareMeta.id));
		assertHttpError(403, () -> admin.shares().delete(shareMeta.id));
		String[] deleted = superadmin.shares().delete(shareMeta.id);
		assertEquals(shareMeta.id, deleted[0]);
	}

	@Test
	public void shareWithCustomSettings() throws IOException {

		// prepare
		prepareTest(false);
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearRootBackend();
		SpaceDog vince = createTempDog(superadmin, "vince");
		SpaceDog fred = createTempDog(superadmin, "fred");
		SpaceDog admin = createTempDog(superadmin, "admin", Roles.admin);

		// superadmin sets custom share permissions
		ShareSettings settings = new ShareSettings();
		settings.enablePublicLocation = true;
		settings.permissions.put("all", Permission.read)//
				.put("user", Permission.create, Permission.deleteMine);
		superadmin.settings().save(settings);

		// this account is brand new, no shared files
		assertEquals(0, superadmin.shares().list().shares.length);

		// anonymous users are not allowed to share files
		guest.post("/1/shares").go(403);

		// vince shares a small png file
		byte[] pngBytes = ClassResources.loadAsBytes(this, "tweeter.png");
		ShareMeta pngMeta = vince.shares().upload(pngBytes, "tweeter.png");

		// admin lists all shared files should return tweeter.png path only
		ShareList list = superadmin.shares().list();
		assertEquals(1, list.shares.length);
		assertEquals(pngMeta.id, list.shares[0].id);

		// anonymous gets png share with its id
		Share png = guest.shares().download(pngMeta.id);
		assertEquals("image/png", png.contentType);
		assertEquals(pngBytes.length, png.contentLength);
		assertEquals(vince.id(), png.owner);
		assertEquals(pngMeta.etag, png.etag);

		assertArrayEquals(pngBytes, png.content);

		// anonymous gets png share with its location
		byte[] downloadedBytes = guest.get(pngMeta.location).go(200)//
				.assertHeaderEquals("image/png", SpaceHeaders.CONTENT_TYPE)//
				.assertHeaderEquals(vince.id(), SpaceHeaders.SPACEDOG_OWNER)//
				.assertHeaderEquals(pngMeta.etag, SpaceHeaders.ETAG)//
				.asBytes();

		assertArrayEquals(pngBytes, downloadedBytes);

		// anonymous gets png share through S3 direct access
		// with wrong etag
		downloadedBytes = SpaceRequest.get(pngMeta.publicLocation)//
				.addHeader(SpaceHeaders.IF_NONE_MATCH, "XXX")//
				.go(200)//
				.assertHeaderEquals("image/png", SpaceHeaders.CONTENT_TYPE)//
				.assertHeaderEquals('"' + pngMeta.etag + '"', SpaceHeaders.ETAG)//
				.asBytes();

		assertArrayEquals(pngBytes, downloadedBytes);

		// anonymous gets png share through S3 direct access
		// with correct etag to get 304 Not Modified
		SpaceRequest.get(pngMeta.publicLocation)//
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
				.assertHeaderEquals("text/plain", SpaceHeaders.CONTENT_TYPE)//
				.assertHeaderEquals(fred.id(), SpaceHeaders.SPACEDOG_OWNER)//
				.asString();

		assertEquals(FILE_CONTENT, stringContent);

		// download shared text file through direct S3 access
		stringContent = SpaceRequest.get(txtMeta.publicLocation).go(200)//
				.assertHeaderEquals("text/plain", SpaceHeaders.CONTENT_TYPE)//
				.asString();

		assertEquals(FILE_CONTENT, stringContent);

		// only admin or owner can delete a shared file
		SpaceRequest.delete(txtMeta.location).go(403);
		vince.delete(txtMeta.location).go(403);

		// owner (fred) can delete its own shared file (test.txt)
		String[] deleted = fred.shares().delete(txtMeta.id);
		assertEquals(txtMeta.id, deleted[0]);

		// superadmin sets share list size to 100
		superadmin.shares().listSize(100);

		// superadmin lists shares and
		// it should only return the png file path
		list = superadmin.shares().list();
		assertEquals(1, list.shares.length);
		assertEquals(pngMeta.id, list.shares[0].id);

		// only superadmin can delete all shared files
		guest.delete("/1/shares").go(403);
		fred.delete("/1/shares").go(403);
		vince.delete("/1/shares").go(403);
		admin.delete("/1/shares").go(403);
		deleted = superadmin.shares().deleteAll();
		assertEquals(1, deleted.length);
		assertEquals(pngMeta.id, deleted[0]);

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
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearRootBackend();
		SpaceDog vince = createTempDog(superadmin, "vince");
		SpaceDog fred = createTempDog(superadmin, "fred");
		byte[] pngBytes = ClassResources.loadAsBytes(this, "tweeter.png");

		// superadmin sets custom share permissions
		ShareSettings settings = new ShareSettings();
		settings.enablePublicLocation = false;
		settings.permissions.put("all", Permission.create)//
				.put("user", Permission.create, Permission.readMine, Permission.deleteMine);
		superadmin.settings().save(settings);

		// only admin can get all shared locations
		guest.get("/1/shares").go(403);
		vince.get("/1/shares").go(403);

		// backend contains no shared file
		assertEquals(0, superadmin.shares().list().shares.length);

		// anonymous is allowed to share a file
		ShareMeta guestPngMeta = guest.shares().upload(pngBytes, "guest.png");
		assertNull(guestPngMeta.publicLocation);

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
		assertNull(vincePngMeta.publicLocation);

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
		SpaceDog superadmin = clearRootBackend();

		// superadmin enables s3 locations
		ShareSettings settings = new ShareSettings();
		settings.enablePublicLocation = true;
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
		stringContent = SpaceRequest.get(meta.publicLocation).go(200)//
				.assertHeaderEquals("application/octet-stream", SpaceHeaders.CONTENT_TYPE)//
				.assertHeaderEquals("attachment; filename=\"un petit text ?\"", //
						SpaceHeaders.CONTENT_DISPOSITION)//
				.asString();

		Assert.assertEquals(FILE_CONTENT, stringContent);
	}

	@Test
	public void downloadZipOfShares() throws IOException {

		// prepare
		SpaceDog superadmin = clearRootBackend();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog vince = createTempDog(superadmin, "vince");
		SpaceDog nath = createTempDog(superadmin, "nath");

		// prepare share settings
		ShareSettings settings = new ShareSettings();
		settings.permissions.put("user", Permission.readMine, Permission.create);
		superadmin.settings().save(settings);

		// nath uploads 1 share
		String path1 = nath.shares().upload("toto".getBytes(), "toto.txt").id;

		// nath has the right to get his own shares
		nath.shares().download(path1);

		// vince does not have access to nath's shares
		assertHttpError(403, () -> vince.shares().download(path1));

		// nath gets a zip of her shares
		byte[] zip = nath.shares().downloadAll(path1);
		assertEquals(1, zipFileNumber(zip));
		assertZipContains(zip, 1, "toto.txt", "toto".getBytes());

		// vince uploads 2 shares
		String path2 = vince.shares().upload("titi".getBytes(), "titi.txt").id;
		byte[] pngBytes = ClassResources.loadAsBytes(this, "tweeter.png");
		String path3 = vince.shares().upload(pngBytes, "tweeter.png").id;

		// vince has the right to get his own shares
		vince.shares().download(path2);
		vince.shares().download(path3);

		// nath does not have access to vince's shares
		assertHttpError(403, () -> nath.shares().download(path2));
		assertHttpError(403, () -> nath.shares().download(path3));

		// vince gets a zip of his shares
		zip = vince.shares().downloadAll(path2, path3);
		assertEquals(2, zipFileNumber(zip));
		assertZipContains(zip, 1, "titi.txt", "titi".getBytes());
		assertZipContains(zip, 2, "tweeter.png", pngBytes);

		// vince needs readAll permission to zip nath's shares
		assertHttpError(403, () -> vince.shares().downloadAll(path1, path2));

		// nath needs readAll permission to zip vince's shares
		assertHttpError(403, () -> nath.shares().downloadAll(path1, path2));

		// guests don't have permission to zip shares
		assertHttpError(403, () -> guest.shares().downloadAll(path1, path2));

		// superadmin updates share settings to allow users
		// to download multiple shares
		settings.permissions.put("user", Permission.read, Permission.create);
		superadmin.settings().save(settings);

		// vince downloads zip containing specified shares
		zip = vince.post("/1/shares/download")//
				.bodyJson("fileName", "download.zip", //
						"paths", Json.array(path1, path2, path3))//
				.go(200)//
				.assertHeaderContains("attachment; filename=\"download.zip\"", //
						SpaceHeaders.CONTENT_DISPOSITION)//
				.asBytes();

		assertZipContains(zip, 1, "toto.txt", "toto".getBytes());
		assertZipContains(zip, 2, "titi.txt", "titi".getBytes());
		assertZipContains(zip, 3, "tweeter.png", pngBytes);
	}

	private int zipFileNumber(byte[] zip) throws IOException {
		ZipInputStream zipStream = new ZipInputStream(new ByteArrayInputStream(zip));
		int size = 0;
		while (zipStream.getNextEntry() != null)
			size = size + 1;
		return size;
	}

	private void assertZipContains(byte[] zip, int position, String name, byte[] file) throws IOException {
		ZipInputStream zipStream = new ZipInputStream(new ByteArrayInputStream(zip));
		for (int i = 1; i < position; i++)
			zipStream.getNextEntry();
		ZipEntry entry = zipStream.getNextEntry();
		System.out.println(entry.getName());
		assertTrue(entry.getName().endsWith(name));
		assertArrayEquals(file, ByteStreams.toByteArray(zipStream));
	}

	@Test
	public void testFileOwnershipErrorsDoNotDrainAllS3Connection() throws IOException {

		// prepare
		prepareTest(false);
		SpaceDog superadmin = clearRootBackend();
		SpaceDog vince = createTempDog(superadmin, "vince");
		SpaceDog fred = createTempDog(superadmin, "fred");

		// super admin sets custom share permissions
		ShareSettings settings = new ShareSettings();
		settings.permissions.put("user", Permission.create, Permission.readMine);
		superadmin.settings().save(settings);

		// vince shares a file
		ShareMeta share = vince.shares().upload("vince".getBytes(), "vince.txt");

		// fred is not allowed to read vince's file
		// check ownership error do not drain s3 connection pool
		for (int i = 0; i < 70; i++)
			fred.get(share.location).go(403);
	}

	@Test
	public void shareDownloadAuthenticatedViaQueryParam() {

		// prepare
		prepareTest(false);
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearRootBackend();
		SpaceDog vince = createTempDog(superadmin, "vince").login();
		SpaceDog fred = createTempDog(superadmin, "fred").login();

		// super admin sets custom share permissions
		ShareSettings settings = new ShareSettings();
		settings.permissions.put("user", Permission.create, Permission.readMine);
		superadmin.settings().save(settings);

		// vince shares a file
		ShareMeta share = vince.shares().upload("vince".getBytes());

		// guest fails to get shared file since no access token query param
		guest.get(share.location).go(403);

		// vince gets his shared file via access token query param
		byte[] bytes = guest.get(share.location)//
				.queryParam("accessToken", vince.accessToken().get())//
				.go(200).asBytes();

		assertArrayEquals("vince".getBytes(), bytes);

		// fred fails to get vince's shared file
		// via access token query param
		// since not the owner
		guest.get(share.location)//
				.queryParam("accessToken", fred.accessToken().get())//
				.go(403);
	}

	@Test
	public void shareUploadHasSizeLimit() {

		// prepare
		prepareTest(false);
		SpaceDog superadmin = clearRootBackend();

		// superadmin sets custom share permissions
		// with share size limit of 1 KB
		ShareSettings settings = new ShareSettings();
		settings.permissions.put("superadmin", Permission.create);
		settings.sizeLimitInKB = 1;
		superadmin.settings().save(settings);

		// vince fails to share file with size of 2048 bytes
		// since settings forbids file with size above 1024 bytes
		assertHttpError(400, () -> superadmin.shares().upload(new byte[2048]));

		// vince succeeds to share file with size of 1024 bytes
		superadmin.shares().upload(new byte[1024]);
	}

	@Test
	public void shareDownloadGetsContentLengthHeaderIfNoGzipEncoding() {

		// prepare
		prepareTest(false);
		SpaceDog superadmin = clearRootBackend();

		// superadmin shares a small png file
		String pngLocation = superadmin.shares()//
				.upload(FILE_CONTENT.getBytes(), "test.txt").location;

		// superadmin gets shared file with its location
		// default stream encoding is chunked since content is gziped
		String downloadedString = superadmin.get(pngLocation).go(200)//
				.assertHeaderEquals(SpaceHeaders.CHUNKED, SpaceHeaders.TRANSFER_ENCODING)//
				.assertHeaderNotPresent(SpaceHeaders.CONTENT_LENGTH)//
				.asString();

		assertEquals(FILE_CONTENT, downloadedString);

		// superadmin gets shared file with its location
		// if request Accept-Encoding without gzip
		// then response contains Content-Length
		downloadedString = superadmin.get(pngLocation)//
				.addHeader(SpaceHeaders.ACCEPT_ENCODING, SpaceHeaders.IDENTITY)//
				.go(200)//
				.assertHeaderNotPresent(SpaceHeaders.TRANSFER_ENCODING)//
				.assertHeaderEquals(FILE_CONTENT.getBytes().length, SpaceHeaders.CONTENT_LENGTH)//
				.asString();

		assertEquals(FILE_CONTENT, downloadedString);
	}
}
