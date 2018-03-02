package io.spacedog.server;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.push.PushProtocol;
import io.spacedog.client.push.PushRequest;
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
		ObjectNode convertedSnsMessage = PushService.toSnsMessage(PushProtocol.APNS, jsonMessage);
		Utils.info("convertedSnsMessageString = %s", convertedSnsMessage.toString());
		assertEquals(Json.object("aps", Json.object("alert", "coucou")), //
				Json.readObject(convertedSnsMessage.get("APNS").asText()));

		// convert sns message to sns message
		// it should not change anything since already converted
		convertedSnsMessage = PushService.toSnsMessage(PushProtocol.APNS, convertedSnsMessage);
		Utils.info("convertedSnsMessageString = %s", convertedSnsMessage.toString());
		assertEquals(Json.object("aps", Json.object("alert", "coucou")), //
				Json.readObject(convertedSnsMessage.get("APNS").asText()));
	}

	@Test
	public void convertTextMessageToSnsMessage() {

		PushRequest request = new PushRequest().text("coucou");
		ObjectNode objectMessage = PushService.toJsonMessage(request);

		ObjectNode snsMessage = PushService.toSnsMessage(PushProtocol.BAIDU, objectMessage);
		assertEquals("coucou", snsMessage.get("default").asText());

		snsMessage = PushService.toSnsMessage(PushProtocol.APNS, objectMessage);
		assertEquals(Json.object("aps", Json.object("alert", "coucou")), //
				Json.readObject(snsMessage.get("APNS").asText()));

		snsMessage = PushService.toSnsMessage(PushProtocol.APNS_SANDBOX, objectMessage);
		assertEquals(Json.object("aps", Json.object("alert", "coucou")), //
				Json.readObject(snsMessage.get("APNS_SANDBOX").asText()));

		snsMessage = PushService.toSnsMessage(PushProtocol.GCM, objectMessage);
		assertEquals(Json.object("data", Json.object("message", "coucou")), //
				Json.readObject(snsMessage.get("GCM").asText()));
	}
}
