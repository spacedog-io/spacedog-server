package io.spacedog.test.file;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.Roles;
import io.spacedog.client.file.FileBucketSettings;
import io.spacedog.client.file.SpaceFile;
import io.spacedog.client.file.SpaceFile.FileList;
import io.spacedog.client.http.ContentTypes;
import io.spacedog.client.http.SpaceHeaders;
import io.spacedog.client.http.SpaceRequest;
import io.spacedog.client.http.SpaceResponse;
import io.spacedog.test.SpaceTest;
import io.spacedog.utils.ClassResources;
import io.spacedog.utils.Json;

public class ShareRestyTest extends SpaceTest {

	private static final String FILE_CONTENT = "This is a test file!";
	private static final byte[] BYTES = "blablabla".getBytes();
	private static final String SHARES = "shares";

	@Test
	public void shareWithDefaultSettings() throws IOException {

		// prepare
		prepareTest();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearServer(true);
		SpaceDog vince = createTempDog(superadmin, "vince");
		SpaceDog admin = createTempDog(superadmin, "admin", Roles.admin);

		// superadmin sets 'shares' file bucket
		FileBucketSettings bucket = new FileBucketSettings(SHARES);
		superadmin.files().setBucket(bucket);

		// only superadmins can list shares
		assertHttpError(401, () -> guest.files().listAll(SHARES));
		assertHttpError(403, () -> vince.files().listAll(SHARES));
		assertHttpError(403, () -> admin.files().listAll(SHARES));

		// only superadmins can create shares
		assertHttpError(401, () -> guest.files().share(SHARES, BYTES));
		assertHttpError(403, () -> vince.files().share(SHARES, BYTES));
		assertHttpError(403, () -> admin.files().share(SHARES, BYTES));

		// superadmins creates a shared file
		SpaceFile file = superadmin.files().share(SHARES, FILE_CONTENT.getBytes());
		// assertNull(shareMeta.publicLocation);

		// only superadmins can read shares
		assertHttpError(401, () -> guest.files().getAsByteArray(SHARES, file.getPath()));
		assertHttpError(403, () -> vince.files().getAsByteArray(SHARES, file.getPath()));
		assertHttpError(403, () -> admin.files().getAsByteArray(SHARES, file.getPath()));
		superadmin.files().getAsByteArray(SHARES, file.getPath());

		// only superadmins can delete shares
		assertHttpError(401, () -> guest.files().delete(SHARES, file.getPath()));
		assertHttpError(403, () -> vince.files().delete(SHARES, file.getPath()));
		assertHttpError(403, () -> admin.files().delete(SHARES, file.getPath()));
		long deleted = superadmin.files().delete(SHARES, file.getPath());
		// assertEquals(shareMeta.path, deleted[0]);
		assertEquals(1, deleted);

	}

	@Test
	public void shareWithCustomSettings() throws IOException {

		// prepare
		prepareTest();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearServer(true);
		SpaceDog vince = createTempDog(superadmin, "vince");
		SpaceDog fred = createTempDog(superadmin, "fred");
		SpaceDog admin = createTempDog(superadmin, "admin", Roles.admin);

		// superadmin sets share file bucket
		FileBucketSettings bucket = new FileBucketSettings(SHARES);
		bucket.permissions.put(Roles.all, Permission.read)//
				.put(Roles.user, Permission.create, Permission.update, Permission.deleteMine);
		superadmin.files().setBucket(bucket);

		// this account is brand new, no shared files
		assertEquals(0, superadmin.files().listAll(SHARES).files.size());

		// anonymous users are not allowed to share files
		assertHttpError(401, () -> guest.files().listAll(SHARES));

		// vince shares a small png file
		byte[] pngBytes = ClassResources.loadAsBytes(this, "tweeter.png");
		// FileMeta pngMeta = vince.files().share(SHARES, pngBytes, "tweeter.png");
		SpaceFile pngFile = vince.files().share(SHARES, new File("tweeter.png"));

		// admin lists all shared files should return tweeter.png path only
		FileList list = superadmin.files().listAll(SHARES);
		assertEquals(1, list.files.size());
		assertEquals(pngFile.getPath(), list.files.get(0).getPath());

		// anonymous gets png share with its path
		SpaceResponse png = guest.get("/2/files/" + SHARES + pngFile.getPath()).go(200);
		assertEquals("image/png", png.contentType());
		assertArrayEquals(pngBytes, png.asBytes());
		assertEquals(vince.id(), png.header(SpaceHeaders.SPACEDOG_OWNER));
		assertEquals(vince.credentials().me().group(), png.header(SpaceHeaders.SPACEDOG_GROUP));
		// assertEquals(pngBytes.length, png.contentLength());
		// assertEquals(pngMeta.etag, png.etag());

		// share small text file
		final SpaceFile txtFile = fred.files().share(SHARES, FILE_CONTENT.getBytes(), "text.txt");

		// superadmin sets share list size to 1
		superadmin.files().listSize(1);

		// list all shared files should return 2 paths
		// superadmin gets first share page with only one path
		list = superadmin.files().listAll(SHARES);
		assertEquals(1, list.files.size());
		Set<String> all = Sets.newHashSet(list.files.get(0).getPath());

		// superadmin gets second (and last) share page with only one path
		list = superadmin.files().list(SHARES, "/", list.next);
		assertEquals(1, list.files.size());
		// assertNull(list.next);
		all.add(list.files.get(0).getPath());

		// the set should contain both file paths
		assertTrue(all.contains(pngFile.getPath()));
		assertTrue(all.contains(txtFile.getPath()));

		// download shared text file
		String txtContent = SpaceRequest.get(location(txtFile)).go(200)//
				.assertHeaderEquals("text/plain", SpaceHeaders.CONTENT_TYPE)//
				.assertHeaderEquals(fred.id(), SpaceHeaders.SPACEDOG_OWNER)//
				.asString();

		assertEquals(FILE_CONTENT, txtContent);

		// only admin or owner can delete a shared file
		assertHttpError(401, () -> guest.files().delete(SHARES, txtFile.getPath()));
		assertHttpError(403, () -> vince.files().delete(SHARES, txtFile.getPath()));

		// owner (fred) can delete its own shared file (test.txt)
		long deleted = fred.files().delete(SHARES, txtFile.getPath());
		// assertEquals(txtMeta.path, deleted[0]);
		assertEquals(1, deleted);

		// superadmin sets share list size to 100
		superadmin.files().listSize(100);

		// superadmin lists shares and
		// it should only return the png file path
		list = superadmin.files().listAll(SHARES);
		assertEquals(1, list.files.size());
		assertEquals(pngFile.getPath(), list.files.get(0).getPath());

		// only superadmin can delete all shared files
		assertHttpError(401, () -> guest.files().deleteAll(SHARES));
		assertHttpError(403, () -> fred.files().deleteAll(SHARES));
		assertHttpError(403, () -> vince.files().deleteAll(SHARES));
		assertHttpError(403, () -> admin.files().deleteAll(SHARES));

		deleted = superadmin.files().deleteAll(SHARES);
		assertEquals(1, deleted);
		// assertEquals(pngMeta.path, deleted[0]);

		// superadmin lists all shares but there is no more
		assertEquals(0, superadmin.files().listAll(SHARES).files.size());

		// share small text file
		SpaceFile txtFile2 = fred.files().share(SHARES, FILE_CONTENT.getBytes(), "text.txt");
		assertEquals(1, superadmin.files().listAll(SHARES).files.size());

		// admin can delete shared file (test.txt) even if not owner
		// with default share ACL settings
		superadmin.files().delete(SHARES, txtFile2.getPath());
		assertEquals(0, superadmin.files().listAll(SHARES).files.size());
	}

	@Test
	public void shareWithAnotherCustomSettings() throws IOException {

		// prepare
		prepareTest();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearServer(true);
		SpaceDog vince = createTempDog(superadmin, "vince");
		SpaceDog fred = createTempDog(superadmin, "fred");
		byte[] pngBytes = ClassResources.loadAsBytes(this, "tweeter.png");

		// superadmin sets 'shares' file bucket
		FileBucketSettings bucket = new FileBucketSettings(SHARES);
		bucket.permissions.put(Roles.all, Permission.create)//
				.put(Roles.user, Permission.update, Permission.readMine, Permission.deleteMine);
		superadmin.files().setBucket(bucket);

		// only admin can get all shared locations
		assertHttpError(401, () -> guest.files().listAll(SHARES));
		assertHttpError(403, () -> vince.files().listAll(SHARES));

		// backend contains no shared file
		assertEquals(0, superadmin.files().listAll(SHARES).files.size());

		// anonymous is allowed to share a file
		SpaceFile guestPng = guest.files().share(SHARES, pngBytes, "guest.png");
		// assertNull(guestPngMeta.publicLocation);

		// backend contains 1 shared file
		FileList list = superadmin.files().listAll(SHARES);
		assertEquals(1, list.files.size());
		assertEquals(guestPng.getPath(), list.files.get(0).getPath());

		// nobody is allowed to read this file
		// but superadmins
		guest.get(location(guestPng)).go(401).asVoid();
		fred.get(location(guestPng)).go(403).asVoid();
		vince.get(location(guestPng)).go(403).asVoid();
		superadmin.get(location(guestPng)).go(200).asVoid();

		// nobody is allowed to delete this file
		// but superadmins
		guest.delete(location(guestPng)).go(401).asVoid();
		fred.delete(location(guestPng)).go(403).asVoid();
		vince.delete(location(guestPng)).go(403).asVoid();
		superadmin.delete(location(guestPng)).go(200).asVoid();

		// backend contains no shared file
		assertEquals(0, superadmin.files().listAll(SHARES).files.size());

		// vince is allowed to share a file
		SpaceFile vincePng = vince.files().share(SHARES, pngBytes, "vince.png");
		// assertNull(vincePngMeta.publicLocation);

		// backend contains 1 shared file
		list = superadmin.files().listAll(SHARES);
		assertEquals(1, list.files.size());
		assertEquals(vincePng.getPath(), list.files.get(0).getPath());

		// nobody is allowed to read this file
		// but vince the owner and superadmins
		assertHttpError(401, () -> guest.files().getAsByteArray(SHARES, vincePng.getPath()));
		assertHttpError(403, () -> fred.files().getAsByteArray(SHARES, vincePng.getPath()));
		vince.files().getAsByteArray(SHARES, vincePng.getPath());
		superadmin.files().getAsByteArray(SHARES, vincePng.getPath());

		// nobody is allowed to delete this file
		// but vince the owner and superadmins
		guest.delete(location(vincePng)).go(401).asVoid();
		fred.delete(location(vincePng)).go(403).asVoid();
		vince.delete(location(vincePng)).go(200).asVoid();

		// backend contains no shared file
		assertEquals(0, superadmin.files().listAll(SHARES).files.size());
	}

	private String location(SpaceFile file) {
		return "/2/files/" + SHARES + file.getPath();
	}

	@Test
	public void shareWithContentDispositionAndEscaping() {

		// prepare
		SpaceDog superadmin = clearServer();

		// superadmin sets 'shares' file bucket
		FileBucketSettings bucket = new FileBucketSettings(SHARES);
		superadmin.files().setBucket(bucket);

		// share file with name that needs escaping
		SpaceFile file = superadmin.files()//
				.share(SHARES, FILE_CONTENT.getBytes(), "un petit text ?");

		// get file from location URI
		// no file extension => no specific content type
		String stringContent = superadmin.get(location(file)).refresh().go(200)//
				.assertHeaderEquals(ContentTypes.OCTET_STREAM, SpaceHeaders.CONTENT_TYPE)//
				.asString();

		Assert.assertEquals(FILE_CONTENT, stringContent);

		// get file from location URI with content disposition
		stringContent = superadmin.get(location(file))//
				.queryParam(WITH_CONTENT_DISPOSITION, true).go(200)//
				.assertHeaderEquals(SpaceHeaders.contentDisposition(file.getName()), //
						SpaceHeaders.CONTENT_DISPOSITION)//
				.asString();

		Assert.assertEquals(FILE_CONTENT, stringContent);
	}

	@Test
	public void exportShares() throws IOException {

		// prepare
		SpaceDog superadmin = clearServer();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog vince = createTempDog(superadmin, "vince");
		SpaceDog nath = createTempDog(superadmin, "nath");

		// superadmin sets 'shares' file bucket
		FileBucketSettings bucket = new FileBucketSettings(SHARES);
		bucket.permissions.put(Roles.user, Permission.create, //
				Permission.readMine, Permission.updateMine);
		superadmin.files().setBucket(bucket);

		// nath uploads 1 share
		String path1 = nath.files().share(SHARES, "toto".getBytes(), "toto.txt").getPath();

		// nath has the right to get his own shares
		nath.files().getAsByteArray(SHARES, path1);

		// vince does not have access to nath's shares
		assertHttpError(403, () -> vince.files().getAsByteArray(SHARES, path1));

		// nath exports her shares
		byte[] zip = nath.files().exportAsByteArray(SHARES, path1);
		assertEquals(1, zipFileNumber(zip));
		assertZipContains(zip, "toto.txt", "toto".getBytes());

		// vince uploads 2 shares
		String path2 = vince.files().share(SHARES, "titi".getBytes(), "titi.txt").getPath();
		byte[] pngBytes = ClassResources.loadAsBytes(this, "tweeter.png");
		String path3 = vince.files().share(SHARES, pngBytes, "tweeter.png").getPath();

		// vince has the right to get his own shares
		vince.files().getAsByteArray(SHARES, path2);
		vince.files().getAsByteArray(SHARES, path3);

		// nath does not have access to vince's shares
		assertHttpError(403, () -> nath.files().getAsByteArray(SHARES, path2));
		assertHttpError(403, () -> nath.files().getAsByteArray(SHARES, path3));

		// vince exports his shares
		zip = vince.files().exportAsByteArray(SHARES, path2, path3);
		assertEquals(2, zipFileNumber(zip));
		assertZipContains(zip, "titi.txt", "titi".getBytes());
		assertZipContains(zip, "tweeter.png", pngBytes);

		// vince fails to export files with invalid paths
		assertHttpError(404, () -> vince.files().exportAsByteArray(//
				SHARES, "/foo/index.html", "/bar/toto.txt"));

		// vince needs read (all) permission to export nath's shares
		assertHttpError(403, () -> vince.files().exportAsByteArray(SHARES, path1, path2));

		// nath needs read (all) permission to export vince's shares
		assertHttpError(403, () -> nath.files().exportAsByteArray(SHARES, path1, path2));

		// guests needs read (all) permission to export shares
		assertHttpError(401, () -> guest.files().exportAsByteArray(SHARES, path1, path2));

		// superadmin updates share settings
		// to allow users to export all shares
		bucket.permissions.put(Roles.user, Permission.read, Permission.updateMine);
		superadmin.files().setBucket(bucket);

		// vince exports specified shares
		zip = vince.post("/2/files/{bucket}")//
				.routeParam("bucket", SHARES)//
				.queryParam("op", "export")//
				.bodyJson("fileName", "export.zip", //
						"paths", Json.array(path1, path2, path3))//
				.go(200)//
				.assertHeaderContains("attachment; filename=\"export.zip\"", //
						SpaceHeaders.CONTENT_DISPOSITION)//
				.asBytes();

		assertZipContains(zip, "toto.txt", "toto".getBytes());
		assertZipContains(zip, "titi.txt", "titi".getBytes());
		assertZipContains(zip, "tweeter.png", pngBytes);
	}

	private int zipFileNumber(byte[] zip) throws IOException {
		ZipInputStream zipStream = new ZipInputStream(new ByteArrayInputStream(zip));
		int size = 0;
		while (zipStream.getNextEntry() != null)
			size = size + 1;
		return size;
	}

	private void assertZipContains(byte[] zip, String name, byte[] file) throws IOException {
		ZipInputStream zipStream = new ZipInputStream(new ByteArrayInputStream(zip));
		ZipEntry entry = zipStream.getNextEntry();
		while (entry != null) {
			if (entry.getName().endsWith(name)) {
				assertArrayEquals(file, ByteStreams.toByteArray(zipStream));
				return;
			}
			entry = zipStream.getNextEntry();
		}
		fail(String.format("file [%s] not found in zip", name));
	}

	@Test
	public void testFileErrorsDoNotDrainAllConnection() {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearServer(true);

		// superadmin sets 'shares' file bucket
		superadmin.files().setBucket(new FileBucketSettings(SHARES));

		// superadmin shares a file
		SpaceFile share = superadmin.files().share(SHARES, "foobar".getBytes());

		// superadmin tries 70 times to get this file
		// he fails since he forces request failure via the _fail param
		// this checks that unexpected errors do not drain s3 connection pool
		for (int i = 0; i < 70; i++)
			superadmin.get(location(share)).queryParam(FAIL_PARAM).go(400).asVoid();
	}

	@Test
	public void shareDownloadAuthenticatedViaQueryParam() {

		// prepare
		prepareTest();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearServer(true);
		SpaceDog vince = createTempDog(superadmin, "vince").login();
		SpaceDog fred = createTempDog(superadmin, "fred").login();

		// superadmin sets 'shares' file bucket
		FileBucketSettings bucket = new FileBucketSettings(SHARES);
		bucket.permissions.put(Roles.user, Permission.create, //
				Permission.updateMine, Permission.readMine);
		superadmin.files().setBucket(bucket);

		// vince shares a file
		SpaceFile share = vince.files().share(SHARES, "vince".getBytes());

		// guest fails to get shared file since no access token query param
		guest.get(location(share)).go(401).asVoid();

		// vince gets his shared file via access token query param
		byte[] bytes = guest.get(location(share))//
				.queryParam("accessToken", vince.accessToken().get())//
				.go(200).asBytes();

		assertArrayEquals("vince".getBytes(), bytes);

		// fred fails to get vince's shared file
		// via access token query param
		// since not the owner
		guest.get(location(share))//
				.queryParam("accessToken", fred.accessToken().get())//
				.go(403).asVoid();
	}

	@Test
	public void shareUploadHasSizeLimit() {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearServer(true);

		// superadmin sets 'shares' file bucket with size limit of 1 KB
		FileBucketSettings bucket = new FileBucketSettings(SHARES);
		bucket.sizeLimitInKB = 1;
		superadmin.files().setBucket(bucket);

		// vince fails to share file with size of 2048 bytes
		// since settings forbids file with size above 1024 bytes
		assertHttpError(400, () -> superadmin.files().share(SHARES, new byte[2048]));

		// vince succeeds to share file with size of 1024 bytes
		superadmin.files().share(SHARES, new byte[1024]);
	}

	@Test
	public void shareDownloadGetsContentLengthHeaderIfNoGzipEncoding() {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearServer(true);

		// superadmin sets 'shares' file bucket
		superadmin.files().setBucket(new FileBucketSettings(SHARES));

		// superadmin shares a small png file
		SpaceFile pngFile = superadmin.files()//
				.share(SHARES, FILE_CONTENT.getBytes(), "test.txt");

		// superadmin gets shared file with its location
		// default stream encoding is chunked since content is gziped
		String downloadedString = superadmin.get(location(pngFile)).go(200)//
				.assertHeaderEquals(SpaceHeaders.CHUNKED, SpaceHeaders.TRANSFER_ENCODING)//
				.assertHeaderNotPresent(SpaceHeaders.CONTENT_LENGTH)//
				.asString();

		assertEquals(FILE_CONTENT, downloadedString);

		// superadmin gets shared file with its location
		// if request Accept-Encoding without gzip
		// then response contains Content-Length
		downloadedString = superadmin.get(location(pngFile))//
				.setHeader(SpaceHeaders.ACCEPT_ENCODING, SpaceHeaders.IDENTITY)//
				.go(200)//
				.assertHeaderNotPresent(SpaceHeaders.TRANSFER_ENCODING)//
				.assertHeaderEquals(FILE_CONTENT.getBytes().length, SpaceHeaders.CONTENT_LENGTH)//
				.asString();

		assertEquals(FILE_CONTENT, downloadedString);
	}
}
