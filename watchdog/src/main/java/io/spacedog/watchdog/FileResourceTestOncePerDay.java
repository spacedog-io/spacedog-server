package io.spacedog.watchdog;

import org.junit.Test;

import io.spacedog.client.FileEndpoint.FileList;
import io.spacedog.client.SpaceDog;
import io.spacedog.http.SpaceRequest;
import io.spacedog.http.SpaceTest;
import io.spacedog.utils.SpaceHeaders;

public class FileResourceTestOncePerDay extends SpaceTest {

	private static final byte[] XXX = "XXX".getBytes();

	@Test
	public void test() throws Exception {

		// prepare
		prepareTest(false);
		SpaceDog superadmin = resetTestBackend();

		// superadmin checks backend is truly empty
		FileList list = superadmin.file().list("/");
		assertEquals(0, list.files.length);

		// upload file without prefix is illegal
		superadmin.put("/1/file").bodyBytes(XXX).go(400);

		// admin can upload a web site
		superadmin.file().save("/www/app.html", "/www/app.html".getBytes());
		superadmin.file().save("/www/app.js", "/www/app.js".getBytes());
		superadmin.file().save("/www/images/riri.png", "/www/images/riri.png".getBytes());
		superadmin.file().save("/www/images/fifi.jpg", "/www/images/fifi.jpg".getBytes());
		superadmin.file().save("/www/css/black.css", "/www/css/black.css".getBytes());
		superadmin.file().save("/www/css/white.css", "/www/css/white.css".getBytes());

		list = superadmin.file().list("/");
		assertEquals(6, list.files.length);

		// superadmin gets app.html
		byte[] bytes = superadmin.file().get("/www/app.html");
		assertArrayEquals("/www/app.html".getBytes(), bytes);

		// superadmin gets file and to check content types
		SpaceRequest.get("/1/file/www/app.html").backend(superadmin).go(200)//
				.assertHeaderEquals("text/html", SpaceHeaders.CONTENT_TYPE);

		SpaceRequest.get("/1/file/www/app.js").backend(superadmin).go(200)//
				.assertHeaderEquals("application/javascript", SpaceHeaders.CONTENT_TYPE);

		SpaceRequest.get("/1/file/www/css/black.css").backend(superadmin).go(200)//
				.assertHeaderEquals("text/css", SpaceHeaders.CONTENT_TYPE);

		// superadmin lists all images
		list = superadmin.file().list("/www/images");
		assertEquals(2, list.files.length);

		// superadmin deletes all css files
		superadmin.file().delete("/www/css");
		list = superadmin.file().list("/www/css");
		assertEquals(0, list.files.length);

		// superadmin fails to get just deleted css file
		superadmin.get("/1/file/www/css/black.css").go(404);

		// superadmin deletes all files
		superadmin.file().delete("/");
		list = superadmin.file().list("/");
		assertEquals(0, list.files.length);
	}
}
