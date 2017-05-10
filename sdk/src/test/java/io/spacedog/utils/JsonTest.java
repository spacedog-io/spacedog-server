/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonTest extends Assert {

	@Test
	public void shouldGet() {
		JsonNode json = Json7.objectBuilder()//
				.put("tutu", null)//
				.object("riri")//
				.array("fifi")//
				.add(12)//
				.object().put("loulou", false)//
				.build();

		assertEquals(12, Json7.get(json, "riri.fifi.0").asInt());
		assertFalse(Json7.get(json, "riri.fifi.1.loulou").asBoolean());
		assertNull(Json7.get(json, "tutu"));
		assertNull(Json7.get(json, "riri.name"));
		assertNull(Json7.get(json, "riri.fifi.1.loulou.name"));
	}

	@Test
	public void shouldSet() {
		JsonNode json = Json7.objectBuilder().object("riri").array("fifi")//
				.add(12).object().put("loulou", false).build();
		Json7.set(json, "riri.fifi.1.loulou", BooleanNode.TRUE);
		assertTrue(Json7.get(json, "riri.fifi.1.loulou").asBoolean());
	}

	@Test
	public void shouldRemove() {
		JsonNode json = Json7.objectBuilder().object("riri").array("fifi")//
				.add(12).object().put("loulou", false).build();
		Json7.remove(json, "riri.fifi.0");
		assertFalse(Json7.get(json, "riri.fifi.0.loulou").asBoolean());
		Json7.remove(json, "riri.fifi.0.loulou");
		assertEquals(0, Json7.get(json, "riri.fifi.0").size());
	}

	@Test
	public void shouldWithArray() {
		ObjectNode node = Json7.object("ints", null, "bools", //
				Json7.array(), "object", Json7.object());

		Json7.withArray(node, "floats").add(1.7f);
		Json7.withArray(node, "ints").add(7);
		Json7.withArray(node, "bools").add(true);

		Json7.assertNode(node)//
				.assertEquals(1.7f, "floats.0", 0.01f)//
				.assertEquals(Json7.array(7), "ints")//
				.assertEquals(Json7.array(true), "bools");

		try {
			Json7.withArray(node, "object");
			fail();
		} catch (Exception ignore) {
		}
	}

	// @Test
	// public void shouldConvertJsonToStringList() {
	// // array nodes
	// assertEquals(Collections.emptyList(), Json.toStrings(Json.array()));
	// assertEquals(Arrays.asList("toto", "200", "true"),
	// Json.toStrings(Json.array().add("toto").add(200).add(true)));
	//
	// // value nodes
	// assertEquals(Arrays.asList("toto"),
	// Json.toStrings(TextNode.valueOf("toto")));
	// assertEquals(Arrays.asList("200"), Json.toStrings(IntNode.valueOf(200)));
	// assertEquals(Arrays.asList("true"),
	// Json.toStrings(BooleanNode.valueOf(true)));
	// }

	@Test
	public void checkTheseStringsAreJsonObjectsOrNot() {
		// true
		assertTrue(Json7.isObject("{}"));
		assertTrue(Json7.isObject(" {} "));

		// false
		assertFalse(Json7.isObject("{]"));
		assertFalse(Json7.isObject("[}"));
	}

	@Test
	public void shouldCheckStringFields() {
		JsonNode node = Json7.object("name", "david", "age", 3);
		assertEquals("david", Json7.checkString(node, "name").get());
		assertFalse(Json7.checkString(node, "city").isPresent());
		try {
			Json7.checkString(node, "age");
			fail();
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testToArrayNode() {
		assertEquals(Json7.arrayBuilder().add(1).add("vince").array().add(1).add(2).build(), //
				Json7.toArrayNode(new Object[] { 1, "vince", new int[] { 1, 2 } }));
	}

	@Test
	public void testToArray() {
		assertTrue(Arrays.deepEquals(new Object[] { 1, "vince", new Object[] { 1, 2 } }, //
				Json7.toArray(Json7.arrayBuilder().add(1).add("vince").array().add(1).add(2).build())));
	}
}
