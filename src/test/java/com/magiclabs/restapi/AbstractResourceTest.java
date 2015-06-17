package com.magiclabs.restapi;

import org.junit.Test;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class AbstractResourceTest extends AbstractTest {

	@Test
	public void shouldConvertRuntimeExceptionToJsonError() {
		JsonObject json = AbstractResource.toJsonObject(new RuntimeException(
				new NullPointerException()));

		assertEquals("java.lang.RuntimeException", json.get("type").asString());
		assertEquals("java.lang.NullPointerException", json.get("message")
				.asString());
		assertTrue(json.get("trace").asArray().size() > 5);
		assertEquals("java.lang.NullPointerException", json.get("cause")
				.asObject().get("type").asString());
		assertEquals(JsonValue.NULL, json.get("cause").asObject()
				.get("message"));
		assertTrue(json.get("cause").asObject().get("trace").asArray().size() > 5);
	}
}
