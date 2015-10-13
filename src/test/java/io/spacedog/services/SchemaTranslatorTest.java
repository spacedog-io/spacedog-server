/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;

import org.junit.Assert;
import org.junit.Test;

import com.eclipsesource.json.JsonObject;
import com.google.common.io.Resources;

public class SchemaTranslatorTest extends Assert {

	@Test
	public void shouldTranslateSchema() throws IOException {
		Charset utf8 = Charset.forName("UTF-8");

		// load schema
		URL urlSchema = Resources.getResource("io/spacedog/services/SchemaTranslatorTest-schema.json");
		String jsonSchema = Resources.toString(urlSchema, utf8);
		JsonObject schema = JsonObject.readFrom(jsonSchema);

		// validate and translate
		SchemaValidator.validate("myschema", schema);
		JsonObject mapping = SchemaTranslator.translate("myschema", schema);
		System.out.println("Translated schema into mapping =");
		System.out.println(mapping.toString());

		// load expected mapping
		URL urlExpectedMapping = Resources.getResource("io/spacedog/services/SchemaTranslatorTest-mapping.json");
		String jsonExpectedMapping = Resources.toString(urlExpectedMapping, utf8);
		JsonObject expectedMapping = JsonObject.readFrom(jsonExpectedMapping);

		// assert
		assertTrue(Json.equals(mapping, expectedMapping));
	}
}