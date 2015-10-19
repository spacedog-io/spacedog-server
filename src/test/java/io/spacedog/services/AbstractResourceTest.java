/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

public class AbstractResourceTest extends AbstractTest {

	@Test
	public void shouldConvertRuntimeExceptionToJsonError() {
		JsonNode json = AbstractResource.toJsonNode(new RuntimeException(new NullPointerException()));

		assertEquals("java.lang.RuntimeException", json.get("type").asText());
		assertEquals("java.lang.NullPointerException", json.get("message").asText());
		assertTrue(json.get("trace").size() > 5);
		assertEquals("java.lang.NullPointerException", json.get("cause").get("type").asText());
		assertTrue(json.get("cause").get("message").isNull());
		assertTrue(json.get("cause").get("trace").size() > 5);
	}
}
