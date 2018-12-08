package io.spacedog.test.file;

import java.util.Map;

import org.junit.Test;

import com.google.common.collect.Maps;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.Roles;
import io.spacedog.client.file.FileBucket;
import io.spacedog.client.file.FileBucket.StoreType;
import io.spacedog.client.file.SpaceFile;
import io.spacedog.client.http.SpaceBackend;
import io.spacedog.client.http.SpaceEnv;
import io.spacedog.client.http.SpaceHeaders;
import io.spacedog.client.http.SpaceRequest;
import io.spacedog.test.SpaceTest;

public class WebRestyTest extends SpaceTest {

	@Test
	public void testDefault404() {

		// prepare
		prepareTest();
		superdog().admin().clearBackend();

		// browse without bucket returns default not found html page
		SpaceRequest.get("/2/web").go(404).asVoid()//
				.assertHeaderEquals("text/html", SpaceHeaders.CONTENT_TYPE);
	}

	private static final String WWW = "www";
	private static SpaceDog superadmin;
	private Map<String, SpaceFile> uploads;

	@Test
	public void testWebBrowsing() {

		// prepare
		prepareTest();
		superadmin = clearServer();
		uploads = Maps.newHashMap();

		// superadmin sets www bucket
		FileBucket bucket = new FileBucket(WWW);
		bucket.type = StoreType.s3;
		bucket.permissions.put(Roles.all, Permission.read);
		superadmin.files().setBucket(bucket);

		// superadmin checks bucket www is empty
		assertEquals(0, superadmin.files().listAll(WWW).files.size());

		// upload to unknown bucket is illegal
		superadmin.put("/2/files/XXX.html").go(400).asVoid();

		// superadmin uploads web site to www bucket
		upload("/index.html");
		upload("/toto.html");
		upload("/a/b/index.html");
		upload("/a/b/toto.html");

		// guest browses www pages
		browse("/index.html");
		browse("/toto.html");
		browse("", "/index.html");
		browse("/", "/index.html");
		browse("/a/b/index.html");
		browse("/a/b/toto.html");
		browse("/a/b", "/a/b/index.html");
		browse("/a/b/", "/a/b/index.html");

		// superadmin uploads custom 404 file to www
		// '/404.html' is the default not found path
		upload("/404.html");

		// if user browses invalid URIs
		// server returns default not found path
		// i.e. /404.html page with status code 200
		browse("/index", "/404.html");
		browse("/c", "/404.html");
		browse("/a/", "/404.html");
		browse("/a/b/c/index.html", "/404.html");

		// superadmin sets the not found path of www bucket to /index.html
		bucket.notFoundPage = "/index.html";
		superadmin.files().setBucket(bucket);

		// if user browses invalid URIs
		// server returns the not found path
		// i.e. /index.html page with status code 200
		browse("/index", "/index.html");
		browse("/c", "/index.html");
		browse("/a/", "/index.html");
		browse("/a/b/c/index.html", "/index.html");
	}

	private void upload(String uri) {
		upload(uri, html(uri));
	}

	private void upload(String uri, String html) {
		uploads.put(uri, //
				superadmin.files().upload(WWW, uri, html.getBytes()));
	}

	private String html(String uri) {
		return String.format("<h1>This is %s</h1>", uri);
	}

	private void browse(String uri) {
		browse(uri, uri);
	}

	private void browse(String uri, String expectedUri) {
		String expectedBody = html(expectedUri);
		SpaceFile file = uploads.get(expectedUri);
		SpaceBackend wwwBackend = SpaceEnv.env().wwwBackend();

		SpaceRequest.head("/2/web/" + WWW + uri)//
				.backend(superadmin.backend()).go(200).asVoid()//
				.assertHeaderEquals(file.getContentType(), SpaceHeaders.CONTENT_TYPE)//
				.assertHeaderEquals(file.getLength(), SpaceHeaders.CONTENT_LENGTH)//
				.assertHeaderEquals(file.getHash(), SpaceHeaders.ETAG);

		SpaceRequest.head(uri).backend(wwwBackend).go(200).asVoid()//
				.assertHeaderEquals(file.getContentType(), SpaceHeaders.CONTENT_TYPE)//
				.assertHeaderEquals(file.getLength(), SpaceHeaders.CONTENT_LENGTH)//
				.assertHeaderEquals(file.getHash(), SpaceHeaders.ETAG);

		SpaceRequest.get("/2/web/" + WWW + uri)//
				.backend(superadmin.backend()).go(200)//
				.assertHeaderEquals(file.getContentType(), SpaceHeaders.CONTENT_TYPE)//
				.assertHeaderEquals(file.getHash(), SpaceHeaders.ETAG)//
				.assertBodyEquals(expectedBody);

		SpaceRequest.get(uri).backend(wwwBackend).go(200)//
				.assertHeaderEquals(file.getContentType(), SpaceHeaders.CONTENT_TYPE)//
				.assertHeaderEquals(file.getHash(), SpaceHeaders.ETAG)//
				.assertBodyEquals(expectedBody);
	}
}
