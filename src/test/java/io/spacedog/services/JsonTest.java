package io.spacedog.services;

import io.spacedog.services.Json;

import org.junit.Test;

import com.eclipsesource.json.JsonValue;

public class JsonTest extends AbstractTest {

	@Test
	public void shouldFollowThePath() {
		JsonValue json = Json.builder().stObj("riri").stArr("fifi").add(12)
				.addObj().add("loulou", false).build();
		assertEquals(false, Json.get(json, "riri.fifi.1.loulou").asBoolean());
	}
}
