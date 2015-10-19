/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonTest extends AbstractTest {

	@Test
	public void shouldFollowThePath() {
		JsonNode json = Json.startObject().startObject("riri").startArray("fifi").add(12).startObject()
				.put("loulou", false).build();
		assertEquals(false, Json.get(json, "riri.fifi.1.loulou").asBoolean());
	}
}
