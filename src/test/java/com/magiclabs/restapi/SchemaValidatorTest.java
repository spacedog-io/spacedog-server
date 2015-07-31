package com.magiclabs.restapi;

import org.junit.Assert;
import org.junit.Test;

import com.eclipsesource.json.JsonObject;
import com.magiclabs.restapi.SchemaValidator.InvalidSchemaException;

public class SchemaValidatorTest extends Assert {

	@Test
	public void shouldSucceedToValidateSchema() {

		// no yet valid
		// testValidSchema(Json.builder().stObj("car").add("_type", "stash"));

		testValidSchema(Json.builder().stObj("car").stObj("color")
				.add("_type", "enum").add("_required", true).end()
				.stObj("model").add("_type", "string").add("_required", true));
		testValidSchema(Json.builder().stObj("car").stObj("color")
				.add("_type", "enum").add("_required", true).end()
				.stObj("model").add("_type", "string").add("_required", true)
				.end().stObj("descr").stObj("short").add("_type", "text").end()
				.stObj("detailed").add("_type", "stash"));
		testValidSchema(Json.builder().stObj("car").stObj("color")
				.add("_type", "enum").add("_required", true)
				.add("_array", true).end().stObj("model")
				.add("_type", "object").add("_required", true)
				.add("_array", true).stObj("name").add("_type", "string")
				.add("_required", true).end().stObj("description")
				.add("_type", "text"));
	}

	private void testValidSchema(JsonBuilder builder) {
		JsonObject schema = builder.build();
		System.out.println("Valid schema: " + schema.toString());
		SchemaValidator.validate("car", schema);
	}

	@Test
	public void shouldFailToValidateSchema() {
		testInvalidSchema(Json.builder().add("XXX", "XXX"));
		testInvalidSchema(Json.builder().add("car", "XXX"));
		testInvalidSchema(Json.builder().stObj("car"));
		testInvalidSchema(Json.builder().stObj("car").end().stObj("XXX"));
		testInvalidSchema(Json.builder().stObj("car").add("_type", "XXX"));
		testInvalidSchema(Json.builder().stObj("car").add("type", "XXX"));
		testInvalidSchema(Json.builder().stObj("car").add("_type", "object")
				.stObj("name").add("_type", "text").end().end()
				.add("XXX", "hello"));
		testInvalidSchema(Json.builder().stObj("car").add("_type", "object")
				.stObj("name").add("_type", "text").add("_XXX", true));
		testInvalidSchema(Json.builder().stObj("car").add("_type", "object")
				.stObj("name").add("_type", "text").stObj("XXX"));
		testInvalidSchema(Json.builder().stObj("car").add("_type", "object")
				.stObj("name").add("_type", "text").add("XXX", true));
	}

	private void testInvalidSchema(JsonBuilder builder) {
		JsonObject schema = builder.build();
		System.out.println("Invalid schema: " + schema.toString());
		try {
			SchemaValidator.validate("car", schema);
			fail("an InvalidSchemaException should be thrown");
		} catch (InvalidSchemaException e) {
			e.printStackTrace();
		}
	}
}
