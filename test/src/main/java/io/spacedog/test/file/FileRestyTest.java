package io.spacedog.test.file;

import java.util.Map;

import org.junit.Test;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.Roles;
import io.spacedog.client.file.FileBucket;
import io.spacedog.client.file.FileStoreType;
import io.spacedog.client.file.SpaceFile;
import io.spacedog.client.file.SpaceFile.FileList;
import io.spacedog.client.http.ContentTypes;
import io.spacedog.client.http.SpaceResponse;
import io.spacedog.test.SpaceTest;

public class FileRestyTest extends SpaceTest {

	private static final String ASSETS = "assets";
	private static final String WWW = "www";

	@Test
	public void testDefaultWwwBucketSettings() throws Exception {
		testDefaultWwwBucketSettings(FileStoreType.fs);
		testDefaultWwwBucketSettings(FileStoreType.s3);
	}

	private void testDefaultWwwBucketSettings(FileStoreType storeType) throws Exception {

		// prepare
		prepareTest();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearServer();
		SpaceDog vince = createTempDog(superadmin, "vince");

		// invalid uri throws 404
		assertHttpError(404, () -> superadmin.files().getAsByteArray(WWW, "toto"));

		// invalid POST operation throws 400
		superadmin.post("/2/files").go(400).asVoid();

		// superadmin sets www bucket
		FileBucket bucket = new FileBucket(WWW);
		bucket.type = storeType;
		bucket.permissions.put(Roles.all, Permission.read);
		superadmin.files().setBucket(bucket);

		// superadmin checks bucket www is truly empty
		assertEquals(0, superadmin.files().listAll(WWW).files.size());

		// upload file without bucket is invalid
		superadmin.put("/2/files").go(400).asVoid();

		// admin can upload a web site
		superadmin.files().upload(WWW, "/app.html", "/app.html".getBytes());
		superadmin.files().upload(WWW, "/app.js", "/app.js".getBytes());
		superadmin.files().upload(WWW, "/images/riri.png", "/images/riri.png".getBytes());
		superadmin.files().upload(WWW, "/images/fifi.jpg", "/images/fifi.jpg".getBytes());
		superadmin.files().upload(WWW, "/css/black.css", "/css/black.css".getBytes());
		superadmin.files().upload(WWW, "/css/white.css", "/css/white.css".getBytes());

		FileList list = superadmin.files().listAll(WWW);
		assertEquals(6, list.files.size());

		// only superadmins are allowed to upload
		assertHttpError(401, () -> guest.files().upload(WWW, //
				"/xxx.html", "/xxx.html".getBytes()));
		assertHttpError(403, () -> vince.files().upload(WWW, //
				"/xxx.html", "/xxx.html".getBytes()));

		assertEquals(6, superadmin.files().listAll(WWW).files.size());

		// only superadmins are allowed to list
		assertHttpError(401, () -> guest.files().listAll(WWW));
		assertHttpError(403, () -> vince.files().listAll(WWW));

		// superadmin gets app.html
		SpaceResponse response = superadmin.get("/2/files/www/app.html").go(200);
		assertArrayEquals("/app.html".getBytes(), response.asBytes());
		assertEquals("text/html", response.contentType());

		// guest gets app.js
		response = guest.get("/2/files/www/app.js").go(200);
		assertArrayEquals("/app.js".getBytes(), response.asBytes());
		assertEquals("application/javascript", response.contentType());

		// guest gets black.css
		response = guest.get("/2/files/www/css/black.css").go(200);
		assertArrayEquals("/css/black.css".getBytes(), response.asBytes());
		assertEquals("text/css", response.contentType());

		// superadmin lists all images
		assertEquals(2, superadmin.files().list(WWW, "/images").files.size());

		// superadmin deletes all css files
		// only superadmins are allowed to delete files
		assertHttpError(401, () -> guest.files().delete(WWW, "/css"));
		assertHttpError(403, () -> vince.files().delete(WWW, "/css"));
		assertEquals(2, superadmin.files().delete(WWW, "/css"));
		assertEquals(0, superadmin.files().list(WWW, "/css").files.size());

		// superadmin fails to get just deleted css file
		superadmin.get("/2/files/www/css/black.css").go(404).asVoid();

		// superadmin deletes www bucket
		assertEquals(4, superadmin.files().deleteBucket(WWW));
		assertHttpError(404, () -> superadmin.files().listAll(WWW));
	}

	@Test
	public void testCustomBucketSettings() throws Exception {
		testCustomBucketSettings(FileStoreType.fs);
		testCustomBucketSettings(FileStoreType.s3);
	}

	private void testCustomBucketSettings(FileStoreType storeType) throws Exception {

		// prepare
		prepareTest();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearServer();
		SpaceDog vince = createTempDog(superadmin, "vince");

		// superadmin sets 'assets' file bucket
		FileBucket bucket = new FileBucket(ASSETS);
		bucket.type = storeType;
		bucket.permissions.put(Roles.user, Permission.create, //
				Permission.readMine, Permission.updateMine, Permission.deleteMine);
		superadmin.files().setBucket(bucket);

		// superadmin checks backend is truly empty
		assertEquals(0, superadmin.files().listAll(ASSETS).files.size());

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
		assertHttpError(401, () -> guest.files().getAsByteArray(ASSETS, "/superadmin.txt"));

		// users can not read superadmin uploaded file
		// since users only have read mine permission
		assertHttpError(403, () -> vince.files().getAsByteArray(ASSETS, "/superadmin.txt"));

		// vince can read his own files
		assertArrayEquals("vince".getBytes(), //
				vince.files().getAsByteArray(ASSETS, "/vince/vince.txt"));

		// superadmin can read all files
		assertArrayEquals("superadmin".getBytes(), //
				superadmin.files().getAsByteArray(ASSETS, "/superadmin.txt"));
		assertArrayEquals("vince".getBytes(), //
				superadmin.files().getAsByteArray(ASSETS, "/vince/vince.txt"));

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

	@Test
	public void manageBuckets() throws Exception {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearServer();

		// superadmin creates www bucket
		FileBucket www = new FileBucket(WWW);
		superadmin.files().setBucket(www);

		// superadmin fails to update bucket store type
		www.type = FileStoreType.s3;
		assertHttpError(400, () -> superadmin.files().setBucket(www));

		// superadmin uploads a file into www bucket
		superadmin.files().upload(www.name, "/toto.html", "Hello".getBytes());

		// superadmin deletes www bucket
		superadmin.files().deleteBucket(www.name);

		// superadmin re creates www bucket
		superadmin.files().setBucket(www);

		// superadmin fails to get www bucket file
		// since it was deleted at bucket deletion
		assertHttpError(404, () -> superadmin.files().getAsString(www.name, "/toto.html"));

		// superadmin creates assets bucket
		FileBucket assets = new FileBucket(ASSETS);
		superadmin.files().setBucket(assets);

		// superadmin list all buckets
		Map<String, FileBucket> buckets = superadmin.files().listBuckets();
		assertEquals(2, buckets.size());
		assertEquals(www, buckets.get(www.name));
		assertEquals(assets, buckets.get(assets.name));

		// superadmin deletes www bucket
		superadmin.files().deleteBucket(www.name);
		buckets = superadmin.files().listBuckets();
		assertEquals(1, buckets.size());
		assertEquals(assets, buckets.get(assets.name));

		// superadmin deletes assets bucket
		superadmin.files().deleteBucket(assets.name);
		buckets = superadmin.files().listBuckets();
		assertEquals(0, buckets.size());
	}

	@Test
	public void uploadWithOrWithoutFileNamePassedAsParam() {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearServer();

		// superadmin sets "documents" bucket
		superadmin.files().setBucket(new FileBucket("docs"));

		// superadmin uploads a file without any file name
		SpaceFile doc = superadmin.files().upload("docs", "/titi", "titi".getBytes());
		assertEquals("titi", doc.getName());
		assertEquals("/titi", doc.getPath());
		assertEquals("/titi", doc.getExportEntry());
		assertEquals(ContentTypes.OCTET_STREAM, doc.getContentType());
		assertEquals("titi".getBytes().length, doc.getLength());

		// superadmin downloads this file
		byte[] bytes = superadmin.files().getAsByteArray("docs", "/titi");
		assertArrayEquals("titi".getBytes(), bytes);

		// superadmin uploads a file with file name
		doc = superadmin.files().upload("docs", "/toto", "toto".getBytes(), "toto.txt");
		assertEquals("toto.txt", doc.getName());
		assertEquals("/toto", doc.getPath());
		assertEquals("/toto/toto.txt", doc.getExportEntry());
		assertEquals(ContentTypes.TEXT_PLAIN, doc.getContentType());
		assertEquals("toto".getBytes().length, doc.getLength());

		// superadmin downloads this file
		bytes = superadmin.files().getAsByteArray("docs", "/toto");
		assertArrayEquals("toto".getBytes(), bytes);
	}
}
