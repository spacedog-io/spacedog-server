package io.spacedog.watchdog;

import org.junit.Test;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceDogHelper.Account;
import io.spacedog.client.SpaceRequest;

public class FileResourceTest {

	private static final byte[] BYTES = "This is a test file!".getBytes();

	@Test
	public void test() throws Exception {

		Account testAccount = SpaceDogHelper.resetTestAccount();

		SpaceRequest.get("/v1/file").basicAuth(testAccount).go(200)//
				.assertSizeEquals(0, "results");

		SpaceRequest.put("/v1/file/app.html").basicAuth(testAccount).body(BYTES).go(200);
		SpaceRequest.put("/v1/file/app.js").basicAuth(testAccount).body(BYTES).go(200);
		SpaceRequest.put("/v1/file/images/riri.png").basicAuth(testAccount).body(BYTES).go(200);
		SpaceRequest.put("/v1/file/images/fifi.jpg").basicAuth(testAccount).body(BYTES).go(200);
		SpaceRequest.put("/v1/file/css/black.css").basicAuth(testAccount).body(BYTES).go(200);
		SpaceRequest.put("/v1/file/css/white.css").basicAuth(testAccount).body(BYTES).go(200);

		SpaceRequest.get("/v1/file").basicAuth(testAccount).go(200)//
				.assertSizeEquals(6, "results");

		SpaceRequest.get("/v1/file/app.html").backendKey(testAccount).go(200);
		SpaceRequest.get("/v1/file/css/black.css").backendKey(testAccount).go(200);

		SpaceRequest.get("/v1/file/images").basicAuth(testAccount).go(200)//
				.assertSizeEquals(2, "results");

		SpaceRequest.delete("/v1/file/css").basicAuth(testAccount).go(200)//
				.assertSizeEquals(2, "deleted");

		SpaceRequest.get("/v1/file").basicAuth(testAccount).go(200)//
				.assertSizeEquals(4, "results");

		SpaceRequest.get("/v1/file/css/black.css").backendKey(testAccount).go(404);

		SpaceRequest.delete("/v1/file").basicAuth(testAccount).go(200)//
				.assertSizeEquals(4, "deleted");
	}
}
