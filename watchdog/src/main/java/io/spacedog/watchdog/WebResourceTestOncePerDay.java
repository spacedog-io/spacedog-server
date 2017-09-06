package io.spacedog.watchdog;

import org.junit.Test;

import io.spacedog.client.SpaceDog;
import io.spacedog.http.SpaceRequest;
import io.spacedog.http.SpaceTest;
import io.spacedog.model.WebSettings;
import io.spacedog.utils.SpaceHeaders;

public class WebResourceTestOncePerDay extends SpaceTest {

	private static SpaceDog test;

	@Test
	public void test() {

		// prepare
		prepareTest(false);
		test = resetTestBackend();

		SpaceRequest.get("/1/file").auth(test).go(200)//
				.assertSizeEquals(0, "results");

		// upload without prefix is illegal
		SpaceRequest.put("/1/file/XXX.html").auth(test)//
				.bodyString("<h1>Hello</h1>").go(400);

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
		// '/404.html' is the default not found path
		upload("www", "/404.html");

		// if user browses invalid URIs
		// the server returns the default not found path
		// i.e. the /404.html page with status code 200
		browse("www", "/index", html("/404.html"));
		browse("www", "/c", html("/404.html"));
		browse("www", "/a/", html("/404.html"));
		browse("www", "/a/b/c/index.html", html("/404.html"));

		// changes the not found path to /index.html
		WebSettings settings = new WebSettings();
		settings.notFoundPage = "/index.html";
		test.settings().save(settings);

		// if user browses invalid URIs
		// the server returns the not found path
		// i.e. the /index.html page with status code 200
		browse("www", "/index", html("/index.html"));
		browse("www", "/c", html("/index.html"));
		browse("www", "/a/", html("/index.html"));
		browse("www", "/a/b/c/index.html", html("/index.html"));

		// browse without prefix returns default not found html page
		SpaceRequest.get("/1/web").backend(test).go(404)//
				.assertHeaderEquals("text/html", SpaceHeaders.CONTENT_TYPE);
	}

	private void upload(String prefix, String uri) {
		upload(prefix, uri, html(uri));
	}

	private void upload(String prefix, String uri, String html) {
		SpaceRequest.put("/1/file/" + prefix + uri).auth(test).bodyString(html).go(200);
	}

	private String html(String uri) {
		return String.format("<h1>This is %s</h1>", uri);
	}

	private void browse(String prefix, String uri) {
		browse(prefix, uri, html(uri), "text/html");
	}

	private void browse(String prefix, String uri, String expectedBody) {
		browse(prefix, uri, expectedBody, "text/html");
	}

	private void browse(String prefix, String uri, String expectedBody, String expectedContentType) {
		SpaceRequest.head("/1/web/" + prefix + uri).backend(test).go(200)//
				.assertHeaderEquals(expectedContentType, SpaceHeaders.CONTENT_TYPE);

		SpaceRequest.head(uri).www(test).go(200)//
				.assertHeaderEquals(expectedContentType, SpaceHeaders.CONTENT_TYPE);

		SpaceRequest.get("/1/web/" + prefix + uri).backend(test).go(200)//
				.assertHeaderEquals(expectedContentType, SpaceHeaders.CONTENT_TYPE)//
				.assertBodyEquals(expectedBody);

		SpaceRequest.get(uri).www(test).go(200)//
				.assertHeaderEquals(expectedContentType, SpaceHeaders.CONTENT_TYPE)//
				.assertBodyEquals(expectedBody);
	}
}
