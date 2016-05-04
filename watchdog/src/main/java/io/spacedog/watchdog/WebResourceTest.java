package io.spacedog.watchdog;

import org.junit.Test;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceDogHelper.Backend;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.SpaceHeaders;
import io.spacedog.watchdog.SpaceSuite.TestOncePerDay;

@TestOncePerDay
public class WebResourceTest {

	private static final String HTML_404 = "<h1>404</h1>";

	private static Backend testBackend;

	@Test
	public void test() throws Exception {

		// prepare
		SpaceDogHelper.prepareTest(false);
		testBackend = SpaceDogHelper.resetTestBackend();

		SpaceRequest.get("/1/file").adminAuth(testBackend).go(200)//
				.assertSizeEquals(0, "results");

		// upload web site
		upload("/index.html");
		upload("/toto.html");
		upload("/404.html", HTML_404);
		upload("/a/b/index.html");
		upload("/a/b/toto.html");

		// browse web pages
		browse("/index.html");
		browse("/toto.html");
		browse("", html("/index.html"));
		browse("/", html("/index.html"));
		browse("/a/b/index.html");
		browse("/a/b/toto.html");
		browse("/a/b", html("/a/b/index.html"));
		browse("/a/b/", html("/a/b/index.html"));

		// get 404 for bad uris
		notFound("/index");
		notFound("/c");
		notFound("/a/");
		notFound("/a/b/c/index.html");
	}

	private void upload(String uri) throws Exception {
		upload(uri, html(uri));
	}

	private void upload(String uri, String html) throws Exception {
		SpaceRequest.put("/1/file" + uri).adminAuth(testBackend).body(html).go(200);
	}

	private String html(String uri) {
		return String.format("<h1>This is %s</h1>", uri);
	}

	private void browse(String uri) throws Exception {
		browse(uri, html(uri), "text/html");
	}

	private void browse(String uri, String expectedBody) throws Exception {
		browse(uri, expectedBody, "text/html");
	}

	private void browse(String uri, String expectedBody, String expectedContentType) throws Exception {
		SpaceRequest.get("/1/web" + uri).backend(testBackend).go(200)//
				.assertHeaderEquals(expectedContentType, SpaceHeaders.CONTENT_TYPE)//
				.assertBodyEquals(expectedBody);
	}

	private void notFound(String uri) throws Exception {
		SpaceRequest.get("/1/web" + uri).backend(testBackend).go(404)//
				.assertHeaderEquals("text/html", SpaceHeaders.CONTENT_TYPE)//
				.assertBodyEquals(HTML_404);
	}
}
