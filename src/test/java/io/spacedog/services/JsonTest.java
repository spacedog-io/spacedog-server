/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;

public class JsonTest extends Assert {

	@Test
	public void shouldSucceedToGet() {
		JsonNode json = Json.objectBuilder().object("riri").array("fifi").add(12).object()
				.put("loulou", false).build();
		assertEquals(false, Json.get(json, "riri.fifi.1.loulou").asBoolean());
	}

	@Test
	public void shouldSucceedToSet() {
		JsonNode json = Json.objectBuilder().object("riri").array("fifi").add(12).object()
				.put("loulou", false).build();
		Json.set(json, "riri.fifi.1.loulou", BooleanNode.TRUE);
		assertEquals(true, Json.get(json, "riri.fifi.1.loulou").asBoolean());
	}

	@Test
	public void shouldConvertRuntimeExceptionToJsonError() {
		JsonNode json = Json.toJson(new RuntimeException(new NullPointerException()));

		assertEquals("java.lang.RuntimeException", json.get("type").asText());
		assertEquals("java.lang.NullPointerException", json.get("message").asText());
		assertTrue(json.get("trace").size() > 5);
		assertEquals("java.lang.NullPointerException", json.get("cause").get("type").asText());
		assertTrue(json.get("cause").get("message").isNull());
		assertTrue(json.get("cause").get("trace").size() > 5);
	}

}
