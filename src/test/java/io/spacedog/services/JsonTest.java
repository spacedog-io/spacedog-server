/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.junit.Test;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class JsonTest extends AbstractTest {

	@Test
	public void shouldFollowThePath() {
		JsonValue json = Json.builder().stObj("riri").stArr("fifi").add(12).addObj().add("loulou", false).build();
		assertEquals(false, Json.get(json, "riri.fifi.1.loulou").asBoolean());
	}

	@Test
	public void shouldCopyJsonValues() {
		checkCopy(JsonValue.valueOf(null));
		checkCopy(JsonValue.valueOf(true));
		checkCopy(JsonValue.valueOf(false));
		checkCopy(JsonValue.valueOf(1));
		checkCopy(JsonValue.valueOf(1.1));
		checkCopy(JsonValue.valueOf(2l));
		checkCopy(JsonValue.valueOf(2.2d));
		checkCopy(JsonValue.valueOf("toto"));
		checkCopy(new JsonObject());
		checkCopy(Json.builder().add("p", true).add("b", 1).add("c", 1.1).add("d", "toto").stObj("e").add("f", "titi")
				.stArr("g").add("tutu").add(true).add(1).add(1.1).add(new JsonObject()).build());
	}

	/**
	 * @param valueOf
	 */
	private void checkCopy(JsonValue value) {
		assertTrue(Json.equals(value, Json.copy(value)));
	}
}
