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
		SpaceDogHelper.User fred = SpaceDogHelper.createUser(testAccount, "fred", "hi fred", "david@spacedog.io");
		SpaceDogHelper.User nath = SpaceDogHelper.createUser(testAccount, "nath", "hi nath", "david@spacedog.io");

		// subscribe
		SpaceRequest.post("/v1/device").backendKey(testAccount).basicAuth(vince).go(201);
		SpaceRequest.post("/v1/device").backendKey(testAccount).basicAuth(fred).go(201);
		SpaceRequest.post("/v1/device").backendKey(testAccount).basicAuth(nath).go(201);

		// push all
		SpaceRequest.post("/v1/device/push").basicAuth(testAccount).go(200);
	}

}
