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

		testValidSchema(Json.objectBuilder().object("car").object("color").put("_type", "enum")
				.put("_required", true).end().object("model").put("_type", "string").put("_required", true));
		testValidSchema(Json.objectBuilder().object("car").object("color").put("_type", "enum")
				.put("_required", true).end().object("model").put("_type", "string").put("_required", true).end()
				.object("descr").object("short").put("_type", "text").end().object("detailed")
				.put("_type", "stash"));
		testValidSchema(Json.objectBuilder().object("car").object("color").put("_type", "enum")
				.put("_required", true).put("_array", true).end().object("model").put("_type", "object")
				.put("_required", true).put("_array", true).object("name").put("_type", "string")
				.put("_required", true).end().object("description").put("_type", "text"));
	}

	private void testValidSchema(JsonBuilder<ObjectNode> builder) {
		ObjectNode schema = builder.build();
		System.out.println("Valid schema: " + schema.toString());
		SchemaValidator.validate("car", schema);
	}

	@Test
	public void shouldFailToValidateSchema() {
		testInvalidSchema(Json.objectBuilder().put("XXX", "XXX"));
		testInvalidSchema(Json.objectBuilder().put("car", "XXX"));
		testInvalidSchema(Json.objectBuilder().object("car"));
		testInvalidSchema(Json.objectBuilder().object("car").end().object("XXX"));
		testInvalidSchema(Json.objectBuilder().object("car").put("_type", "XXX"));
		testInvalidSchema(Json.objectBuilder().object("car").put("type", "XXX"));
		testInvalidSchema(Json.objectBuilder().object("car").put("_type", "object").object("name")
				.put("_type", "text").end().end().put("XXX", "hello"));
		testInvalidSchema(Json.objectBuilder().object("car").put("_type", "object").object("name")
				.put("_type", "text").put("_XXX", true));
		testInvalidSchema(Json.objectBuilder().object("car").put("_type", "object").object("name")
				.put("_type", "text").object("XXX"));
		testInvalidSchema(Json.objectBuilder().object("car").put("_type", "object").object("name")
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
