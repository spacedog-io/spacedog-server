package io.spacedog.test;

import org.junit.Test;

import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.SpaceHeaders;

public class FileResourceTestOncePerDay extends SpaceTest {

	private static final byte[] BYTES = "This is a test file!".getBytes();

	@Test
	public void test() throws Exception {

		prepareTest(false);
		SpaceDog superadmin = resetTestBackend();
		SpaceDog guest = SpaceDog.backend(superadmin);

		// invalid uri throws 404
		superadmin.get("/1/filetoto").go(404);

		// invalid method throws 405
		superadmin.post("/1/file").go(405);

		// superadmin gets file root list
		superadmin.get("/1/file").go(200)//
				.assertSizeEquals(0, "results");

		// upload file without prefix is illegal
		superadmin.put("/1/file/index.html").bodyBytes(BYTES).go(400);

		// admin can upload a web site
		superadmin.put("/1/file/www/app.html").bodyBytes(BYTES).go(200);
		superadmin.put("/1/file/www/app.js").bodyBytes(BYTES).go(200);
		superadmin.put("/1/file/www/images/riri.png").bodyBytes(BYTES).go(200);
		superadmin.put("/1/file/www/images/fifi.jpg").bodyBytes(BYTES).go(200);
		superadmin.put("/1/file/www/css/black.css").bodyBytes(BYTES).go(200);
		superadmin.put("/1/file/www/css/white.css").bodyBytes(BYTES).go(200);

		superadmin.get("/1/file").go(200)//
				.assertSizeEquals(6, "results");

		guest.get("/1/file/www/app.html").go(200)//
				.assertHeaderEquals("text/html", SpaceHeaders.CONTENT_TYPE);

		guest.get("/1/file/www/app.js").go(200)//
				.assertHeaderEquals("application/javascript", SpaceHeaders.CONTENT_TYPE);

		guest.get("/1/file/www/css/black.css").go(200)//
				.assertHeaderEquals("text/css", SpaceHeaders.CONTENT_TYPE);

		superadmin.get("/1/file/www/images").go(200)//
				.assertSizeEquals(2, "results");

		superadmin.delete("/1/file/www/css").go(200)//
				.assertSizeEquals(2, "deleted");

		superadmin.get("/1/file/www").go(200)//
				.assertSizeEquals(4, "results");

		guest.get("/1/file/www/css/black.css").go(404);

		superadmin.delete("/1/file").go(200)//
				.assertSizeEquals(4, "deleted");
	}
}
