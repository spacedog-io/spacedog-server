package io.spacedog.watchdog;

import org.junit.Test;

import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceTest;
import io.spacedog.utils.SpaceHeaders;
import io.spacedog.watchdog.SpaceSuite.TestOncePerDay;

@TestOncePerDay
public class FileResourceTestOncePerDay extends SpaceTest {

	private static final byte[] BYTES = "This is a test file!".getBytes();

	@Test
	public void test() throws Exception {

		prepareTest(false);
		Backend test = resetTestBackend();

		SpaceRequest.get("/1/file").adminAuth(test).go(200)//
				.assertSizeEquals(0, "results");

		// upload file without prefix is illegal
		SpaceRequest.put("/1/file/index.html").adminAuth(test).body(BYTES).go(400);

		// admin can upload a web site
		SpaceRequest.put("/1/file/www/app.html").adminAuth(test).body(BYTES).go(200);
		SpaceRequest.put("/1/file/www/app.js").adminAuth(test).body(BYTES).go(200);
		SpaceRequest.put("/1/file/www/images/riri.png").adminAuth(test).body(BYTES).go(200);
		SpaceRequest.put("/1/file/www/images/fifi.jpg").adminAuth(test).body(BYTES).go(200);
		SpaceRequest.put("/1/file/www/css/black.css").adminAuth(test).body(BYTES).go(200);
		SpaceRequest.put("/1/file/www/css/white.css").adminAuth(test).body(BYTES).go(200);

		SpaceRequest.get("/1/file").adminAuth(test).go(200)//
				.assertSizeEquals(6, "results");

		SpaceRequest.get("/1/file/www/app.html").backend(test).go(200)//
				.assertHeaderEquals("text/html", SpaceHeaders.CONTENT_TYPE);

		SpaceRequest.get("/1/file/www/app.js").backend(test).go(200)//
				.assertHeaderEquals("application/javascript", SpaceHeaders.CONTENT_TYPE);

		SpaceRequest.get("/1/file/www/css/black.css").backend(test).go(200)//
				.assertHeaderEquals("text/css", SpaceHeaders.CONTENT_TYPE);

		SpaceRequest.get("/1/file/www/images").adminAuth(test).go(200)//
				.assertSizeEquals(2, "results");

		SpaceRequest.delete("/1/file/www/css").adminAuth(test).go(200)//
				.assertSizeEquals(2, "deleted");

		SpaceRequest.get("/1/file/www").adminAuth(test).go(200)//
				.assertSizeEquals(4, "results");

		SpaceRequest.get("/1/file/www/css/black.css").backend(test).go(404);

		SpaceRequest.delete("/1/file").adminAuth(test).go(200)//
				.assertSizeEquals(4, "deleted");
	}
}
