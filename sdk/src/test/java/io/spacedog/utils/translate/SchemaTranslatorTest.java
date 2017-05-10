/**
 * © David Attias 2015
 */
package io.spacedog.utils.translate;

import java.io.IOException;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.Resources;

import io.spacedog.utils.Json7;
import io.spacedog.utils.SchemaTranslator;
import io.spacedog.utils.SchemaValidator;

public class SchemaTranslatorTest extends Assert {

	@Test
	public void shouldTranslateSchema() throws IOException {

		// load schema
		URL urlSchema = Resources.getResource(this.getClass(), "schema.json");
		JsonNode schema = Json7.readNode(urlSchema);

		// validate and translate
		SchemaValidator.validate("myschema", schema);
		ObjectNode mapping = SchemaTranslator.translate("myschema", schema);
		System.out.println("Translated schema into mapping =");
		System.out.println(mapping.toString());

		// load expected mapping
		URL urlExpectedMapping = Resources.getResource(this.getClass(), "mapping.json");
		JsonNode expectedMapping = Json7.readNode(urlExpectedMapping);

		// assert
		assertEquals(mapping, expectedMapping);
	}
}