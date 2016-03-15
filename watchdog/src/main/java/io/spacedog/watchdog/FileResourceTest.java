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

		Backend testAccount = SpaceDogHelper.resetTestBackend();

		SpaceRequest.get("/1/file").basicAuth(testAccount).go(200)//
				.assertSizeEquals(0, "results");

		SpaceRequest.put("/1/file/app.html").basicAuth(testAccount).body(BYTES).go(200);
		SpaceRequest.put("/1/file/app.js").basicAuth(testAccount).body(BYTES).go(200);
		SpaceRequest.put("/1/file/images/riri.png").basicAuth(testAccount).body(BYTES).go(200);
		SpaceRequest.put("/1/file/images/fifi.jpg").basicAuth(testAccount).body(BYTES).go(200);
		SpaceRequest.put("/1/file/css/black.css").basicAuth(testAccount).body(BYTES).go(200);
		SpaceRequest.put("/1/file/css/white.css").basicAuth(testAccount).body(BYTES).go(200);

		SpaceRequest.get("/1/file").basicAuth(testAccount).go(200)//
				.assertSizeEquals(6, "results");

		SpaceRequest.get("/1/file/app.html").backend(testAccount).go(200);
		SpaceRequest.get("/1/file/css/black.css").backend(testAccount).go(200);

		SpaceRequest.get("/1/file/images").basicAuth(testAccount).go(200)//
				.assertSizeEquals(2, "results");

		SpaceRequest.delete("/1/file/css").basicAuth(testAccount).go(200)//
				.assertSizeEquals(2, "deleted");

		SpaceRequest.get("/1/file").basicAuth(testAccount).go(200)//
				.assertSizeEquals(4, "results");

		SpaceRequest.get("/1/file/css/black.css").backend(testAccount).go(404);

		SpaceRequest.delete("/1/file").basicAuth(testAccount).go(200)//
				.assertSizeEquals(4, "deleted");
	}
}
