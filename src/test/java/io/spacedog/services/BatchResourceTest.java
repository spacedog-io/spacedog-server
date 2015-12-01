/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceDogHelper.User;
import io.spacedog.client.SpaceRequest;

public class BatchResourceTest extends Assert {

	@Test
	public void shouldSuccessfullyExecuteBatch() throws Exception {

		SpaceDogHelper.Account testAccount = SpaceDogHelper.resetTestAccount();
		User user = SpaceDogHelper.createUser(testAccount.backendKey, "vince", "hi vince", "vince@dog.com");
		User dave = SpaceDogHelper.createUser(testAccount.backendKey, "dave", "hi dave", "dave@dog.com");

		ArrayNode batch = Json.arrayBuilder()//
				.object()//
				.put("method", "GET")//
				.put("uri", "/v1/data/user/vince")//
				.end()//
				.object()//
				.put("method", "GET")//
				.put("uri", "/v1/data/user/dave")//
				.end()//
				.build();

		SpaceRequest.post("/v1/batch").backendKey(testAccount).body(batch).go(200);

	}
}
