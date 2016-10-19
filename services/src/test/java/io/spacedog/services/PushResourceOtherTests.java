package io.spacedog.services;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;

public class PushResourceOtherTests extends Assert {

	@Test
	public void convertJsonMessageToSnsMessageTest() {

		ObjectNode jsonMessage = Json.object("APNS", //
				Json.object("aps", Json.object("alert", "coucou")));
		Utils.info("jsonMessage = %s", jsonMessage.toString());

		// convert json message to sns message
		// it converts field objects to string
		ObjectNode convertedSnsMessage = PushResource.convertJsonMessageToSnsMessage(jsonMessage);
		Utils.info("convertedSnsMessageString = %s", convertedSnsMessage.toString());
		assertEquals(Json.object("aps", Json.object("alert", "coucou")), //
				Json.readObject(convertedSnsMessage.get("APNS").asText()));

		// convert sns message to sns message
		// it should not change anything since already converted
		convertedSnsMessage = PushResource.convertJsonMessageToSnsMessage(convertedSnsMessage);
		Utils.info("convertedSnsMessageString = %s", convertedSnsMessage.toString());
		assertEquals(Json.object("aps", Json.object("alert", "coucou")), //
				Json.readObject(convertedSnsMessage.get("APNS").asText()));
	}

	@Test
	public void convertTextMessageToSnsMessage() {

		JsonNode textMessage = new TextNode("coucou");
		ObjectNode snsMessage = PushResource.convertJsonMessageToSnsMessage(textMessage);

		assertEquals("coucou", snsMessage.get("default").asText());
		assertEquals(Json.object("aps", Json.object("alert", "coucou")), //
				Json.readObject(snsMessage.get("APNS").asText()));
		assertEquals(Json.object("aps", Json.object("alert", "coucou")), //
				Json.readObject(snsMessage.get("APNS_SANDBOX").asText()));
		assertEquals(Json.object("data", Json.object("message", "coucou")), //
				Json.readObject(snsMessage.get("GCM").asText()));
		assertEquals(Json.object("data", Json.object("message", "coucou")), //
				Json.readObject(snsMessage.get("ADM").asText()));
		assertEquals(Json.object("title", "coucou", "description", "coucou"), //
				Json.readObject(snsMessage.get("BAIDU").asText()));
	}

	@Test
	public void convertInvalidMessageToSnsMessage() {

		try {
			JsonNode invalidMessage = new IntNode(123);
			PushResource.convertJsonMessageToSnsMessage(invalidMessage);
			fail();
		} catch (IllegalArgumentException e) {
		}
	}
}
