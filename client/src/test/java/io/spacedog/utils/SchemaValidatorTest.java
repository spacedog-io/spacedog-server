/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.utils.SchemaValidator.SchemaException;

public class SchemaValidatorTest extends Assert {

	@Test
	public void shouldSucceedToValidateSchema() {

		testValidSchema(Json.builder().object().object("car").object("color").add("_type", "enum").add("_required", true)
				.end().object("model").add("_type", "string").add("_required", true));
		testValidSchema(Json.builder().object().object("car").object("color").add("_type", "enum").add("_required", true)
				.end().object("model").add("_type", "string").add("_required", true).end().object("descr")
				.object("short").add("_type", "text").end().object("detailed").add("_type", "stash"));
		testValidSchema(Json.builder().object().object("car").object("color").add("_type", "enum").add("_required", true)
				.add("_array", true).end().object("model").add("_type", "object").add("_required", true)
				.add("_array", true).object("name").add("_type", "string").add("_required", true).end()
				.object("description").add("_type", "text"));
	}

	private void testValidSchema(JsonBuilder<ObjectNode> builder) {
		ObjectNode schema = builder.build();
		System.out.println("Valid schema: " + schema.toString());
		SchemaValidator.validate("car", schema);
	}

	@Test
	public void shouldFailToValidateSchema() {
		testInvalidSchema(Json.builder().object().add("XXX", "XXX"));
		testInvalidSchema(Json.builder().object().add("car", "XXX"));
		testInvalidSchema(Json.builder().object().object("car"));
		testInvalidSchema(Json.builder().object().object("car").end().object("XXX"));
		testInvalidSchema(Json.builder().object().object("car").add("_type", "XXX"));
		testInvalidSchema(Json.builder().object().object("car").add("type", "XXX"));
		testInvalidSchema(Json.builder().object().object("car").add("_type", "object").object("name").add("_type", "text")
				.end().end().add("XXX", "hello"));
		testInvalidSchema(Json.builder().object().object("car").add("_type", "object").object("name").add("_type", "text")
				.object("XXX"));
		testInvalidSchema(Json.builder().object().object("car").add("_type", "object").object("name").add("_type", "text")
				.add("XXX", true));
		testInvalidSchema(Json.builder().object().object("car").object("color").add("_type", "enum")
				.add("_required", true).end().object("").add("_type", "string").add("_required", true));
	}

	private void testInvalidSchema(JsonBuilder<ObjectNode> builder) {
		ObjectNode schema = builder.build();
		System.err.println("Invalid schema: " + schema.toString());
		try {
			SchemaValidator.validate("car", schema);
			fail("an InvalidSchemaException should be thrown");
		} catch (SchemaException e) {
			e.printStackTrace();
		}
	}
}
