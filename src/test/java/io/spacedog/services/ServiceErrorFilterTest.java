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
				.assertEquals("[/v1/toto] is not a valid SpaceDog route", "error.message");
	}
}
