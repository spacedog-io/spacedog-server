package io.spacedog.test.file;

import org.junit.Test;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.Roles;
import io.spacedog.client.file.InternalFileSettings.FileBucketSettings;
import io.spacedog.client.http.SpaceBackend;
import io.spacedog.client.http.SpaceEnv;
import io.spacedog.client.http.SpaceHeaders;
import io.spacedog.client.http.SpaceRequest;
import io.spacedog.test.SpaceTest;

public class WebRestyTest extends SpaceTest {

	private static final String WWW = "www";
	private static SpaceDog superadmin;

	@Test
	public void test() {

		// prepare
		prepareTest();
		superadmin = clearServer(true);

		// superadmin sets www bucket
		FileBucketSettings settings = new FileBucketSettings(WWW);
		settings.permissions.put(Roles.all, Permission.read);
		superadmin.files().setBucket(settings);

		// superadmin checks bucket www is empty
		assertEquals(0, superadmin.files().listAll(WWW).files.size());

		// upload to unknown bucket is illegal
		superadmin.put("/1/files/XXX.html").go(400);

		// superadmin uploads web site to www bucket
		upload("/index.html");
		upload("/toto.html");
		upload("/a/b/index.html");
		upload("/a/b/toto.html");

		// guest browses www pages
		browse("/index.html");
		browse("/toto.html");
		browse("", html("/index.html"));
		browse("/", html("/index.html"));
		browse("/a/b/index.html");
		browse("/a/b/toto.html");
		browse("/a/b", html("/a/b/index.html"));
		browse("/a/b/", html("/a/b/index.html"));

		// superadmin uploads custom 404 file to www
		// '/404.html' is the default not found path
		upload("/404.html");

		// if user browses invalid URIs
		// server returns default not found path
		// i.e. /404.html page with status code 200
		browse("/index", html("/404.html"));
		browse("/c", html("/404.html"));
		browse("/a/", html("/404.html"));
		browse("/a/b/c/index.html", html("/404.html"));

		// superadmin sets the not found path of www bucket to /index.html
		settings.notFoundPage = "/index.html";
		superadmin.files().setBucket(settings);

		// if user browses invalid URIs
		// server returns the not found path
		// i.e. /index.html page with status code 200
		browse("/index", html("/index.html"));
		browse("/c", html("/index.html"));
		browse("/a/", html("/index.html"));
		browse("/a/b/c/index.html", html("/index.html"));

		// browse without bucket returns default not found html page
		superadmin.get("/1/web").go(404)//
				.assertHeaderEquals("text/html", SpaceHeaders.CONTENT_TYPE);
	}

	private void upload(String uri) {
		upload(uri, html(uri));
	}

	private void upload(String uri, String html) {
		superadmin.files().upload(WWW, uri, html.getBytes());
	}

	private String html(String uri) {
		return String.format("<h1>This is %s</h1>", uri);
	}

	private void browse(String uri) {
		browse(uri, html(uri), "text/html");
	}

	private void browse(String uri, String expectedBody) {
		browse(uri, expectedBody, "text/html");
	}

	private void browse(String uri, String expectedBody, String expectedContentType) {
		SpaceBackend wwwBackend = SpaceEnv.env().wwwBackend();

		SpaceRequest.head("/1/web/" + WWW + uri)//
				.backend(superadmin.backend()).go(200)//
				.assertHeaderEquals(expectedContentType, SpaceHeaders.CONTENT_TYPE);

		SpaceRequest.head(uri).backend(wwwBackend).go(200)//
				.assertHeaderEquals(expectedContentType, SpaceHeaders.CONTENT_TYPE);

		SpaceRequest.get("/1/web/" + WWW + uri)//
				.backend(superadmin.backend()).go(200)//
				.assertHeaderEquals(expectedContentType, SpaceHeaders.CONTENT_TYPE)//
				.assertBodyEquals(expectedBody);

		SpaceRequest.get(uri).backend(wwwBackend).go(200)//
				.assertHeaderEquals(expectedContentType, SpaceHeaders.CONTENT_TYPE)//
				.assertBodyEquals(expectedBody);
	}
}
