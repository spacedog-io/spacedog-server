package io.spacedog.server;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.spacedog.client.http.SpaceException;
import io.spacedog.utils.Exceptions;

public class JsonPayloadTest extends Assert {

	@Test
	public void shouldConvertRuntimeExceptionToJsonError() {
		JsonNode json = JsonPayload.toJson(new RuntimeException(new NullPointerException()), true);
		assertEquals(RuntimeException.class.getName(), json.get("type").asText());
		assertEquals(NullPointerException.class.getName(), json.get("message").asText());
		assertTrue(json.get("trace").size() > 5);
		assertEquals(NullPointerException.class.getName(), json.get("cause").get("type").asText());
		assertNull(json.get("cause").get("message"));
		assertTrue(json.get("cause").get("trace").size() > 5);
	}

	@Test
	public void convertSpaceExceptionToJsonErrorDebugMode() {
		JsonNode json = JsonPayload.toJson(Exceptions.alreadyExists("credentials", "vince"), true);
		assertEquals(SpaceException.class.getName(), json.get("type").asText());
		assertEquals("[credentials][vince] already exists", json.get("message").asText());
		assertEquals("already-exists", json.get("code").asText());
		assertNull(json.get("cause"));
	}

	@Test
	public void convertSpaceExceptionToJsonErrorNoDebugMode() {
		JsonNode json = JsonPayload.toJson(Exceptions.alreadyExists("credentials", "vince"), false);
		assertNull(json.get("type"));
		assertEquals("[credentials][vince] already exists", json.get("message").asText());
		assertEquals("already-exists", json.get("code").asText());
		assertNull(json.get("cause"));
	}

}
