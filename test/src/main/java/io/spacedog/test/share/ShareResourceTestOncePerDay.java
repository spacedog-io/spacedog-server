package io.spacedog.test.share;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;

import io.spacedog.model.DataPermission;
import io.spacedog.model.ShareSettings;
import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceResponse;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.ClassResources;
import io.spacedog.utils.Json7;
import io.spacedog.utils.SpaceHeaders;
import io.spacedog.utils.SpaceParams;
import io.spacedog.utils.Utils;

public class ShareResourceTestOncePerDay extends SpaceTest {

	private static final String FILE_CONTENT = "This is a test file!";

	@Test
	public void shareWithDefaultSettings() throws IOException {

		// prepare
		prepareTest(false);
		SpaceDog test = resetTestBackend();
		SpaceDog vince = signUp(test, "vince", "hi vince", "vince@dog.com");
		SpaceDog fred = signUp(test, "fred", "hi fred", "fred@dog.com");

		// only admin can get all shared locations
		SpaceRequest.get("/1/share").backend(test).go(401);
		SpaceRequest.get("/1/share").auth(vince).go(403);

		// this account is brand new, no shared files
		SpaceRequest.get("/1/share").auth(test).go(200)//
				.assertSizeEquals(0, "results");

		// anonymous users are not allowed to share files
		SpaceRequest.put("/1/share/tweeter.png").backend(test).go(401);

		// vince shares a small png file
		byte[] pngBytes = ClassResources.loadToBytes(this, "tweeter.png");

		JsonNode json = SpaceRequest.put("/1/share/tweeter.png").auth(vince)//
				.bodyBytes(pngBytes)//
				.go(200).asJson();

		String pngPath = json.get("path").asText();
		String pngLocation = json.get("location").asText();
		String pngS3Location = json.get("s3").asText();

		// admin lists all shared files should return tweeter.png path only
		SpaceRequest.get("/1/share").auth(test).go(200)//
				.assertSizeEquals(1, "results")//
				.assertEquals(pngPath, "results.0.path");

		// anonymous gets shared file with its location
		byte[] downloadedBytes = SpaceRequest.get(pngLocation).go(200)//
				.assertHeaderEquals("image/png", SpaceHeaders.CONTENT_TYPE)//
				.assertHeaderEquals("vince", SpaceHeaders.SPACEDOG_OWNER)//
				.asBytes();

		assertArrayEquals(pngBytes, downloadedBytes);

		// download shared png file through S3 direct access
		downloadedBytes = SpaceRequest.get(pngS3Location).go(200)//
				.assertHeaderEquals("image/png", SpaceHeaders.CONTENT_TYPE)//
				.assertHeaderEquals(Integer.toString(pngBytes.length), //
						SpaceHeaders.CONTENT_LENGTH)//
				.asBytes();

		assertArrayEquals(pngBytes, downloadedBytes);

		// share small text file
		json = SpaceRequest.put("/1/share/test.txt").auth(fred)//
				.bodyBytes(FILE_CONTENT.getBytes())//
				.go(200).asJson();

		String txtPath = json.get("path").asText();
		String txtLocation = json.get("location").asText();
		String txtS3Location = json.get("s3").asText();

		// list all shared files should return 2 paths
		// get first page with only one path
		json = SpaceRequest.get("/1/share")//
				.size(1).auth(test)//
				.go(200)//
				.assertSizeEquals(1, "results")//
				.asJson();

		Set<String> all = Sets.newHashSet(Json7.get(json, "results.0.path").asText());

		// get the index of the next page in the first request response
		String next = json.get("next").asText();

		// get second (and last) page with only one path
		json = SpaceRequest.get("/1/share")//
				.queryParam("next", next)//
				.size(1).auth(test)//
				.go(200)//
				.assertSizeEquals(1, "results")//
				.assertNotPresent("next")//
				.asJson();

		// the set should contain both file paths
		all.add(Json7.get(json, "results.0.path").asText());
		Assert.assertTrue(all.contains(pngPath));
		Assert.assertTrue(all.contains(txtPath));

		// download shared text file
		String stringContent = SpaceRequest.get(txtLocation).backend(test).go(200)//
				// .assertHeaderEquals("gzip", SpaceHeaders.CONTENT_ENCODING)//
				.assertHeaderEquals("text/plain", SpaceHeaders.CONTENT_TYPE)//
				.assertHeaderEquals("fred", SpaceHeaders.SPACEDOG_OWNER)//
				.asString();

		Assert.assertEquals(FILE_CONTENT, stringContent);

		// download shared text file through direct S3 access
		stringContent = SpaceRequest.get(txtS3Location).go(200)//
				.assertHeaderEquals("text/plain", SpaceHeaders.CONTENT_TYPE)//
				.asString();

		Assert.assertEquals(FILE_CONTENT, stringContent);

		// only admin or owner can delete a shared file
		SpaceRequest.delete(txtLocation).go(401);
		SpaceRequest.delete(txtLocation).backend(test).go(401);
		SpaceRequest.delete(txtLocation).auth(vince).go(403);

		// owner (fred) can delete its own shared file (test.txt)
		SpaceRequest.delete(txtLocation).auth(fred).go(200);

		// list of shared files should only return the png file path
		SpaceRequest.get("/1/share").auth(test).go(200)//
				.assertSizeEquals(1, "results")//
				.assertEquals(pngPath, "results.0.path");

		// only admin can delete all shared files
		SpaceRequest.delete("/1/share").backend(test).go(401);
		SpaceRequest.delete("/1/share").auth(fred).go(403);
		SpaceRequest.delete("/1/share").auth(vince).go(403);
		SpaceRequest.delete("/1/share").auth(test).go(200)//
				.assertSizeEquals(1, "deleted")//
				.assertContains(TextNode.valueOf(pngPath), "deleted");

		SpaceRequest.get("/1/share").auth(test).go(200)//
				.assertSizeEquals(0, "results");

		// share small text file
		txtLocation = SpaceRequest.put("/1/share/test.txt").auth(fred)//
				.bodyBytes(FILE_CONTENT.getBytes())//
				.go(200)//
				.getString("location");

		// admin can delete shared file (test.txt) even if not owner
		// with default share ACL settings
		SpaceRequest.delete(txtLocation).auth(test).go(200);
		SpaceRequest.get("/1/share").auth(test).go(200)//
				.assertSizeEquals(0, "results");
	}

	@Test
	public void shareWithCustomSettings() throws IOException {

		// prepare
		prepareTest(false);
		SpaceDog test = resetTestBackend();
		SpaceDog vince = signUp(test, "vince", "hi vince", "vince@dog.com");
		SpaceDog fred = signUp(test, "fred", "hi fred", "fred@dog.com");
		byte[] pngBytes = ClassResources.loadToBytes(this, "tweeter.png");

		// super admin sets custom share permissions
		ShareSettings settings = new ShareSettings();
		settings.enableS3Location = false;
		settings.acl.put("key", Sets.newHashSet(DataPermission.create));
		settings.acl.put("user", Sets.newHashSet(DataPermission.create, DataPermission.read, //
				DataPermission.delete));
		settings.acl.put("admin", Sets.newHashSet(DataPermission.delete_all, DataPermission.search));
		test.settings().save(settings);

		// only admin can get all shared locations
		SpaceRequest.get("/1/share").backend(test).go(401);
		SpaceRequest.get("/1/share").auth(vince).go(403);

		// backend contains no shared file
		SpaceRequest.get("/1/share").auth(test).go(200)//
				.assertSizeEquals(0, "results");

		// anonymous is allowed to share a file
		String location = SpaceRequest.post("/1/share/guest.png")//
				.backend(test).bodyBytes(pngBytes).go(200)//
				.assertNotPresent("s3")//
				.getString("location");

		// backend contains 1 shared file
		SpaceRequest.get("/1/share").auth(test).go(200)//
				.assertSizeEquals(1, "results");

		// nobody is allowed to read this file
		SpaceRequest.get(location).go(401);
		SpaceRequest.get(location).auth(fred).go(403);
		SpaceRequest.get(location).auth(vince).go(403);
		SpaceRequest.get(location).auth(test).go(403);

		// nobody is allowed to delete this file but superadmins
		// since they got delete_all permission
		SpaceRequest.delete(location).go(401);
		SpaceRequest.delete(location).auth(fred).go(403);
		SpaceRequest.delete(location).auth(vince).go(403);
		SpaceRequest.delete(location).auth(test).go(200);

		// backend contains no shared file
		SpaceRequest.get("/1/share").auth(test).go(200)//
				.assertSizeEquals(0, "results");

		// vince is allowed to share a file
		location = SpaceRequest.post("/1/share/vince.png").auth(vince).bodyBytes(pngBytes).go(200)//
				.assertNotPresent("s3")//
				.getString("location");

		// backend contains 1 shared file
		SpaceRequest.get("/1/share").auth(test).go(200)//
				.assertSizeEquals(1, "results");

		// nobody is allowed to read this file but vince the owner
		SpaceRequest.get(location).go(401);
		SpaceRequest.get(location).auth(fred).go(403);
		SpaceRequest.get(location).auth(test).go(403);
		SpaceRequest.get(location).auth(vince).go(200);

		// nobody is allowed to delete this file
		// but vince the owner (and super admins)
		SpaceRequest.delete(location).go(401);
		SpaceRequest.delete(location).auth(fred).go(403);
		SpaceRequest.delete(location).auth(vince).go(200);

		// backend contains no shared file
		SpaceRequest.get("/1/share").auth(test).go(200)//
				.assertSizeEquals(0, "results");
	}

	@Test
	public void testSharingWithContentDispositionAndEscaping() {

		// prepare
		SpaceDog test = resetTestBackend();

		// share file with name that needs escaping
		ObjectNode json = SpaceRequest.put("/1/share/{fileName}")//
				.routeParam("fileName", "un petit text ?").auth(test)//
				.bodyBytes(FILE_CONTENT.getBytes())//
				.go(200)//
				.asJsonObject();

		String location = json.get("location").asText();
		String s3Location = json.get("s3").asText();

		// get file from location URI
		// no file extension => no specific content type
		String stringContent = SpaceRequest.get(location).backend(test).go(200)//
				.assertHeaderEquals("application/octet-stream", SpaceHeaders.CONTENT_TYPE)//
				.asString();

		Assert.assertEquals(FILE_CONTENT, stringContent);

		// get file from location URI with content disposition
		stringContent = SpaceRequest.get(location).backend(test)//
				.queryParam("withContentDisposition", "true").go(200)//
				.assertHeaderEquals("attachment; filename=\"un petit text ?\"", //
						SpaceHeaders.CONTENT_DISPOSITION)//
				.asString();

		Assert.assertEquals(FILE_CONTENT, stringContent);

		// get file from S3 location URI
		// no file extension => no specific content type
		// by default S3 returns content disposition header if set in metadata
		stringContent = SpaceRequest.get(s3Location).go(200)//
				.assertHeaderEquals("application/octet-stream", SpaceHeaders.CONTENT_TYPE)//
				.assertHeaderEquals("attachment; filename=\"un petit text ?\"", //
						SpaceHeaders.CONTENT_DISPOSITION)//
				.asString();

		Assert.assertEquals(FILE_CONTENT, stringContent);
	}

	@Test
	public void testDownloadMultipleShares() throws IOException {

		// prepare
		SpaceDog superadmin = resetTestBackend();
		byte[] pngBytes = Utils.readResource(this.getClass(), "tweeter.png");

		// prepare share settings
		ShareSettings settings = new ShareSettings();
		settings.enableS3Location = false;
		settings.acl.put("admin", Sets.newHashSet(DataPermission.read, DataPermission.create));
		superadmin.settings().save(settings);

		// superadmin shares file 1
		String path1 = superadmin.put("/1/share/toto.txt")//
				.bodyBytes("toto".getBytes())//
				.go(200).getString("path");

		// superadmin shares file 2
		String path2 = superadmin.put("/1/share/titi.txt")//
				.bodyBytes("titi".getBytes())//
				.go(200).getString("path");

		// superadmin shares file 3
		String path3 = superadmin.put("/1/share/tweeter.png")//
				.bodyBytes(pngBytes)//
				.go(200).getString("path");

		// superadmin shares file 4 with same name than file 1
		String path4 = superadmin.put("/1/share/toto.txt")//
				.bodyBytes("toto2".getBytes())//
				.go(200).getString("path");

		// superadmin needs read_all permission to downloads many shares
		superadmin.post("/1/share/_zip")//
				.bodyJson("fileName", "download.zip", //
						"paths", Json7.array(path1, path2, path3, path4))//
				.go(403);

		// superadmin updates share settings to allow admin
		// to download multiple shares
		settings = superadmin.settings().get(ShareSettings.class);
		settings.acl.put("admin", Sets.newHashSet(DataPermission.read_all, DataPermission.create));
		superadmin.settings().save(settings);

		// superadmin downloads zip containing specified shares
		byte[] zip = superadmin.post("/1/share/_zip")//
				.bodyJson("fileName", "download.zip", //
						"paths", Json7.array(path1, path2, path3, path4))//
				.go(200)//
				.assertHeaderContains("attachment; filename=\"download.zip\"", //
						SpaceHeaders.CONTENT_DISPOSITION)//
				.asBytes();

		assertEquals(4, zipFileNumber(zip));
		assertZipContains(zip, 1, "toto.txt", "toto".getBytes());
		assertZipContains(zip, 2, "titi.txt", "titi".getBytes());
		assertZipContains(zip, 3, "tweeter.png", pngBytes);
		assertZipContains(zip, 4, "toto.txt", "toto2".getBytes());
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
		assertTrue(entry.getName().endsWith(name));
		assertArrayEquals(file, ByteStreams.toByteArray(zipStream));
	}

	@Test
	public void testFileOwnershipErrorsDoNotDrainAllS3Connection() throws IOException {

		// prepare
		prepareTest(false);
		SpaceDog superadmin = resetTestBackend();
		SpaceDog vince = signUp(superadmin, "vince", "hi vince", "vince@dog.com");
		SpaceDog fred = signUp(superadmin, "fred", "hi fred", "fred@dog.com");

		// super admin sets custom share permissions
		ShareSettings settings = new ShareSettings();
		settings.acl = Maps.newHashMap();
		settings.acl.put("user", Sets.newHashSet(DataPermission.create, //
				DataPermission.read));
		superadmin.settings().save(settings);

		// vince shares a file
		String location = vince.post("/1/share/vince.txt")//
				.bodyBytes("vince".getBytes()).go(200)//
				.getString("location");

		// fred is not allowed to read vince's file
		// check ownership error do not drain s3 connection pool
		for (int i = 0; i < 75; i++)
			fred.get(location).go(403);
	}

	@Test
	public void shareDownloadAuthenticatedViaQueryParam() {

		// prepare
		prepareTest(false);
		SpaceDog superadmin = resetTestBackend();
		SpaceDog guest = SpaceDog.backend(superadmin);
		SpaceDog vince = signUp(superadmin, "vince", "hi vince", "vince@dog.com");
		SpaceDog fred = signUp(superadmin, "fred", "hi fred", "fred@dog.com");

		// super admin sets custom share permissions
		ShareSettings settings = new ShareSettings();
		settings.acl = Maps.newHashMap();
		settings.acl.put("user", Sets.newHashSet(DataPermission.create, //
				DataPermission.read));
		superadmin.settings().save(settings);

		// vince shares a file
		String location = vince.post("/1/share/vince.txt")//
				.bodyBytes("vince".getBytes()).go(200)//
				.getString("location");

		// guest fails to get shared file since no access token query param
		guest.get(location).go(401);

		// vince gets his shared file via access token query param
		byte[] bytes = guest.get(location)//
				.queryParam("accessToken", vince.accessToken().get())//
				.go(200).asBytes();

		assertArrayEquals("vince".getBytes(), bytes);

		// fred fails to get vince's shared file
		// via access token query param
		// since not the owner
		guest.get(location)//
				.queryParam("accessToken", fred.accessToken().get())//
				.go(403);
	}

	@Test
	public void shareUploadHasSizeLimit() {

		// prepare
		prepareTest(false);
		SpaceDog superadmin = resetTestBackend();

		// superadmin sets custom share permissions
		// with share size limit of 1 KB
		ShareSettings settings = new ShareSettings();
		settings.acl = Maps.newHashMap();
		settings.acl.put("super_admin", Sets.newHashSet(DataPermission.create));
		settings.shareSizeLimitInKB = 1;
		superadmin.settings().save(settings);

		// vince fails to share file with size of 2048 bytes
		// since settings forbids file with size above 1024 bytes
		superadmin.post("/1/share/toto.bin")//
				.bodyBytes(new byte[2048]).go(400);

		// vince succeeds to share file with size of 1024 bytes
		superadmin.post("/1/share/toto.bin")//
				.bodyBytes(new byte[1024]).go(200);
	}

	@Test
	public void shareDownloadGetsContentLengthHeaderIfNoGzipEncoding() throws IOException, InterruptedException {

		// prepare
		prepareTest(false);
		SpaceDog superadmin = resetTestBackend();
		SpaceDog vince = signUp(superadmin, "vince", "hi vince", "vince@dog.com");
		byte[] pngBytes = ClassResources.loadToBytes(this, "tweeter.png");

		// vince shares a small png file
		String pngLocation = vince.put("/1/share/tweeter.png")//
				.bodyBytes(pngBytes)//
				.go(200).getString("location");

		// anonymous gets shared file with its location
		// default stream encoding is chunked since content is gziped
		byte[] downloadedBytes = vince.get(pngLocation).go(200)//
				.assertHeaderEquals("chunked", SpaceHeaders.TRANSFER_ENCODING)//
				.assertHeaderNotPresent(SpaceHeaders.CONTENT_LENGTH)//
				.asBytes();

		assertArrayEquals(pngBytes, downloadedBytes);

		// anonymous gets shared file with its location
		// if request Accept-Encoding without gzip
		// then response contains Content-Length
		downloadedBytes = vince.get(pngLocation)//
				.addHeader(SpaceHeaders.ACCEPT_ENCODING, "identity")//
				.go(200)//
				.assertHeaderNotPresent(SpaceHeaders.TRANSFER_ENCODING)//
				.assertHeaderEquals(Integer.toString(pngBytes.length), //
						SpaceHeaders.CONTENT_LENGTH)//
				.asBytes();

		assertArrayEquals(pngBytes, downloadedBytes);
	}

	@Test
	public void bigFileUploadIsDelayed() throws IOException, InterruptedException {

		// prepare
		prepareTest(false);
		SpaceDog superadmin = resetTestBackend();
		SpaceDog vince = signUp(superadmin, "vince", "hi vince", "vince@dog.com");
		SpaceDog nath = signUp(superadmin, "nath", "hi nath", "nath@dog.com");
		byte[] pngBytes = ClassResources.loadToBytes(this, "tweeter.png");

		// superadmin sets custom share permissions
		ShareSettings settings = new ShareSettings();
		settings.enableS3Location = false;
		settings.acl.put("user", Sets.newHashSet(DataPermission.create, DataPermission.read, //
				DataPermission.delete));
		settings.acl.put("admin", Sets.newHashSet(DataPermission.delete_all, DataPermission.search));
		superadmin.settings().save(settings);

		// vince wants to upload big file
		// vince sets delay = true to force delayed upload to s3
		SpaceResponse response = vince.post("/1/share/tweeter.png")//
				.queryParam(PARAM_DELAY, "true")//
				.withContentType("image/png")//
				.go(202);

		String downloadLocation = response.getString("location");
		String uploadLocation = response.getString("uploadTo");

		// vince uploads file via 'uploadTo' location
		SpaceRequest.put(uploadLocation)//
				.withContentType("image/png")//
				.bodyBytes(pngBytes)//
				.go(200);

		// vince download file
		byte[] downloadedBytes = vince.get(downloadLocation)//
				.addHeader(SpaceHeaders.ACCEPT_ENCODING, "identity")//
				.queryParam(SpaceParams.PARAM_WITH_CONTENT_DISPOSITION, "true").go(200)//
				.assertHeaderEquals(Long.toString(pngBytes.length), //
						SpaceHeaders.CONTENT_LENGTH)//
				// .assertHeaderEquals(SpaceHeaders.contentDisposition("tweeter.png"), //
				// SpaceHeaders.CONTENT_DISPOSITION)//
				.asBytes();

		assertArrayEquals(pngBytes, downloadedBytes);

		// nath is not allowed to download file since not the owner
		nath.get(downloadLocation).go(403);
	}

}
