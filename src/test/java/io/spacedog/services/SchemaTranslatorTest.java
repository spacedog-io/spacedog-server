/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.Resources;

public class SchemaTranslatorTest extends Assert {

	@Test
	public void shouldTranslateSchema() throws IOException {
		// Charset utf8 = Charset.forName("UTF-8");

		// load schema
		URL urlSchema = Resources.getResource("io/spacedog/services/SchemaTranslatorTest-schema.json");
		JsonNode schema = Json.getMapper().readTree(urlSchema);

		// validate and translate
		SchemaValidator.validate("myschema", schema);
		ObjectNode mapping = SchemaTranslator.translate("myschema", schema);
		System.out.println("Translated schema into mapping =");
		System.out.println(mapping.toString());

		// load expected mapping
		URL urlExpectedMapping = Resources.getResource("io/spacedog/services/SchemaTranslatorTest-mapping.json");
		JsonNode expectedMapping = Json.getMapper().readTree(urlExpectedMapping);

		// assert
		assertEquals(mapping, expectedMapping);
	}
}