package io.spacedog.test.file;

import org.junit.Test;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.Roles;
import io.spacedog.client.file.InternalFileSettings.FileBucketSettings;
import io.spacedog.client.file.SpaceFile;
import io.spacedog.client.file.SpaceFile.FileList;
import io.spacedog.test.SpaceTest;

public class FileServiceTest extends SpaceTest {

	private static final String ASSETS = "assets";
	private static final String WWW = "www";

	@Test
	public void testDefaultWwwBucketSettings() throws Exception {

		// prepare
		prepareTest();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearRootBackend(true);
		SpaceDog vince = createTempDog(superadmin, "vince");

		// invalid uri throws 404
		assertHttpError(404, () -> superadmin.files().get(WWW, "toto"));

		// invalid POST operation throws 400
		superadmin.post("/1/files").go(400);

		// superadmin sets www bucket
		FileBucketSettings settings = new FileBucketSettings(WWW);
		settings.permissions.put(Roles.all, Permission.read);
		superadmin.files().setBucket(settings);

		// superadmin checks bucket www is truly empty
		assertEquals(0, superadmin.files().listAll(WWW).files.length);

		// upload file without bucket is invalid
		superadmin.put("/1/files").go(400);

		// admin can upload a web site
		superadmin.files().upload(WWW, "/app.html", "/app.html".getBytes());
		superadmin.files().upload(WWW, "/app.js", "/app.js".getBytes());
		superadmin.files().upload(WWW, "/images/riri.png", "/images/riri.png".getBytes());
		superadmin.files().upload(WWW, "/images/fifi.jpg", "/images/fifi.jpg".getBytes());
		superadmin.files().upload(WWW, "/css/black.css", "/css/black.css".getBytes());
		superadmin.files().upload(WWW, "/css/white.css", "/css/white.css".getBytes());

		FileList list = superadmin.files().listAll(WWW);
		assertEquals(6, list.files.length);

		// only superadmins are allowed to upload
		assertHttpError(401, () -> guest.files().upload(WWW, //
				"/xxx.html", "/xxx.html".getBytes()));
		assertHttpError(403, () -> vince.files().upload(WWW, //
				"/xxx.html", "/xxx.html".getBytes()));

		assertEquals(6, superadmin.files().listAll(WWW).files.length);

		// only superadmins are allowed to list
		assertHttpError(401, () -> guest.files().listAll(WWW));
		assertHttpError(403, () -> vince.files().listAll(WWW));

		// superadmin gets app.html
		SpaceFile file = superadmin.files().get(WWW, "/app.html");
		assertArrayEquals("/app.html".getBytes(), file.asBytes());
		assertEquals("text/html", file.contentType());

		// guest gets app.js
		file = guest.files().get(WWW, "/app.js");
		assertArrayEquals("/app.js".getBytes(), file.asBytes());
		assertEquals("application/javascript", file.contentType());

		// guest gets black.css
		file = guest.files().get(WWW, "/css/black.css");
		assertArrayEquals("/css/black.css".getBytes(), file.asBytes());
		assertEquals("text/css", file.contentType());

		// superadmin lists all images
		assertEquals(2, superadmin.files().list(WWW, "/images").files.length);

		// superadmin deletes all css files
		// only superadmins are allowed to delete files
		assertHttpError(401, () -> guest.files().delete(WWW, "/css"));
		assertHttpError(403, () -> vince.files().delete(WWW, "/css"));
		assertEquals(2, superadmin.files().delete(WWW, "/css"));
		assertEquals(0, superadmin.files().list(WWW, "/css").files.length);

		// superadmin fails to get just deleted css file
		superadmin.get("/1/files/www/css/black.css").go(404);

		// superadmin deletes all files
		assertEquals(4, superadmin.files().deleteAll(WWW));
		// assertEquals(Sets.newHashSet("/www/app.html", "/www/app.js", //
		// "/www/images/fifi.jpg", "/www/images/riri.png"), //
		// Sets.newHashSet(deleted));
		assertEquals(0, superadmin.files().listAll(WWW).files.length);
	}

	@Test
	public void testCustomBucketSettings() throws Exception {

		// prepare
		prepareTest();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearRootBackend(true);
		SpaceDog vince = createTempDog(superadmin, "vince");

		// superadmin sets 'assets' file bucket
		FileBucketSettings bucket = new FileBucketSettings(ASSETS);
		bucket.permissions.put(Roles.user, Permission.create, //
				Permission.readMine, Permission.updateMine, Permission.deleteMine);
		superadmin.files().setBucket(bucket);

		// superadmin checks backend is truly empty
		assertEquals(0, superadmin.files().listAll(ASSETS).files.length);

		// superadmin can upload files to assets prefix
		superadmin.files().upload(ASSETS, "/superadmin.txt", "superadmin".getBytes());

		// vince can upload files to assets prefix
		vince.files().upload(ASSETS, "/vince/vince.txt", "vince".getBytes());

		// vince fails to upload to xxx bucket
		// since it doesn't exists
		assertHttpError(404, () -> vince.files().upload("xxx", //
				"/vince.txt", "vince".getBytes()));

		// guest is not allowed to upload files to assets bucket
		assertHttpError(401, () -> guest.files().upload(ASSETS, //
				"/guest.txt", "guest".getBytes()));

		// superadmin is allowed to list files for all prefixes
		// FileList list = superadmin.files().listAll();
		// assertEquals(2, list.files.length);

		// guests don't have any read permission for assets prefix
		assertHttpError(401, () -> guest.files().get(ASSETS, "/superadmin.txt"));

		// users can not read superadmin uploaded file
		// since users only have read mine permission
		assertHttpError(403, () -> vince.files().get(ASSETS, "/superadmin.txt"));

		// vince can read his own files
		SpaceFile file = vince.files().get(ASSETS, "/vince/vince.txt");
		assertArrayEquals("vince".getBytes(), file.asBytes());

		// superadmin can read all files
		file = superadmin.files().get(ASSETS, "/superadmin.txt");
		assertArrayEquals("superadmin".getBytes(), file.asBytes());
		file = superadmin.files().get(ASSETS, "/vince/vince.txt");
		assertArrayEquals("vince".getBytes(), file.asBytes());

		// only superadmins are allowed to list
		assertHttpError(401, () -> guest.files().listAll(ASSETS));
		assertHttpError(403, () -> vince.files().listAll(ASSETS));

		// vince can not update assets file he doesn't own
		assertHttpError(403, () -> vince.files().upload(ASSETS, //
				"/superadmin.txt", "vince".getBytes()));

		// guest can not delete any assets files
		assertHttpError(401, () -> guest.files().delete(ASSETS, "/superadmin.txt"));

		// vince can not delete assets files he doesn't own
		assertHttpError(403, () -> vince.files().delete(ASSETS, "/superadmin.txt"));

		// vince can delete his own assets files
		vince.files().delete(ASSETS, "/vince/vince.txt");

		// superadmin can delete his own assets files
		superadmin.files().delete(ASSETS, "/superadmin.txt");

		// superadmin can delete any assets files
		vince.files().upload(ASSETS, "/vince/vince2.txt", "vince".getBytes());
		superadmin.files().delete(ASSETS, "/vince/vince2.txt");
	}
}
