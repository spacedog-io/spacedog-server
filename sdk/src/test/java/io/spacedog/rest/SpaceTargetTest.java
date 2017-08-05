package io.spacedog.rest;

import org.junit.Assert;
import org.junit.Test;

public class SpaceTargetTest extends Assert {

	@Test
	public void testFromDefaults() {
		SpaceTarget backend = SpaceTarget.fromDefaults("production");
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
	public void testFromBackendId() {
		SpaceTarget backend = SpaceTarget.production.fromBackendId("test");
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
		SpaceTarget backend = SpaceTarget.fromUrl("http://connect.acme.net");
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
		SpaceTarget backend = SpaceTarget.fromUrl("http://www.*.acme.net:8080", true);
		assertEquals("www.api.acme.net", backend.host());
		assertEquals("www.connect.acme.net", backend.host("connect"));
		assertEquals(8080, backend.port());
		assertEquals("http", backend.scheme());
		assertEquals("api", backend.backendId());
		assertTrue(backend.webApp());
		assertFalse(backend.ssl());
		assertEquals("http://www.*.acme.net:8080", backend.toString());
		assertEquals("http://www.api.acme.net:8080/1/data", backend.url("/1/data"));
	}

}
