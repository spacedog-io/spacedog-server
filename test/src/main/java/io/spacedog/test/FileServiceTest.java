package io.spacedog.test;

import org.junit.Test;

import com.google.common.collect.Sets;

import io.spacedog.client.SpaceDog;
import io.spacedog.model.FileSettings;
import io.spacedog.model.Permission;
import io.spacedog.model.Roles;
import io.spacedog.model.SpaceFile;
import io.spacedog.model.SpaceFile.FileList;

public class FileServiceTest extends SpaceTest {

	@Test
	public void testDefaultWwwSettings() throws Exception {

		// prepare
		prepareTest(false);
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearRootBackend();
		SpaceDog vince = createTempDog(superadmin, "vince");

		// invalid uri throws 404
		assertHttpError(404, () -> superadmin.files().get("toto"));

		// invalid method throws 405
		superadmin.post("/1/files").go(405);

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

		FileList list = superadmin.files().list("/www");
		assertEquals(6, list.files.length);

		// only superadmins are allowed to upload
		assertHttpError(403, () -> guest.files().upload(//
				"/www/xxx.html", "/www/xxx.html".getBytes()));
		assertHttpError(403, () -> vince.files().upload(//
				"/www/xxx.html", "/www/xxx.html".getBytes()));

		list = superadmin.files().listAll();
		assertEquals(6, list.files.length);

		// only superadmins are allowed to list
		assertHttpError(403, () -> guest.files().list("/www"));
		assertHttpError(403, () -> vince.files().list("/www"));

		// superadmin gets app.html
		SpaceFile file = superadmin.files().get("/www/app.html");
		assertArrayEquals("/www/app.html".getBytes(), file.asBytes());
		assertEquals("text/html", file.contentType());

		// guest gets app.js
		file = guest.files().get("/www/app.js");
		assertArrayEquals("/www/app.js".getBytes(), file.asBytes());
		assertEquals("application/javascript", file.contentType());

		// guest gets black.css
		file = guest.files().get("/www/css/black.css");
		assertArrayEquals("/www/css/black.css".getBytes(), file.asBytes());
		assertEquals("text/css", file.contentType());

		// superadmin lists all images
		assertEquals(2, superadmin.files().list("/www/images").files.length);

		// superadmin deletes all css files
		// only superadmins are allowed to delete files
		assertHttpError(403, () -> guest.files().delete("/www/css"));
		assertHttpError(403, () -> vince.files().delete("/www/css"));
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

	@Test
	public void testCustomPrefixSettings() throws Exception {

		// prepare
		prepareTest(false);
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearRootBackend();
		SpaceDog vince = createTempDog(superadmin, "vince");

		// superadmin sets file settings for 'assets' prefix
		FileSettings settings = new FileSettings();
		settings.permissions.put("assets", Roles.user, Permission.readMine, //
				Permission.updateMine, Permission.deleteMine);
		superadmin.settings().save(settings);

		// superadmin checks backend is truly empty
		assertEquals(0, superadmin.files().listAll().files.length);

		// superadmin can upload files to assets prefix
		superadmin.files().upload("/assets/superadmin.txt", "superadmin".getBytes());

		// vince can upload files to assets prefix
		vince.files().upload("/assets/vince/vince.txt", "vince".getBytes());

		// vince is not allowed to upload files to another prefix
		assertHttpError(403, () -> guest.files().upload(//
				"/xxx/vince.txt", "vince".getBytes()));

		// guestis not allowed to upload files to assets prefix
		assertHttpError(403, () -> guest.files().upload(//
				"/assets/guest.txt", "guest".getBytes()));

		// superadmin is allowed to list files for all prefixes
		FileList list = superadmin.files().listAll();
		assertEquals(2, list.files.length);

		// guests don't have any read permission for assets prefix
		assertHttpError(403, () -> guest.files().get("/assets/superadmin.txt"));

		// users can not read superadmin uploaded file
		// since users only have read mine permission
		assertHttpError(403, () -> vince.files().get("/assets/superadmin.txt"));

		// vince can read his own files
		SpaceFile file = vince.files().get("/assets/vince/vince.txt");
		assertArrayEquals("vince".getBytes(), file.asBytes());

		// superadmin can read all files
		file = superadmin.files().get("/assets/superadmin.txt");
		assertArrayEquals("superadmin".getBytes(), file.asBytes());
		file = superadmin.files().get("/assets/vince/vince.txt");
		assertArrayEquals("vince".getBytes(), file.asBytes());

		// only superadmins are allowed to list
		assertHttpError(403, () -> guest.files().list("/assets"));
		assertHttpError(403, () -> vince.files().list("/assets"));

		// vince can not update assets file he doesn't own
		assertHttpError(403, () -> vince.files().upload(//
				"/assets/superadmin.txt", "vince".getBytes()));

		// guest can not delete any assets files
		assertHttpError(403, () -> guest.files().delete("/assets/superadmin.txt"));

		// vince can not delete assets files he doesn't own
		assertHttpError(403, () -> vince.files().delete("/assets/superadmin.txt"));

		// vince can delete his own assets files
		vince.files().delete("/assets/vince/vince.txt");

		// superadmin can delete his own assets files
		superadmin.files().delete("/assets/superadmin.txt");

		// superadmin can delete any assets files
		vince.files().upload("/assets/vince/vince2.txt", "vince".getBytes());
		superadmin.files().delete("/assets/vince/vince2.txt");
	}
}
