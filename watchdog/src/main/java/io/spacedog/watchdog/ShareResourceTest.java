package io.spacedog.watchdog;

import java.util.Arrays;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceDogHelper.Backend;
import io.spacedog.client.SpaceDogHelper.User;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Json;
import io.spacedog.watchdog.SpaceSuite.TestOncePerDay;

@TestOncePerDay
public class ShareResourceTest {

	private static final String FILE_CONTENT = "This is a test file!";

	@Test
	public void shareListAndGetFiles() throws Exception {

		// prepare
		Backend testAccount = SpaceDogHelper.resetTestBackend();
		User vince = SpaceDogHelper.createUser(testAccount, "vince", "hi vince", "vince@dog.com");
		User fred = SpaceDogHelper.createUser(testAccount, "fred", "hi fred", "fred@dog.com");

		// only admin can get all shared locations
		SpaceRequest.get("/1/share").go(401);
		SpaceRequest.get("/1/share").backend(testAccount).go(401);
		SpaceRequest.get("/1/share").basicAuth(vince).go(401);

		// this account is brand new, no shared files
		SpaceRequest.get("/1/share").basicAuth(testAccount).go(200)//
				.assertSizeEquals(0, "results");

		// only users and admins are allowed to share files
		SpaceRequest.put("/1/share/tweeter.png").go(401);
		SpaceRequest.put("/1/share/tweeter.png").backend(testAccount).go(401);

		// share a small png file
		byte[] pngBytes = Resources.toByteArray(//
				Resources.getResource("io/spacedog/watchdog/tweeter.png"));

		JsonNode json = SpaceRequest.put("/1/share/tweeter.png")//
				.backend(testAccount)//
				.basicAuth(vince)//
				.body(pngBytes)//
				.go(200).jsonNode();

		String pngPath = json.get("path").asText();
		String pngLocation = json.get("location").asText();
		String pngS3Location = json.get("s3").asText();

		// list all shared files should return tweeter.png path only
		SpaceRequest.get("/1/share").basicAuth(testAccount).go(200)//
				.assertSizeEquals(1, "results")//
				.assertEquals(pngPath, "results.0.path");

		// Anonymous are allowed to access shared files if they get the location
		// but they need to provide the backend key until the backendId is part
		// of the host
		SpaceRequest.get(pngLocation).go(401);

		// download shared png file
		byte[] downloadedBytes = SpaceRequest.get(pngLocation)//
				.backend(testAccount)//
				.go(200)//
				// .assertHeaderEquals("image/png", "content-type")//
				.assertHeaderEquals("vince", "x-amz-meta-owner")//
				.assertHeaderEquals("USER", "x-amz-meta-owner-type")//
				.bytes();

		Assert.assertTrue(Arrays.equals(pngBytes, downloadedBytes));

		// download shared png file through S3 direct access

		downloadedBytes = SpaceRequest.get(pngS3Location).go(200)//
				// .assertHeaderEquals("image/png", "content-type")//
				.assertHeaderEquals("vince", "x-amz-meta-owner")//
				.assertHeaderEquals("USER", "x-amz-meta-owner-type")//
				.bytes();

		Assert.assertTrue(Arrays.equals(pngBytes, downloadedBytes));

		// share small text file
		json = SpaceRequest.put("/1/share/test.txt")//
				.backend(testAccount).basicAuth(fred)//
				.body(FILE_CONTENT.getBytes())//
				.go(200)//
				.jsonNode();

		String txtPath = json.get("path").asText();
		String txtLocation = json.get("location").asText();
		String txtS3Location = json.get("s3").asText();

		// list all shared files should return 2 paths
		// get first page with only one path
		json = SpaceRequest.get("/1/share")//
				.queryString("size", "1")//
				.basicAuth(testAccount)//
				.go(200)//
				.assertSizeEquals(1, "results")//
				.jsonNode();

		Set<String> all = Sets.newHashSet(Json.get(json, "results.0.path").asText());

		// get the index of the next page in the first request response
		String next = json.get("next").asText();

		// get second (and last) page with only one path
		json = SpaceRequest.get("/1/share")//
				.queryString("size", "1")//
				.queryString("next", next)//
				.basicAuth(testAccount)//
				.go(200)//
				.assertSizeEquals(1, "results")//
				.assertNotPresent("next")//
				.jsonNode();

		// the set should contain both file paths
		all.add(Json.get(json, "results.0.path").asText());
		Assert.assertTrue(all.contains(pngPath));
		Assert.assertTrue(all.contains(txtPath));

		// download shared text file
		String stringContent = SpaceRequest.get(txtLocation).backend(testAccount).go(200)//
				// .assertHeaderEquals("text/plain", "content-type")//
				.assertHeaderEquals("fred", "x-amz-meta-owner")//
				.assertHeaderEquals("USER", "x-amz-meta-owner-type")//
				.httpResponse().getBody();

		Assert.assertEquals(FILE_CONTENT, stringContent);

		// download shared text file through direct S3 access

		stringContent = SpaceRequest.get(txtS3Location).go(200)//
				// .assertHeaderEquals("text/plain", "content-type")//
				.assertHeaderEquals("fred", "x-amz-meta-owner")//
				.assertHeaderEquals("USER", "x-amz-meta-owner-type")//
				.httpResponse().getBody();

		Assert.assertEquals(FILE_CONTENT, stringContent);

		// only admin or owner can delete a shared file
		SpaceRequest.delete(txtLocation).go(401);
		SpaceRequest.delete(txtLocation).backend(testAccount).go(401);
		SpaceRequest.delete(txtLocation).backend(testAccount).basicAuth(vince).go(401);

		// owner (fred) can delete its own shared file (test.txt)
		SpaceRequest.delete(txtLocation).backend(testAccount).basicAuth(fred).go(200);

		// list of shared files should only return the png file path
		SpaceRequest.get("/1/share").basicAuth(testAccount).go(200)//
				.assertSizeEquals(1, "results")//
				.assertEquals(pngPath, "results.0.path");

		// only admin can delete all shared files
		SpaceRequest.delete("/1/share").go(401);
		SpaceRequest.delete("/1/share").backend(testAccount).go(401);
		SpaceRequest.delete("/1/share").basicAuth(fred).go(401);
		SpaceRequest.delete("/1/share").basicAuth(vince).go(401);
		SpaceRequest.delete("/1/share").basicAuth(testAccount).go(200)//
				.assertSizeEquals(1, "deleted")//
				.assertContains(TextNode.valueOf(pngPath), "deleted");

		SpaceRequest.get("/1/share").basicAuth(testAccount).go(200)//
				.assertSizeEquals(0, "results");

	}

	void upload(String putUrl, String content, String contentType, String fileName, String username, String userType)
			throws UnirestException {
		HttpResponse<String> response = Unirest.put(putUrl)//
				.header("x-amz-meta-username", username)//
				.header("x-amz-meta-user-type", userType)//
				.header("Content-Type", contentType)//
				.header("Content-Disposition", String.format("attachment; filename=\"%s\"", fileName))//
				.body(content.getBytes())//
				.asString();

		System.out.println();
		System.out.println(String.format("PUT %s => %s %s", //
				putUrl, response.getStatus(), response.getStatusText()));
		System.out.println("Response body = " + response.getBody());

		Assert.assertEquals(200, response.getStatus());
	}
}
