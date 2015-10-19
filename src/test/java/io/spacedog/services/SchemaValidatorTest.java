/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.services.SchemaValidator.InvalidSchemaException;

public class SchemaValidatorTest extends Assert {

	@Test
	public void shouldSucceedToValidateSchema() {

		// no yet valid
		// testValidSchema(Json.startObject().startObject("car").put("_type",
		// "stash"));

		testValidSchema(Json.startObject().startObject("car").startObject("color").put("_type", "enum")
				.put("_required", true).end().startObject("model").put("_type", "string").put("_required", true));
		testValidSchema(Json.startObject().startObject("car").startObject("color").put("_type", "enum")
				.put("_required", true).end().startObject("model").put("_type", "string").put("_required", true).end()
				.startObject("descr").startObject("short").put("_type", "text").end().startObject("detailed")
				.put("_type", "stash"));
		testValidSchema(Json.startObject().startObject("car").startObject("color").put("_type", "enum")
				.put("_required", true).put("_array", true).end().startObject("model").put("_type", "object")
				.put("_required", true).put("_array", true).startObject("name").put("_type", "string")
				.put("_required", true).end().startObject("description").put("_type", "text"));
	}

	private void testValidSchema(JsonBuilder<ObjectNode> builder) {
		ObjectNode schema = builder.build();
		System.out.println("Valid schema: " + schema.toString());
		SchemaValidator.validate("car", schema);
	}

	@Test
	public void shouldFailToValidateSchema() {
		testInvalidSchema(Json.startObject().put("XXX", "XXX"));
		testInvalidSchema(Json.startObject().put("car", "XXX"));
		testInvalidSchema(Json.startObject().startObject("car"));
		testInvalidSchema(Json.startObject().startObject("car").end().startObject("XXX"));
		testInvalidSchema(Json.startObject().startObject("car").put("_type", "XXX"));
		testInvalidSchema(Json.startObject().startObject("car").put("type", "XXX"));
		testInvalidSchema(Json.startObject().startObject("car").put("_type", "object").startObject("name")
				.put("_type", "text").end().end().put("XXX", "hello"));
		testInvalidSchema(Json.startObject().startObject("car").put("_type", "object").startObject("name")
				.put("_type", "text").put("_XXX", true));
		testInvalidSchema(Json.startObject().startObject("car").put("_type", "object").startObject("name")
				.put("_type", "text").startObject("XXX"));
		testInvalidSchema(Json.startObject().startObject("car").put("_type", "object").startObject("name")
				.put("_type", "text").put("XXX", true));
	}

	private void testInvalidSchema(JsonBuilder<ObjectNode> builder) {
		ObjectNode schema = builder.build();
		System.out.println("Invalid schema: " + schema.toString());
		try {
			SchemaValidator.validate("car", schema);
			fail("an InvalidSchemaException should be thrown");
		} catch (InvalidSchemaException e) {
			e.printStackTrace();
		}
	}
}
