/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.junit.Assert;
import org.junit.Test;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceDogHelper.Account;
import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceSuite.TestAlways;

@TestAlways
public class HttpPermanentRedirectTest extends Assert {

	@Test
	public void shouldBeRedirectedToMainPort() throws Exception {

		// should redirect from http to https and get the root page

		String content = SpaceRequest.get(false, "/").go(200).httpResponse().getBody();
		assertTrue(content.startsWith("<!DOCTYPE html>"));

		// should redirect from http to https and fetch all account data objects
		// TODO in the future the redirect mechanism should not work for api
		// calls since api should always be secured

		Account account = SpaceDogHelper.resetTestAccount();
		SpaceDogHelper.createUser(account, "bob", "hi bob", "bob@dog.com");
		SpaceRequest.get(false, "/v1/data?refresh=true").backendKey(account).go(200)//
				.assertEquals("bob", "results.0.username").assertEquals("bob@dog.com", "results.0.email");
	}
}