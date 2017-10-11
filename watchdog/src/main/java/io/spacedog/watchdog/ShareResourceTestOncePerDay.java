package io.spacedog.watchdog;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;

import io.spacedog.client.SpaceDog;
import io.spacedog.http.SpaceRequest;
import io.spacedog.http.SpaceTest;
import io.spacedog.model.DataPermission;
import io.spacedog.model.ShareSettings;
import io.spacedog.utils.Json;
import io.spacedog.utils.SpaceHeaders;

public class ShareResourceTestOncePerDay extends SpaceTest {

	private static final String FILE_CONTENT = "This is a test file!";

	@Test
	public void shareWithDefaultSettings() throws IOException {

		// prepare
		prepareTest(false);
		SpaceDog test = resetTestBackend();
		SpaceDog vince = createTempUser(test, "vince");
		SpaceDog fred = createTempUser(test, "fred");

		// only admin can get all shared locations
		SpaceRequest.get("/1/share").backend(test).go(403);
		SpaceRequest.get("/1/share").auth(vince).go(403);

		// this account is brand new, no shared files
		SpaceRequest.get("/1/share").auth(test).go(200)//
				.assertSizeEquals(0, "results");

		// anonymous users are not allowed to share files
		SpaceRequest.put("/1/share/tweeter.png").backend(test).go(403);

		// vince shares a small png file
		byte[] pngBytes = Resources.toByteArray(//
				Resources.getResource(this.getClass(), "tweeter.png"));

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
				// .assertHeaderEquals("gzip", SpaceHeaders.CONTENT_ENCODING)//
				.assertHeaderEquals("image/png", SpaceHeaders.CONTENT_TYPE)//
				.assertHeaderEquals(vince.id(), SpaceHeaders.SPACEDOG_OWNER)//
				.asBytes();

		Assert.assertTrue(Arrays.equals(pngBytes, downloadedBytes));

		// download shared png file through S3 direct access
		downloadedBytes = SpaceRequest.get(pngS3Location).go(200)//
				.assertHeaderEquals("image/png", SpaceHeaders.CONTENT_TYPE)//
				.asBytes();

		Assert.assertTrue(Arrays.equals(pngBytes, downloadedBytes));

		// share small text file
		json = SpaceRequest.put("/1/share/test.txt").auth(fred)//
				.bodyBytes(FILE_CONTENT.getBytes())//
				.go(200)//
				.asJson();

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

		Set<String> all = Sets.newHashSet(Json.get(json, "results.0.path").asText());

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
		all.add(Json.get(json, "results.0.path").asText());
		Assert.assertTrue(all.contains(pngPath));
		Assert.assertTrue(all.contains(txtPath));

		// download shared text file
		String stringContent = SpaceRequest.get(txtLocation).backend(test).go(200)//
				// .assertHeaderEquals("gzip", SpaceHeaders.CONTENT_ENCODING)//
				.assertHeaderEquals("text/plain", SpaceHeaders.CONTENT_TYPE)//
				.assertHeaderEquals(fred.id(), SpaceHeaders.SPACEDOG_OWNER)//
				.asString();

		Assert.assertEquals(FILE_CONTENT, stringContent);

		// download shared text file through direct S3 access
		stringContent = SpaceRequest.get(txtS3Location).go(200)//
				.assertHeaderEquals("text/plain", SpaceHeaders.CONTENT_TYPE)//
				.asString();

		Assert.assertEquals(FILE_CONTENT, stringContent);

		// only admin or owner can delete a shared file
		SpaceRequest.delete(txtLocation).go(403);
		SpaceRequest.delete(txtLocation).backend(test).go(403);
		SpaceRequest.delete(txtLocation).auth(vince).go(403);

		// owner (fred) can delete its own shared file (test.txt)
		SpaceRequest.delete(txtLocation).auth(fred).go(200);

		// list of shared files should only return the png file path
		SpaceRequest.get("/1/share").auth(test).go(200)//
				.assertSizeEquals(1, "results")//
				.assertEquals(pngPath, "results.0.path");

		// only admin can delete all shared files
		SpaceRequest.delete("/1/share").backend(test).go(403);
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
		SpaceDog vince = createTempUser(test, "vince");
		SpaceDog fred = createTempUser(test, "fred");
		byte[] pngBytes = Resources.toByteArray(//
				Resources.getResource("io/spacedog/watchdog/tweeter.png"));

		// super admin sets custom share permissions
		ShareSettings settings = new ShareSettings();
		settings.enableS3Location = false;
		settings.acl.put("all", Sets.newHashSet(DataPermission.create));
		settings.acl.put("user", Sets.newHashSet(DataPermission.create, DataPermission.read, //
				DataPermission.delete));
		settings.acl.put("admin", Sets.newHashSet(DataPermission.delete_all, DataPermission.search));
		test.settings().save(settings);

		// only admin can get all shared locations
		SpaceRequest.get("/1/share").backend(test).go(403);
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
		// but superadmins and superdogs
		SpaceRequest.get(location).go(403);
		SpaceRequest.get(location).auth(fred).go(403);
		SpaceRequest.get(location).auth(vince).go(403);
		SpaceRequest.get(location).auth(test).go(200);

		// nobody is allowed to read this file but super admins
		// since they got delete_all permission
		SpaceRequest.delete(location).go(403);
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

		// nobody is allowed to read this file
		// but vince the owner (and superadmin/dogs)
		SpaceRequest.get(location).go(403);
		SpaceRequest.get(location).auth(fred).go(403);
		SpaceRequest.get(location).auth(test).go(200);
		SpaceRequest.get(location).auth(vince).go(200);

		// nobody is allowed to delete this file
		// but vince the owner (and superadmin/dogs)
		SpaceRequest.delete(location).go(403);
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

	void upload(String putUrl, String content, String contentType, String fileName, String username, String userType) {
		SpaceRequest.put(putUrl)//
				.setHeader("x-amz-meta-username", username)//
				.setHeader("x-amz-meta-user-type", userType)//
				.setHeader("Content-Type", contentType)//
				.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", fileName))//
				.bodyBytes(content.getBytes())//
				.go(200);
	}

}
