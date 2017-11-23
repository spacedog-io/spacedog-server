/**
 * © David Attias 2015
 */
package io.spacedog.utils.translate;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.model.Schema;
import io.spacedog.utils.ClassResources;
import io.spacedog.utils.Json;

public class SchemaTranslatorTest extends Assert {

	@Test
	public void shouldTranslateSchema() throws IOException {

		// load schema
		JsonNode node = Json.readNode(ClassResources.loadToString(getClass(), "schema.json"));
		Schema schema = new Schema("myschema", Json.checkObject(node));

		// validate and translate
		ObjectNode mapping = schema.validate().translate();

		System.out.println("Translated schema into mapping =");
		System.out.println(Json.toPrettyString(mapping));

		// load expected mapping
		JsonNode expectedMapping = Json.readNode(//
				ClassResources.loadToString(getClass(), "mapping.json"));

		// assert
		assertEquals(mapping, expectedMapping);
	}
}