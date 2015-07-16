package com.magiclabs.restapi;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

import com.eclipsesource.json.JsonObject;
import com.google.common.io.Resources;

public class SchemaTranslatorTest extends Assert {

	@Test
	public void shouldTranslateSchema() throws IOException {
		Charset utf8 = Charset.forName("UTF-8");

		// load schema
		URL urlSchema = Resources
				.getResource("com/magiclabs/restapi/SchemaTranslatorTest-schema.json");
		String jsonSchema = Resources.toString(urlSchema, utf8);
		JsonObject schema = JsonObject.readFrom(jsonSchema);

		// validate and translate
		SchemaValidator.validate("myschema", schema);
		JsonObject mapping = SchemaTranslator.translate("myschema", schema);

		try {
			// load expected mapping
			URL urlExpectedMapping = Resources
					.getResource("com/magiclabs/restapi/SchemaTranslatorTest-mapping.json");
			String jsonExpectedMapping = Resources.toString(urlExpectedMapping,
					utf8);
			JsonObject expectedMapping = JsonObject
					.readFrom(jsonExpectedMapping);

			// assert
			assertTrue(Json.equals(mapping, expectedMapping));

		} catch (IllegalArgumentException exc) {

			// save mapping if no expected mapping found
			Path path = Paths.get("SchemaTranslatorTest-mapping.json");
			BufferedWriter writer = Files.newBufferedWriter(path, utf8);
			writer.write(mapping.toString());
			writer.close();
		}

	}
}