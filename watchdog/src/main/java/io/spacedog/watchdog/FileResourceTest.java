package io.spacedog.watchdog;

import org.junit.Test;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceDogHelper.Backend;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.SpaceHeaders;
import io.spacedog.watchdog.SpaceSuite.TestOncePerDay;

@TestOncePerDay
public class FileResourceTest {

	private static final byte[] BYTES = "This is a test file!".getBytes();

	@Test
	public void test() throws Exception {

		Backend testBackend = SpaceDogHelper.resetTestBackend();

		SpaceRequest.get("/1/file").adminAuth(testBackend).go(200)//
				.assertSizeEquals(0, "results");

		SpaceRequest.put("/1/file/app.html").adminAuth(testBackend).body(BYTES).go(200);
		SpaceRequest.put("/1/file/app.js").adminAuth(testBackend).body(BYTES).go(200);
		SpaceRequest.put("/1/file/images/riri.png").adminAuth(testBackend).body(BYTES).go(200);
		SpaceRequest.put("/1/file/images/fifi.jpg").adminAuth(testBackend).body(BYTES).go(200);
		SpaceRequest.put("/1/file/css/black.css").adminAuth(testBackend).body(BYTES).go(200);
		SpaceRequest.put("/1/file/css/white.css").adminAuth(testBackend).body(BYTES).go(200);

		SpaceRequest.get("/1/file").adminAuth(testBackend).go(200)//
				.assertSizeEquals(6, "results");

		SpaceRequest.get("/1/file/app.html").backend(testBackend).go(200)//
				.assertHeaderEquals("text/html", SpaceHeaders.CONTENT_TYPE);

		SpaceRequest.get("/1/file/app.js").backend(testBackend).go(200)//
				.assertHeaderEquals("application/javascript", SpaceHeaders.CONTENT_TYPE);

		SpaceRequest.get("/1/file/css/black.css").backend(testBackend).go(200)//
				.assertHeaderEquals("text/html", SpaceHeaders.CONTENT_TYPE);

		SpaceRequest.get("/1/file/images").adminAuth(testBackend).go(200)//
				.assertSizeEquals(2, "results");

		SpaceRequest.delete("/1/file/css").adminAuth(testBackend).go(200)//
				.assertSizeEquals(2, "deleted");

		SpaceRequest.get("/1/file").adminAuth(testBackend).go(200)//
				.assertSizeEquals(4, "results");

		SpaceRequest.get("/1/file/css/black.css").backend(testBackend).go(404);

		SpaceRequest.delete("/1/file").adminAuth(testBackend).go(200)//
				.assertSizeEquals(4, "deleted");
	}
}
