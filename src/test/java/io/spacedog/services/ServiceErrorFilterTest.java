/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.junit.Assert;
import org.junit.Test;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceDogHelper.Account;
import io.spacedog.client.SpaceRequest;

public class ServiceErrorFilterTest extends Assert {

	@Test
	public void shouldReturnCORSHeaders() throws Exception {

		Account testAccount = SpaceDogHelper.resetTestAccount();

		// should fail to access invalid route

		SpaceRequest.get("/v1/toto").backendKey(testAccount).go(404)//
				.assertFalse("success")//
				.assertEquals("[/v1/toto] is not a valid SpaceDog path", "error.message");

		// should fail to use this method for this valid route

		SpaceRequest.put("/v1/login").basicAuth(testAccount).go(405)//
				.assertFalse("success")//
				.assertEquals("method [PUT] not valid for SpaceDog path [/v1/login]", "error.message");
	}
}
