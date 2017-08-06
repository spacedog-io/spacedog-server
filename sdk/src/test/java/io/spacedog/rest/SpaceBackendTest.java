package io.spacedog.rest;

import org.junit.Assert;
import org.junit.Test;

public class SpaceBackendTest extends Assert {

	@Test
	public void testIsBackendIdValid() {

		// valid backend ids
		assertTrue(SpaceBackend.isValid("a1234"));
		assertTrue(SpaceBackend.isValid("abcd"));
		assertTrue(SpaceBackend.isValid("1a2b34d"));

		// invalid backend ids
		assertFalse(SpaceBackend.isValid("a"));
		assertFalse(SpaceBackend.isValid("bb"));
		assertFalse(SpaceBackend.isValid("ccc"));
		assertFalse(SpaceBackend.isValid("dd-dd"));
		assertFalse(SpaceBackend.isValid("/eeee"));
		assertFalse(SpaceBackend.isValid("eeee:"));
		assertFalse(SpaceBackend.isValid("abcdE"));
		assertFalse(SpaceBackend.isValid("spacedog"));
		assertFalse(SpaceBackend.isValid("spacedog123"));
		assertFalse(SpaceBackend.isValid("123spacedog"));
		assertFalse(SpaceBackend.isValid("123spacedog123"));
	}

	@Test
	public void testFromDefaults() {
		SpaceBackend backend = SpaceBackend.fromDefaults("production");
		assertEquals("api.spacedog.io", backend.host());
		assertEquals(443, backend.port());
		assertEquals("https", backend.scheme());
		assertEquals("api", backend.backendId());
		assertFalse(backend.webApp());
		assertTrue(backend.ssl());
		assertEquals("https://*.spacedog.io", backend.toString());
		assertEquals("https://api.spacedog.io/1/data", backend.url("/1/data"));
	}

	@Test
	public void testInstanciate() {
		SpaceBackend backend = SpaceBackend.production.instanciate("test");
		assertEquals("test.spacedog.io", backend.host());
		assertEquals(443, backend.port());
		assertEquals("https", backend.scheme());
		assertEquals("test", backend.backendId());
		assertFalse(backend.webApp());
		assertTrue(backend.ssl());
		assertEquals("https://test.spacedog.io", backend.toString());
		assertEquals("https://test.spacedog.io/1/data", backend.url("/1/data"));
	}

	@Test
	public void testFromUrlMonoApiBackend() {
		SpaceBackend backend = SpaceBackend.fromUrl("http://connect.acme.net");
		assertEquals("connect.acme.net", backend.host());
		assertEquals(80, backend.port());
		assertEquals("http", backend.scheme());
		assertEquals("api", backend.backendId());
		assertFalse(backend.webApp());
		assertFalse(backend.ssl());
		assertEquals("http://connect.acme.net", backend.toString());
		assertEquals("http://connect.acme.net/1/data", backend.url("/1/data"));
	}

	@Test
	public void testFromUrlMultiWebAppBackend() {
		SpaceBackend backend = SpaceBackend.fromUrl("http://www.*.acme.net:8080", true);
		assertEquals("www.api.acme.net", backend.host());
		assertEquals(8080, backend.port());
		assertEquals("http", backend.scheme());
		assertEquals("api", backend.backendId());
		assertTrue(backend.webApp());
		assertFalse(backend.ssl());
		assertEquals("http://www.*.acme.net:8080", backend.toString());
		assertEquals("http://www.api.acme.net:8080/1/data", backend.url("/1/data"));
	}

	@Test
	public void apiMultiBackendCanHandleApiRequest() {
		SpaceBackend backend = SpaceBackend.fromDefaults("production")//
				.checkAndInstantiate("test.spacedog.io").get();

		assertEquals("test.spacedog.io", backend.host());
		assertEquals("https://test.spacedog.io", backend.toString());
		assertEquals(443, backend.port());
		assertEquals("https", backend.scheme());
		assertEquals("test", backend.backendId());
		assertFalse(backend.webApp());
		assertTrue(backend.ssl());

		try {
			backend.instanciate("test2");
			fail();

		} catch (IllegalArgumentException ignored) {
		}
	}

	@Test
	public void apiMultiBackendCanNotHandleWebAppRequest() {
		assertFalse(SpaceBackend.fromDefaults("production")//
				.checkAndInstantiate("test.www.spacedog.io").isPresent());
	}

	@Test
	public void webAppMultiBackendCanHandleWebAppRequest() {
		SpaceBackend backend = SpaceBackend.fromUrl("https://*.www.spacedog.io", true)//
				.checkAndInstantiate("test.www.spacedog.io").get();

		assertEquals("test.www.spacedog.io", backend.host());
		assertEquals(443, backend.port());
		assertEquals("https", backend.scheme());
		assertEquals("test", backend.backendId());
		assertTrue(backend.webApp());
		assertTrue(backend.ssl());
		assertFalse(backend.multi());
		assertEquals("https://test.www.spacedog.io", backend.toString());
	}

}
