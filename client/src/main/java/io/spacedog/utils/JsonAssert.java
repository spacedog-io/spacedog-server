package io.spacedog.utils;

import org.joda.time.DateTime;
import org.junit.Assert;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.google.common.collect.Iterators;

public class JsonAssert<K extends JsonNode> {

	private K node;

	JsonAssert(K node) {
		this.node = node;
	}

	public JsonAssert<K> assertEquals(String expected, String jsonPath) {
		Assert.assertEquals(expected, Json.checkString(//
				Json.checkNotNull(Json.get(node, jsonPath))));
		return this;
	}

	public JsonAssert<K> assertEquals(int expected, String jsonPath) {
		JsonNode field = Json.get(node, jsonPath);
		Assert.assertEquals(expected, //
				field.isArray() || field.isObject() ? field.size() : field.intValue());
		return this;
	}

	public JsonAssert<K> assertEquals(long expected, String jsonPath) {
		Assert.assertEquals(expected, Json.get(node, jsonPath).longValue());
		return this;
	}

	public JsonAssert<K> assertEquals(float expected, String jsonPath, float delta) {
		Assert.assertEquals(expected, Json.get(node, jsonPath).floatValue(), delta);
		return this;
	}

	public JsonAssert<K> assertEquals(double expected, String jsonPath, double delta) {
		Assert.assertEquals(expected, Json.get(node, jsonPath).doubleValue(), delta);
		return this;
	}

	public JsonAssert<K> assertEquals(DateTime expected, String jsonPath) {
		Assert.assertEquals(expected, DateTime.parse(Json.get(node, jsonPath).asText()));
		return this;
	}

	public JsonAssert<K> assertEquals(boolean expected, String jsonPath) {
		Assert.assertEquals(expected, Json.get(node, jsonPath).asBoolean());
		return this;
	}

	public JsonAssert<K> assertEquals(JsonNode expected) {
		Assert.assertEquals(expected, node);
		return this;
	}

	public JsonAssert<K> assertEquals(JsonNode expected, String jsonPath) {
		Assert.assertEquals(expected, Json.get(node, jsonPath));
		return this;
	}

	public JsonAssert<K> assertNotNull(String jsonPath) {
		JsonNode field = Json.get(node, jsonPath);
		if (field == null || field.isNull())
			Assert.fail(String.format("json property [%s] is null", jsonPath));
		return this;
	}

	public JsonAssert<K> assertNotNullOrEmpty(String jsonPath) {
		JsonNode field = Json.get(node, jsonPath);
		if (Strings.isNullOrEmpty(field.asText()))
			Assert.fail(String.format("json string property [%s] is null or empty", jsonPath));
		return this;
	}

	public JsonAssert<K> assertTrue(String jsonPath) {
		Assert.assertTrue(Json.get(node, jsonPath).asBoolean());
		return this;
	}

	public JsonAssert<K> assertFalse(String jsonPath) {
		Assert.assertFalse(Json.get(node, jsonPath).asBoolean());
		return this;
	}

	public JsonAssert<K> assertDateIsValid(String jsonPath) {
		assertNotNull(jsonPath);
		JsonNode field = Json.get(node, jsonPath);
		if (field.isTextual()) {
			try {
				DateTime.parse(field.asText());
				return this;
			} catch (IllegalArgumentException e) {
			}
		}
		Assert.fail(String.format(//
				"json property [%s] with value [%s] is not a valid SpaceDog date", jsonPath, field));
		return this;
	}

	public JsonAssert<K> assertDateIsRecent(String jsonPath) {
		long now = DateTime.now().getMillis();
		assertDateIsValid(jsonPath);
		DateTime date = DateTime.parse(Json.get(node, jsonPath).asText());
		if (date.isBefore(now - 3000) || date.isAfter(now + 3000))
			Assert.fail(String.format(//
					"json property [%s] with value [%s] is not a recent SpaceDog date (now +/- 3s)", jsonPath, date));
		return this;
	}

	public JsonAssert<K> assertSizeEquals(int size, String jsonPath) {
		JsonNode field = Json.get(node, jsonPath);
		if (Json.isNull(field))
			Assert.fail(String.format("property [%s] is null", jsonPath));
		if (size != field.size())
			Assert.fail(String.format("expected size [%s], json property [%s] node size [%s]", //
					size, jsonPath, field.size()));
		return this;
	}

	public JsonAssert<K> assertSizeEquals(int size) {
		if (size != node.size())
			Assert.fail(String.format("expected size [%s], root json node size [%s]", //
					size, node.size()));
		return this;
	}

	public JsonAssert<K> assertContainsValue(String expected, String fieldName) {
		if (!node.findValuesAsText(fieldName).contains(expected))
			Assert.fail(String.format("no field named [%s] found with value [%s]", fieldName, expected));
		return this;
	}

	public JsonAssert<K> assertContains(JsonNode expected) {
		if (!Iterators.contains(node.elements(), expected))
			Assert.fail(String.format(//
					"response does not contain [%s]", expected));
		return this;
	}

	public JsonAssert<K> assertContains(JsonNode expected, String jsonPath) {
		if (!Iterators.contains(Json.get(node, jsonPath).elements(), expected))
			Assert.fail(String.format(//
					"field [%s] does not contain [%s]", jsonPath, expected));
		return this;
	}

	public JsonAssert<K> assertNotPresent(String jsonPath) {
		JsonNode field = Json.get(node, jsonPath);
		if (field != null)
			Assert.fail(String.format(//
					"json path [%s] contains [%s]", jsonPath, field));
		return this;
	}

	public JsonAssert<K> assertPresent(String jsonPath) {
		JsonNode field = Json.get(node, jsonPath);
		if (field == null)
			Assert.fail(String.format(//
					"json path [%s] not found", jsonPath));
		return this;
	}

	public JsonAssert<K> assertString(String jsonPath) {
		assertPresent(jsonPath);
		JsonNode field = Json.get(node, jsonPath);
		if (!field.isTextual())
			Assert.fail(String.format(//
					"json path [%s] not string", jsonPath));
		return this;
	}

	public JsonAssert<K> assertInteger(String jsonPath) {
		assertPresent(jsonPath);
		JsonNode field = Json.get(node, jsonPath);
		if (!field.isInt())
			Assert.fail(String.format(//
					"json path [%s] not integer", jsonPath));
		return this;
	}

}
