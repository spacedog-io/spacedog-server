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
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class JsonTest extends Assert {

	@Test
	public void shouldGet() {
		JsonNode json = Json.objectBuilder().object("riri").array("fifi").add(12).object().put("loulou", false).build();
		assertEquals(12, Json.get(json, "riri.fifi.0").asInt());
		assertFalse(Json.get(json, "riri.fifi.1.loulou").asBoolean());
		assertNull(Json.get(json, "riri.name"));
		assertNull(Json.get(json, "riri.fifi.1.loulou.name"));
	}

	@Test
	public void shouldSet() {
		JsonNode json = Json.objectBuilder().object("riri").array("fifi").add(12).object().put("loulou", false).build();
		Json.set(json, "riri.fifi.1.loulou", BooleanNode.TRUE);
		assertTrue(Json.get(json, "riri.fifi.1.loulou").asBoolean());
	}

	@Test
	public void shouldRemove() {
		JsonNode json = Json.objectBuilder().object("riri").array("fifi").add(12).object().put("loulou", false).build();
		Json.remove(json, "riri.fifi.0");
		assertFalse(Json.get(json, "riri.fifi.0.loulou").asBoolean());
		Json.remove(json, "riri.fifi.0.loulou");
		assertEquals(0, Json.get(json, "riri.fifi.0").size());
	}

	@Test
	public void shouldConvertJsonToStringList() {
		// array nodes
		assertEquals(Collections.emptyList(), Json.toStrings(Json.array()));
		assertEquals(Arrays.asList("toto", "200", "true"), Json.toStrings(Json.array().add("toto").add(200).add(true)));

		// value nodes
		assertEquals(Arrays.asList("toto"), Json.toStrings(TextNode.valueOf("toto")));
		assertEquals(Arrays.asList("200"), Json.toStrings(IntNode.valueOf(200)));
		assertEquals(Arrays.asList("true"), Json.toStrings(BooleanNode.valueOf(true)));
	}

	@Test
	public void checkTheseStringsAreJsonObjectsOrNot() {
		// true
		assertTrue(Json.isObject("{}"));
		assertTrue(Json.isObject(" {} "));

		// false
		assertFalse(Json.isObject("{]"));
		assertFalse(Json.isObject("[}"));
	}

	@Test
	public void shouldCheckStringFields() {
		JsonNode node = Json.object("name", "david", "age", 3);
		assertEquals("david", Json.checkString(node, "name").get());
		assertFalse(Json.checkString(node, "city").isPresent());
		try {
			Json.checkString(node, "age");
			fail();
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testToArrayNode() {
		assertEquals(Json.arrayBuilder().add(1).add("vince").array().add(1).add(2).build(), //
				Json.toArrayNode(new Object[] { 1, "vince", new int[] { 1, 2 } }));
	}

	@Test
	public void testToArray() {
		assertTrue(Arrays.deepEquals(new Object[] { 1, "vince", new Object[] { 1, 2 } }, //
				Json.toArray(Json.arrayBuilder().add(1).add("vince").array().add(1).add(2).build())));
	}
}
