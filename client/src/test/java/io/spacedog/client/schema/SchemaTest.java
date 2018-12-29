package io.spacedog.client.schema;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.spacedog.utils.ClassResources;
import io.spacedog.utils.Json;

public class SchemaTest extends Assert {

	@Test
	public void testSchemaBuilder() {

		Schema schema = SchemaBuilder.builder("message")//
				.text("text")//
				.language("french")//
				.timestamp("publishedAt")//
				.bool("published")//

				.object("metadata")//
				.keyword("m1")//
				.integer("boost")//

				.build();

		System.out.println(Json.toString(schema.mapping(), true));

		JsonNode mapping = Json.readNode(//
				ClassResources.loadAsString(this, "mapping.json"));

		assertEquals(mapping, schema.mapping());
	}
}
