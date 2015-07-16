package com.magiclabs.restapi;

import org.junit.Assert;
import org.junit.Test;

import com.eclipsesource.json.JsonObject;

public class SchemaTranslatorTest extends Assert {

	@Test
	public void shouldSuccessfullyTranslateSchema() {

		// testSchemaTranslation(
		// Json.builder().stObj("mytype").add("_type", "stash").build(),
		// Json.builder().add("type", "object").add("enabled", false)
		// .build());

		testSchemaTranslation(
				Json.builder().stObj("mytype").stObj("color")
						.add("_type", "enum").add("_required", true).end()
						.stObj("model").add("_type", "code")
						.add("_required", true).build(),
				Json.builder().stObj("color").add("type", "string")
						.add("index", "not_analyzed").end().stObj("model")
						.add("type", "string").add("index", "not_analyzed")
						.build());
	}

	private void testSchemaTranslation(JsonObject schema,
			JsonObject expectedDataMapping) {
		JsonObject mapping = SchemaTranslator.translate("mytype", schema);
		JsonObject expected = expected("mytype", expectedDataMapping, schema);
		if (!Json.equals(mapping, expected)) {
			System.out.println(String.format("Schema = %s",
					Json.prettyString(schema)));
			System.out.println(String.format("Expected mapping = %s",
					Json.prettyString(expected)));
			System.out.println(String.format("Translate result = %s",
					Json.prettyString(mapping)));
			fail();
		}
	}

	JsonObject expected(String type, JsonObject expectedDataMapping,
			JsonObject schema) {
		JsonObject meta = Json.builder() //
				.add("type", "object") //
				.stObj("properties") //
				.stObj("createdBy") //
				.add("type", "string") //
				.add("index", "not_analyzed") //
				.end() //
				.stObj("updatedBy") //
				.add("type", "string") //
				.add("index", "not_analyzed") //
				.end() //
				.stObj("createdAt") //
				.add("type", "date") //
				.add("format", "date_time") //
				.end() //
				.stObj("updatedAt") //
				.add("type", "date") //
				.add("format", "date_time") //
				.end() //
				.build();

		expectedDataMapping.add("meta", meta);

		return Json.builder() //
				.stObj(type) //
				.stObj("_timestamp") //
				.add("enabled", false) //
				.end() //
				.add("dynamic", "strict") //
				.add("date_detection", false) //
				.addJson("properties", expectedDataMapping) //
				.addJson("_meta", schema) //
				.build();
	}
}