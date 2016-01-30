/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.junit.Assert;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Json;

public class PushResourceTest extends Assert {

	// @Test
	public void subscribeToTopicsAndPush() throws Exception {

		// prepare
		SpaceDogHelper.prepareTest();
		SpaceDogHelper.Account testAccount = SpaceDogHelper.resetTestAccount();
		SpaceDogHelper.User vince = SpaceDogHelper.createUser(testAccount, "vince", "hi vince", "david@spacedog.io");
		SpaceDogHelper.User fred = SpaceDogHelper.createUser(testAccount, "fred", "hi fred", "davattias@gmail.com");
		SpaceDogHelper.User nath = SpaceDogHelper.createUser(testAccount, "nath", "hi nath", "attias666@gmail.com");

		// subscribe
		String vinceDeviceId = SpaceRequest.post("/v1/device").backendKey(testAccount).basicAuth(vince)//
				.body(Json.objectBuilder().put("protocol", "email").put("endpoint", vince.email))//
				.go(200, 201).objectNode().get("id").asText();

		String fredDeviceId = SpaceRequest.post("/v1/device").backendKey(testAccount).basicAuth(fred)//
				.body(Json.objectBuilder().put("protocol", "email").put("endpoint", fred.email))//
				.go(200, 201).objectNode().get("id").asText();

		String nathDeviceId = SpaceRequest.post("/v1/device").backendKey(testAccount).basicAuth(nath)//
				.body(Json.objectBuilder().put("protocol", "email").put("endpoint", nath.email))//
				.go(200, 201).objectNode().get("id").asText();

		// push all
		SpaceRequest.post("/v1/device/push").basicAuth(testAccount).go(200);
	}

}
