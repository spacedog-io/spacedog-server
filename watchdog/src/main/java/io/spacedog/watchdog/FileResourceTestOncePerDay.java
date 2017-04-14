package io.spacedog.watchdog;

import org.junit.Test;

import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.SpaceHeaders;

public class FileResourceTestOncePerDay extends SpaceTest {

	private static final byte[] BYTES = "This is a test file!".getBytes();

	@Test
	public void test() throws Exception {

		prepareTest(false);
		SpaceDog test = resetTestBackend();

		SpaceRequest.get("/1/file").auth(test).go(200)//
				.assertSizeEquals(0, "results");

		// upload file without prefix is illegal
		SpaceRequest.put("/1/file/index.html").auth(test).bodyBytes(BYTES).go(400);

		// admin can upload a web site
		SpaceRequest.put("/1/file/www/app.html").auth(test).bodyBytes(BYTES).go(200);
		SpaceRequest.put("/1/file/www/app.js").auth(test).bodyBytes(BYTES).go(200);
		SpaceRequest.put("/1/file/www/images/riri.png").auth(test).bodyBytes(BYTES).go(200);
		SpaceRequest.put("/1/file/www/images/fifi.jpg").auth(test).bodyBytes(BYTES).go(200);
		SpaceRequest.put("/1/file/www/css/black.css").auth(test).bodyBytes(BYTES).go(200);
		SpaceRequest.put("/1/file/www/css/white.css").auth(test).bodyBytes(BYTES).go(200);

		SpaceRequest.get("/1/file").auth(test).go(200)//
				.assertSizeEquals(6, "results");

		SpaceRequest.get("/1/file/www/app.html").backend(test).go(200)//
				.assertHeaderEquals("text/html", SpaceHeaders.CONTENT_TYPE);

		SpaceRequest.get("/1/file/www/app.js").backend(test).go(200)//
				.assertHeaderEquals("application/javascript", SpaceHeaders.CONTENT_TYPE);

		SpaceRequest.get("/1/file/www/css/black.css").backend(test).go(200)//
				.assertHeaderEquals("text/css", SpaceHeaders.CONTENT_TYPE);

		SpaceRequest.get("/1/file/www/images").auth(test).go(200)//
				.assertSizeEquals(2, "results");

		SpaceRequest.delete("/1/file/www/css").auth(test).go(200)//
				.assertSizeEquals(2, "deleted");

		SpaceRequest.get("/1/file/www").auth(test).go(200)//
				.assertSizeEquals(4, "results");

		SpaceRequest.get("/1/file/www/css/black.css").backend(test).go(404);

		SpaceRequest.delete("/1/file").auth(test).go(200)//
				.assertSizeEquals(4, "deleted");
	}
}
