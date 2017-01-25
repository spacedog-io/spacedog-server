package io.spacedog.services;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.spacedog.client.SpaceTest;
import io.spacedog.services.PushResource.PushServices;
import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;

public class PushResourceOtherTests extends SpaceTest {

	@Test
	public void convertJsonMessageToSnsMessageTest() {

		ObjectNode jsonMessage = Json.object("APNS", //
				Json.object("aps", Json.object("alert", "coucou")));
		Utils.info("jsonMessage = %s", jsonMessage.toString());

		// convert json message to sns message
		// it converts field objects to string
		ObjectNode convertedSnsMessage = PushResource.toSnsMessage(PushServices.APNS, jsonMessage);
		Utils.info("convertedSnsMessageString = %s", convertedSnsMessage.toString());
		assertEquals(Json.object("aps", Json.object("alert", "coucou")), //
				Json.readObject(convertedSnsMessage.get("APNS").asText()));

		// convert sns message to sns message
		// it should not change anything since already converted
		convertedSnsMessage = PushResource.toSnsMessage(PushServices.APNS, convertedSnsMessage);
		Utils.info("convertedSnsMessageString = %s", convertedSnsMessage.toString());
		assertEquals(Json.object("aps", Json.object("alert", "coucou")), //
				Json.readObject(convertedSnsMessage.get("APNS").asText()));
	}

	@Test
	public void convertTextMessageToSnsMessage() {

		JsonNode textMessage = new TextNode("coucou");
		ObjectNode objectMessage = PushResource.toObjectMessage(textMessage);

		ObjectNode snsMessage = PushResource.toSnsMessage(PushServices.BAIDU, objectMessage);
		assertEquals("coucou", snsMessage.get("default").asText());

		snsMessage = PushResource.toSnsMessage(PushServices.APNS, objectMessage);
		assertEquals(Json.object("aps", Json.object("alert", "coucou")), //
				Json.readObject(snsMessage.get("APNS").asText()));

		snsMessage = PushResource.toSnsMessage(PushServices.APNS_SANDBOX, objectMessage);
		assertEquals(Json.object("aps", Json.object("alert", "coucou")), //
				Json.readObject(snsMessage.get("APNS_SANDBOX").asText()));

		snsMessage = PushResource.toSnsMessage(PushServices.GCM, objectMessage);
		assertEquals(Json.object("data", Json.object("message", "coucou")), //
				Json.readObject(snsMessage.get("GCM").asText()));
	}

	@Test
	public void convertInvalidMessageToSnsMessage() {

		try {
			JsonNode invalidMessage = new IntNode(123);
			PushResource.toObjectMessage(invalidMessage);
			fail();
		} catch (IllegalArgumentException e) {
		}
	}
}
