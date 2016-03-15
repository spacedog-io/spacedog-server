package io.spacedog.watchdog;

import org.junit.Test;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceDogHelper.Backend;
import io.spacedog.client.SpaceRequest;
import io.spacedog.watchdog.SpaceSuite.TestOncePerDay;

@TestOncePerDay
public class FileResourceTest {

	private static final byte[] BYTES = "This is a test file!".getBytes();

	@Test
	public void test() throws Exception {

		Backend testBackend = SpaceDogHelper.resetTestBackend();

		SpaceRequest.get("/1/file").basicAuth(testBackend).go(200)//
				.assertSizeEquals(0, "results");

		SpaceRequest.put("/1/file/app.html").basicAuth(testBackend).body(BYTES).go(200);
		SpaceRequest.put("/1/file/app.js").basicAuth(testBackend).body(BYTES).go(200);
		SpaceRequest.put("/1/file/images/riri.png").basicAuth(testBackend).body(BYTES).go(200);
		SpaceRequest.put("/1/file/images/fifi.jpg").basicAuth(testBackend).body(BYTES).go(200);
		SpaceRequest.put("/1/file/css/black.css").basicAuth(testBackend).body(BYTES).go(200);
		SpaceRequest.put("/1/file/css/white.css").basicAuth(testBackend).body(BYTES).go(200);

		SpaceRequest.get("/1/file").basicAuth(testBackend).go(200)//
				.assertSizeEquals(6, "results");

		SpaceRequest.get("/1/file/app.html").backend(testBackend).go(200);
		SpaceRequest.get("/1/file/css/black.css").backend(testBackend).go(200);

		SpaceRequest.get("/1/file/images").basicAuth(testBackend).go(200)//
				.assertSizeEquals(2, "results");

		SpaceRequest.delete("/1/file/css").basicAuth(testBackend).go(200)//
				.assertSizeEquals(2, "deleted");

		SpaceRequest.get("/1/file").basicAuth(testBackend).go(200)//
				.assertSizeEquals(4, "results");

		SpaceRequest.get("/1/file/css/black.css").backend(testBackend).go(404);

		SpaceRequest.delete("/1/file").basicAuth(testBackend).go(200)//
				.assertSizeEquals(4, "deleted");
	}
}
