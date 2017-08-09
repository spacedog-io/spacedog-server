package io.spacedog.services;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.core.Json8;
import io.spacedog.model.PushService;
import io.spacedog.sdk.PushRequest;
import io.spacedog.utils.Utils;

public class PushResourceOtherTests extends Assert {

	@Test
	public void convertJsonMessageToSnsMessageTest() {

		ObjectNode jsonMessage = Json8.object("APNS", //
				Json8.object("aps", Json8.object("alert", "coucou")));
		Utils.info("jsonMessage = %s", jsonMessage.toString());

		// convert json message to sns message
		// it converts field objects to string
		ObjectNode convertedSnsMessage = PushResource.toSnsMessage(PushService.APNS, jsonMessage);
		Utils.info("convertedSnsMessageString = %s", convertedSnsMessage.toString());
		assertEquals(Json8.object("aps", Json8.object("alert", "coucou")), //
				Json8.readObject(convertedSnsMessage.get("APNS").asText()));

		// convert sns message to sns message
		// it should not change anything since already converted
		convertedSnsMessage = PushResource.toSnsMessage(PushService.APNS, convertedSnsMessage);
		Utils.info("convertedSnsMessageString = %s", convertedSnsMessage.toString());
		assertEquals(Json8.object("aps", Json8.object("alert", "coucou")), //
				Json8.readObject(convertedSnsMessage.get("APNS").asText()));
	}

	@Test
	public void convertTextMessageToSnsMessage() {

		PushRequest request = new PushRequest().text("coucou");
		ObjectNode objectMessage = PushResource.toJsonMessage(request);

		ObjectNode snsMessage = PushResource.toSnsMessage(PushService.BAIDU, objectMessage);
		assertEquals("coucou", snsMessage.get("default").asText());

		snsMessage = PushResource.toSnsMessage(PushService.APNS, objectMessage);
		assertEquals(Json8.object("aps", Json8.object("alert", "coucou")), //
				Json8.readObject(snsMessage.get("APNS").asText()));

		snsMessage = PushResource.toSnsMessage(PushService.APNS_SANDBOX, objectMessage);
		assertEquals(Json8.object("aps", Json8.object("alert", "coucou")), //
				Json8.readObject(snsMessage.get("APNS_SANDBOX").asText()));

		snsMessage = PushResource.toSnsMessage(PushService.GCM, objectMessage);
		assertEquals(Json8.object("data", Json8.object("message", "coucou")), //
				Json8.readObject(snsMessage.get("GCM").asText()));
	}
}
