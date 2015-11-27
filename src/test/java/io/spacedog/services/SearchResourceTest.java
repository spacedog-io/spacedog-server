/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceDogHelper.Account;
import io.spacedog.client.SpaceRequest;

public class SearchResourceTest extends Assert {

	@Test
	public void shouldSucceedToDeleteObjects() throws Exception {

		Account testAccount = SpaceDogHelper.resetTestAccount();
		SpaceDogHelper.setSchema(SchemaBuilder2.builder("message").textProperty("text", "english", true).build(),
				testAccount);

		// should successfully create 4 messages and 1 user

		SpaceDogHelper.createUser(testAccount, "riri", "hi riri", "riri@dog.com");

		SpaceRequest.post("/v1/data/message").basicAuth(testAccount)
				.body(Json.startObject().put("text", "what's up?").build()).go(201);
		SpaceRequest.post("/v1/data/message").basicAuth(testAccount)
				.body(Json.startObject().put("text", "wanna drink something?").build()).go(201);
		SpaceRequest.post("/v1/data/message").basicAuth(testAccount)
				.body(Json.startObject().put("text", "pretty cool, hein?").build()).go(201);
		String id = SpaceRequest.post("/v1/data/message").basicAuth(testAccount)
				.body(Json.startObject().put("text", "so long guys").build()).go(201).getFromJson("id").asText();

		SpaceDogHelper.refresh(testAccount);

		SpaceRequest.get("/v1/data").basicAuth(testAccount).go(200).assertEquals(5, "total");

		// should succeed to delete message containing 'up' by query
		ObjectNode query = Json.startObject().startObject("query")//
				.startObject("match").put("text", "up")//
				.build();
		SpaceRequest.delete("/v1/search/message").basicAuth(testAccount).body(query).go(200);
		SpaceDogHelper.refresh(testAccount);
		SpaceRequest.get("/v1/data/message").basicAuth(testAccount).go(200).assertEquals(3, "total");

		// should succeed to delete objects containing 'wanna' and 'riri'
		query = Json.startObject().startObject("query")//
				.startObject("match").put("_all", "wanna riri")//
				.build();
		SpaceRequest.delete("/v1/search").basicAuth(testAccount).body(query).go(200);
		SpaceDogHelper.refresh(testAccount);
		SpaceRequest.get("/v1/data").basicAuth(testAccount).go(200).assertEquals(2, "total");
	}
}
