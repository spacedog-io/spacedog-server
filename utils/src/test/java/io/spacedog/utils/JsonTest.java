/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;

public class JsonTest extends Assert {

	@Test
	public void shouldSucceedToGet() {
		JsonNode json = Json.objectBuilder().object("riri").array("fifi").add(12).object().put("loulou", false).build();
		assertEquals(12, Json.get(json, "riri.fifi.0").asInt());
		assertEquals(false, Json.get(json, "riri.fifi.1.loulou").asBoolean());
		assertNull(Json.get(json, "riri.name"));
		assertNull(Json.get(json, "riri.fifi.1.loulou.name"));
	}

	@Test
	public void shouldSucceedToSet() {
		JsonNode json = Json.objectBuilder().object("riri").array("fifi").add(12).object().put("loulou", false).build();
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

	@Test
	public void shouldConvertJsonToStringList() {
		// array nodes
		assertEquals(Collections.emptyList(), Json.toList(Json.newArrayNode()));
		assertEquals(Arrays.asList("toto", "200", "true"),
				Json.toList(Json.newArrayNode().add("toto").add(200).add(true)));

		// value nodes
		assertEquals(Arrays.asList("toto"), Json.toList(Json.getMapper().getNodeFactory().textNode("toto")));
		assertEquals(Arrays.asList("200"), Json.toList(Json.getMapper().getNodeFactory().numberNode(200)));
		assertEquals(Arrays.asList("true"), Json.toList(Json.getMapper().getNodeFactory().booleanNode(true)));
	}

	@Test
	public void checkTheseStringsAreJsonObjectsOrNot() {
		// true
		assertTrue(Json.isJsonObject("{}"));
		assertTrue(Json.isJsonObject(" {} "));

		// false
		assertFalse(Json.isJsonObject("{]"));
		assertFalse(Json.isJsonObject("[}"));
	}
}
