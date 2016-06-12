package io.spacedog.watchdog;

import org.junit.Test;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceClient.Backend;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.SpaceHeaders;
import io.spacedog.watchdog.SpaceSuite.TestOncePerDay;

@TestOncePerDay
public class WebResourceTestOncePerDay {

	private static final String HTML_404 = "<h1>404</h1>";

	private static Backend test;

	@Test
	public void test() throws Exception {

		// prepare
		SpaceClient.prepareTest(false);
		test = SpaceClient.resetTestBackend();

		SpaceRequest.get("/1/file").adminAuth(test).go(200)//
				.assertSizeEquals(0, "results");

		// upload without prefix is illegal
		SpaceRequest.put("/1/file/XXX.html").adminAuth(test)//
				.body("<h1>Hello</h1>").go(400);

		// admin uploads web site at prefix 'www'
		upload("www", "/index.html");
		upload("www", "/toto.html");
		upload("www", "/a/b/index.html");
		upload("www", "/a/b/toto.html");

		// anonymous user can browse 'www' pages
		browse("www", "/index.html");
		browse("www", "/toto.html");
		browse("www", "", html("/index.html"));
		browse("www", "/", html("/index.html"));
		browse("www", "/a/b/index.html");
		browse("www", "/a/b/toto.html");
		browse("www", "/a/b", html("/a/b/index.html"));
		browse("www", "/a/b/", html("/a/b/index.html"));

		// admin uploads custom 404.html file to 'www'
		upload("www", "/404.html", HTML_404);

		// if user browses invalid 'www' URIs
		// the server returns 'www' custom 404 file
		notFound("www", "/index");
		notFound("www", "/c");
		notFound("www", "/a/");
		notFound("www", "/a/b/c/index.html");

		// browse without prefix returns default not found html page
		SpaceRequest.get("/1/web").backend(test).go(404)//
				.assertHeaderEquals("text/html", SpaceHeaders.CONTENT_TYPE);
	}

	private void upload(String prefix, String uri) throws Exception {
		upload(prefix, uri, html(uri));
	}

	private void upload(String prefix, String uri, String html) throws Exception {
		SpaceRequest.put("/1/file/" + prefix + uri).adminAuth(test).body(html).go(200);
	}

	private String html(String uri) {
		return String.format("<h1>This is %s</h1>", uri);
	}

	private void browse(String prefix, String uri) throws Exception {
		browse(prefix, uri, html(uri), "text/html");
	}

	private void browse(String prefix, String uri, String expectedBody) throws Exception {
		browse(prefix, uri, expectedBody, "text/html");
	}

	private void browse(String prefix, String uri, String expectedBody, String expectedContentType) throws Exception {
		SpaceRequest.head("/1/web/" + prefix + uri).backend(test).go(200)//
				.assertHeaderEquals(expectedContentType, SpaceHeaders.CONTENT_TYPE);

		SpaceRequest.get("/1/web/" + prefix + uri).backend(test).go(200)//
				.assertHeaderEquals(expectedContentType, SpaceHeaders.CONTENT_TYPE)//
				.assertBodyEquals(expectedBody);
	}

	private void notFound(String prefix, String uri) throws Exception {
		SpaceRequest.get("/1/web/" + prefix + uri).backend(test).go(404)//
				.assertHeaderEquals("text/html", SpaceHeaders.CONTENT_TYPE)//
				.assertBodyEquals(HTML_404);
	}
}
