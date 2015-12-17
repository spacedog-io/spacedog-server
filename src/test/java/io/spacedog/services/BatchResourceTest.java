/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Sets;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceResponse;
import io.spacedog.client.SpaceSuite.TestAlways;

@TestAlways
public class BatchResourceTest extends Assert {

	@Test
	public void shouldSuccessfullyExecuteBatch() throws Exception {

		// we need to make sure the test account is reset and exists before to
		// be able to
		// reset it again by batch requests because we need the a

		SpaceDogHelper.resetTestAccount();

		// should succeed to reset test account and create message schema with
		// admin credentials

		ArrayNode batch = Json.arrayBuilder()//
				.object()//
				.put("method", "GET").put("path", "/v1/admin/account/test")//
				.end()//
				.object()//
				.put("method", "DELETE").put("path", "/v1/admin/account/test")//
				.end()//
				.object()//
				.put("method", "POST").put("path", "/v1/admin/account")//
				.object("content")//
				.put("backendId", "test")//
				.put("username", "test")//
				.put("password", "hi test")//
				.put("email", "test@dog.com")//
				.end()//
				.end()//
				.object()//
				.put("method", "POST").put("path", "/v1/schema/message")//
				.node("content",
						SchemaBuilder2.builder("message", "code")//
								.stringProperty("code", true)//
								.textProperty("text", "english", true).toString())//
				.end()//
				.object()//
				.put("method", "GET").put("path", "/v1/login")//
				.end()//
				.build();

		SpaceResponse spaceResponse = SpaceRequest.post("/v1/batch?debug=true").basicAuth("test", "hi test").body(batch)
				.go(200);

		int accountGetStatus = Json.get(spaceResponse.jsonNode(), "responses.0.status").asInt();

		if (accountGetStatus == 200)
			spaceResponse.assertEquals(200, "responses.0.status")//
					.assertEquals("test", "responses.0.content.backendId")//
					.assertEquals(200, "responses.1.status");
		else
			spaceResponse.assertEquals(404, "responses.0.status")//
					.assertEquals(404, "responses.1.status");

		spaceResponse.assertTrue("responses.2.success")//
				.assertEquals("test", "responses.2.id")//
				.assertEquals("account", "responses.2.type")//
				.assertTrue("responses.3.success")//
				.assertEquals("message", "responses.3.id")//
				.assertEquals("schema", "responses.3.type")//
				.assertTrue("responses.4.success")//
				.assertEquals(1, "debug.batchCredentialChecks");

		io.spacedog.client.SpaceDogHelper.Account testAccount = new io.spacedog.client.SpaceDogHelper.Account(//
				"test", "test", "hi test", "test@dog.com",
				spaceResponse.getFromJson("responses.2.backendKey").asText());

		// should succeed to create dave and vince users and fetch them with
		// simple backend key credentials

		batch = Json.arrayBuilder()//
				.object()//
				.put("method", "POST").put("path", "/v1/user")//
				.object("content")//
				.put("username", "vince")//
				.put("password", "hi vince")//
				.put("email", "vince@dog.com")//
				.end()//
				.end()//
				.object()//
				.put("method", "POST").put("path", "/v1/user")//
				.object("content")//
				.put("username", "dave")//
				.put("password", "hi dave")//
				.put("email", "dave@dog.com")//
				.end()//
				.end()//
				.object()//
				.put("method", "GET").put("path", "/v1/user/vince")//
				.end()//
				.object()//
				.put("method", "GET").put("path", "/v1/data/user/dave")//
				.end()//
				.build();

		SpaceRequest.post("/v1/batch?debug=true").backendKey(testAccount).body(batch).go(200)//
				.assertEquals("vince", "responses.0.id")//
				.assertEquals("dave", "responses.1.id")//
				.assertEquals("vince", "responses.2.content.username")//
				.assertEquals("dave", "responses.3.content.username")//
				.assertEquals(1, "debug.batchCredentialChecks");

		// should succeed to returns errors when batch requests are invalid, not
		// found, unauthorized, ...

		batch = Json.arrayBuilder()//
				.object()//
				.put("method", "POST").put("path", "/v1/user")//
				.object("content")//
				.put("username", "fred")//
				.put("password", "hi fred")//
				.end()//
				.end()//
				.object()//
				.put("method", "GET").put("path", "/v1/toto")//
				.end()//
				.object()//
				.put("method", "GET").put("path", "/v1/user/vince")//
				.end()//
				.object()//
				.put("method", "POST").put("path", "/v1/user")//
				.end()//
				.object()//
				.put("method", "DELETE").put("path", "/v1/user")//
				.end()//
				.object()//
				.put("method", "PUT").put("path", "/v1/user/vince/password")//
				.put("content", "hi vince 2")//
				.end()//
				.build();

		SpaceRequest.post("/v1/batch?debug=true").backendKey(testAccount).body(batch).go(200)//
				.assertFalse("responses.0.success")//
				.assertEquals(400, "responses.0.status")//
				.assertFalse("responses.1.success")//
				.assertEquals(404, "responses.1.status")//
				.assertEquals("vince", "responses.2.content.username")//
				.assertFalse("responses.3.success")//
				.assertEquals(400, "responses.3.status")//
				.assertFalse("responses.4.success")//
				.assertEquals(401, "responses.4.status")//
				.assertFalse("responses.5.success")//
				.assertEquals(401, "responses.5.status")//
				.assertEquals(1, "debug.batchCredentialChecks");

		// should succeed to create and update messages by batch

		batch = Json.arrayBuilder()//
				.object()//
				.put("method", "POST").put("path", "/v1/data/message")//
				.object("content")//
				.put("code", "1")//
				.put("text", "Hi guys!")//
				.end()//
				.end()//
				.object()//
				.put("method", "POST").put("path", "/v1/data/message")//
				.object("content")//
				.put("code", "2")//
				.put("text", "Pretty cool, huhh?")//
				.end()//
				.end()//
				.object()//
				.put("method", "GET").put("path", "/v1/data/message")//
				.object("parameters")//
				.put("refresh", true)//
				.end()//
				.end()//
				.object()//
				.put("method", "PUT").put("path", "/v1/data/message/1")//
				.object("content")//
				.put("code", "0")//
				.put("text", "Hi guys, what's up?")//
				.end()//
				.end()//
				.object()//
				.put("method", "PUT").put("path", "/v1/data/message/2")//
				.object("content")//
				.put("text", "Pretty cool, huhhhhh?")//
				.end()//
				.end()//
				.object()//
				.put("method", "GET").put("path", "/v1/data/message")//
				.object("parameters")//
				.put("refresh", true)//
				.end()//
				.end()//
				.build();

		SpaceResponse response = SpaceRequest.post("/v1/batch?debug=true").backendKey(testAccount).body(batch).go(200)//
				.assertEquals(201, "responses.0.status")//
				.assertEquals("1", "responses.0.id")//
				.assertEquals(201, "responses.1.status")//
				.assertEquals("2", "responses.1.id")//
				.assertEquals(200, "responses.2.status")//
				.assertEquals(2, "responses.2.content.total")//
				.assertEquals(200, "responses.3.status")//
				.assertEquals(200, "responses.4.status")//
				.assertEquals(200, "responses.5.status")//
				.assertEquals(2, "responses.5.content.total")//
				.assertEquals(1, "debug.batchCredentialChecks");

		assertEquals(Sets.newHashSet("Hi guys, what's up?", "Pretty cool, huhhhhh?"),
				Sets.newHashSet(response.getFromJson("responses.5.content.results.0.text").asText(),
						response.getFromJson("responses.5.content.results.1.text").asText()));

		assertEquals(Sets.newHashSet("0", "2"),
				Sets.newHashSet(response.getFromJson("responses.5.content.results.0.code").asText(),
						response.getFromJson("responses.5.content.results.1.code").asText()));

		// should succeed to stop on first batch request error

		batch = Json.arrayBuilder()//
				.object()//
				.put("method", "GET").put("path", "/v1/data/message")//
				.end()//
				.object()//
				.put("method", "GET").put("path", "/v1/data/XXX")//
				.end()//
				.object()//
				.put("method", "GET").put("path", "/v1/data/message")//
				.end()//
				.build();

		SpaceRequest.post("/v1/batch?stopOnError=true&debug=true").backendKey(testAccount).body(batch).go(200)//
				.assertEquals(200, "responses.0.status")//
				.assertEquals(404, "responses.1.status")//
				.assertSizeEquals(2, "responses")//
				.assertEquals(1, "debug.batchCredentialChecks");

		// should fail since batch are limited to 20 sub requests

		JsonBuilder<ArrayNode> bigBatch = Json.arrayBuilder();
		for (int i = 0; i < 11; i++)
			bigBatch.object().put("method", "GET").put("path", "/v1/login").end();

		SpaceRequest.post("/v1/batch?debug=true").backendKey(testAccount).body(bigBatch.build()).go(400)//
				.assertEquals("batch are limited to 10 sub requests", "error.message")//
				.assertEquals(0, "debug.batchCredentialChecks");

	}
}
