/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;

public class JsonTest extends Assert {

	@Test
	public void shouldSucceedToGet() {
		JsonNode json = Json.startObject().startObject("riri").startArray("fifi").add(12).startObject()
				.put("loulou", false).build();
		assertEquals(false, Json.get(json, "riri.fifi.1.loulou").asBoolean());
	}

	@Test
	public void shouldSucceedToSet() {
		JsonNode json = Json.startObject().startObject("riri").startArray("fifi").add(12).startObject()
				.put("loulou", false).build();
		Json.set(json, "riri.fifi.1.loulou", BooleanNode.TRUE);
		assertEquals(true, Json.get(json, "riri.fifi.1.loulou").asBoolean());
	}
}
