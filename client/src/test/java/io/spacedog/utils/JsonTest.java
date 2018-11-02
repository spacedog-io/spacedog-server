/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.spacedog.client.data.DataWrap;
import io.spacedog.client.http.SpaceException;

public class JsonTest extends Assert {

	@Test
	public void shouldGet() {
		JsonNode json = Json.builder().object()//
				.add("tutu", null)//
				.object("riri")//
				.array("fifi")//
				.add(12)//
				.object().add("loulou", false)//
				.build();

		assertEquals(12, Json.get(json, "riri.fifi.0").asInt());
		assertFalse(Json.get(json, "riri.fifi.1.loulou").asBoolean());
		assertNull(Json.get(json, "tutu"));
		assertNull(Json.get(json, "riri.name"));
		assertNull(Json.get(json, "riri.fifi.1.loulou.name"));
	}

	@Test
	public void shouldSet() {
		JsonNode json = Json.builder().object().object("riri").array("fifi")//
				.add(12).object().add("loulou", false).build();
		Json.set(json, "riri.fifi.1.loulou", BooleanNode.TRUE);
		assertTrue(Json.get(json, "riri.fifi.1.loulou").asBoolean());
	}

	@Test
	public void shouldWith() {
		JsonNode expected = Json.builder().object().object("riri").array("fifi")//
				.object().add("loulou", true).build();

		ObjectNode object = Json.object();
		Json.with(object, "riri.fifi.0.loulou", BooleanNode.TRUE);
		assertEquals(expected, object);

		Json.checkArray(expected.get("riri").get("fifi")).add(12);
		Json.with(object, "riri.fifi.1", 12);
		assertEquals(expected, object);

		Json.checkObject(expected.get("riri")).set("toto", NullNode.getInstance());
		Json.with(object, "riri.toto", null);
		assertEquals(expected, object);

		expected = Json.builder().array().add(true).object().add("name", "bill").build();
		ArrayNode array = Json.array();
		Json.with(array, "0", true);
		Json.with(array, "1.name", "bill");
		assertEquals(expected, array);
	}

	@Test
	public void shouldWithWhenPartOfFieldPathExistsAnsIsNull() {
		ObjectNode object = Json.object();
		Json.with(object, "meta", null);
		assertEquals(Json.object("meta", null), object);
		Json.with(object, "meta.createdBy", "fred");
		assertEquals(Json.object("meta", Json.object("createdBy", "fred")), object);
	}

	@Test
	public void shouldRemove() {
		JsonNode json = Json.builder().object().object("riri").array("fifi")//
				.add(12).object().add("loulou", false).build();
		Json.remove(json, "riri.fifi.0");
		assertFalse(Json.get(json, "riri.fifi.0.loulou").asBoolean());
		Json.remove(json, "riri.fifi.0.loulou");
		assertEquals(0, Json.get(json, "riri.fifi.0").size());
	}

	@Test
	public void shouldWithArray() {
		ObjectNode node = Json.object("ints", null, "bools", //
				Json.array(), "object", Json.object());

		Json.withArray(node, "floats").add(1.7f);
		Json.withArray(node, "ints").add(7);
		Json.withArray(node, "bools").add(true);

		Json.assertNode(node)//
				.assertEquals(1.7f, "floats.0", 0.01f)//
				.assertEquals(Json.array(7), "ints")//
				.assertEquals(Json.array(true), "bools");

		try {
			Json.withArray(node, "object");
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
		} catch (SpaceException e) {
			assertEquals(400, e.httpStatus());
		}
	}

	@Test
	public void testToObject() {
		assertEquals(1, Json.toObject(Json.toJsonNode(1)));
		assertEquals("test", Json.toObject(Json.toJsonNode("test")));
		assertEquals(true, Json.toObject(Json.toJsonNode(true)));
	}

	private static class Speed {
		@SuppressWarnings("unused")
		public int s;

		public Speed(int s) {
			this.s = s;
		}
	}

	@Test
	public void testToNode() {
		assertEquals(TextNode.valueOf("toto"), Json.toJsonNode("toto"));
		assertEquals(IntNode.valueOf(1), Json.toJsonNode(1));

		assertEquals(Json.object("s", 2), Json.toJsonNode(new Speed(2)));
		assertEquals(Json.object(), Json.toJsonNode(Json.object()));

		assertEquals(Json.array(), Json.toJsonNode(Json.array()));
		assertEquals(Json.array(1, 2), Json.toJsonNode(Arrays.asList(1, 2)));
		assertEquals(Json.array(1, 2), Json.toJsonNode(Arrays.asList(1, 2)));
	}

	@Test
	public void shouldPartiallyUpdatePojo() {
		DataWrap<ObjectNode> object = DataWrap.wrap(ObjectNode.class);
		String json = Json.object("version", 1).toString();
		Json.updatePojo(json, object);
		assertEquals(1, object.version());
	}

	@Test
	public void shouldAddToSet() {
		// prepare
		ObjectNode object = Json.object();
		ArrayNode array = Json.withArray(object, "values");
		// test 1
		Json.addToSet(array, "toto");
		assertEquals(Json.array("toto"), object.get("values"));
		// test 2
		Json.addToSet(array, "titi", Json.object());
		assertEquals(Json.array("toto", Json.object(), "titi"), //
				object.get("values"));
		// test 3
		Json.addToSet(array, "titi", Json.object());
		assertEquals(Json.array("toto", Json.object(), "titi"), //
				object.get("values"));
	}

	@Test
	public void shouldRemoveFromSet() {
		// prepare
		ObjectNode object = Json.object();
		ArrayNode array = Json.withArray(object, "values");
		Json.addToSet(array, "toto", "titi", Json.object());
		// test 1
		Json.removeFromSet(array, "tata");
		assertEquals(Json.array("toto", Json.object(), "titi"), //
				object.get("values"));
		// test 2
		Json.removeFromSet(array, "titi");
		assertEquals(Json.array("toto", Json.object()), object.get("values"));
		// test 3
		Json.removeFromSet(array, "tata", Json.object(), "toto");
		assertEquals(Json.array(), object.get("values"));
		// test 4
		Json.removeFromSet(array, Json.object(), "titi");
		assertEquals(Json.array(), object.get("values"));
	}
}
