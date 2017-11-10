package io.spacedog.test;

import org.junit.Test;

import com.google.common.collect.Sets;

import io.spacedog.client.FileEndpoint.FileList;
import io.spacedog.client.FileEndpoint.SpaceFile;
import io.spacedog.client.SpaceDog;

public class FileServiceTest extends SpaceTest {

	@Test
	public void test() throws Exception {

		// prepare
		prepareTest(false);
		SpaceDog superadmin = resetTestBackend();
		SpaceDog guest = SpaceDog.backend(superadmin.backend());

		// superadmin checks backend is truly empty
		assertEquals(0, superadmin.files().listAll().files.length);

		// upload file without prefix is illegal
		superadmin.put("/1/files").go(400);

		// admin can upload a web site
		superadmin.files().upload("/www/app.html", "/www/app.html".getBytes());
		superadmin.files().upload("/www/app.js", "/www/app.js".getBytes());
		superadmin.files().upload("/www/images/riri.png", "/www/images/riri.png".getBytes());
		superadmin.files().upload("/www/images/fifi.jpg", "/www/images/fifi.jpg".getBytes());
		superadmin.files().upload("/www/css/black.css", "/www/css/black.css".getBytes());
		superadmin.files().upload("/www/css/white.css", "/www/css/white.css".getBytes());

		FileList list = superadmin.files().listAll();
		assertEquals(6, list.files.length);

		// superadmin gets app.html
		SpaceFile file = superadmin.files().get("/www/app.html");
		assertArrayEquals("/www/app.html".getBytes(), file.content);
		assertEquals("text/html", file.contentType);

		// guest gets app.js
		file = guest.files().get("/www/app.js");
		assertArrayEquals("/www/app.js".getBytes(), file.content);
		assertEquals("application/javascript", file.contentType);

		// guest gets black.css
		file = guest.files().get("/www/css/black.css");
		assertArrayEquals("/www/css/black.css".getBytes(), file.content);
		assertEquals("text/css", file.contentType);

		// superadmin lists all images
		assertEquals(2, superadmin.files().list("/www/images").files.length);

		// superadmin deletes all css files
		superadmin.files().delete("/www/css");
		assertEquals(0, superadmin.files().list("/www/css").files.length);

		// superadmin fails to get just deleted css file
		superadmin.get("/1/files/www/css/black.css").go(404);

		// superadmin deletes all files
		String[] deleted = superadmin.files().deleteAll();
		assertEquals(Sets.newHashSet("/www/app.html", "/www/app.js", //
				"/www/images/fifi.jpg", "/www/images/riri.png"), //
				Sets.newHashSet(deleted));
		assertEquals(0, superadmin.files().listAll().files.length);
	}
}
