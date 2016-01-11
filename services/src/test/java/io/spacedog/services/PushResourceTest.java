/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.junit.Assert;
import org.junit.Test;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceRequest;

public class PushResourceTest extends Assert {

	@Test
	public void subscribeToTopicsAndPush() throws Exception {

		// prepare
		SpaceDogHelper.printTestHeader();
		SpaceDogHelper.Account testAccount = SpaceDogHelper.resetTestAccount();
		SpaceDogHelper.User vince = SpaceDogHelper.createUser(testAccount, "vince", "hi vince", "david@spacedog.io");
		SpaceDogHelper.User fred = SpaceDogHelper.createUser(testAccount, "fred", "hi fred", "davattias@gamil.com");
		SpaceDogHelper.User nath = SpaceDogHelper.createUser(testAccount, "nath", "hi nath", "attias666@gmail.com");

		// subscribe
		SpaceRequest.post("/v1/device").backendKey(testAccount).basicAuth(vince).go(200, 201);
		SpaceRequest.post("/v1/device").backendKey(testAccount).basicAuth(fred).go(200, 201);
		SpaceRequest.post("/v1/device").backendKey(testAccount).basicAuth(nath).go(200, 201);

		// push all
		SpaceRequest.post("/v1/device/push").basicAuth(testAccount).go(200);
	}

}
