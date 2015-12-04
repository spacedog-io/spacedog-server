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
				.put("method", "GET")//
				.put("path", "/v1/admin/account/test")//
				.end()//
				.object()//
				.put("method", "DELETE")//
				.put("path", "/v1/admin/account/test")//
				.end()//
				.object()//
				.put("method", "POST")//
				.put("path", "/v1/admin/account")//
				.object("body")//
				.put("backendId", "test")//
				.put("username", "test")//
				.put("password", "hi test")//
				.put("email", "test@dog.com")//
				.end()//
				.end()//
				.object()//
				.put("method", "POST")//
				.put("path", "/v1/schema/message")//
				.node("body",
						SchemaBuilder2.builder("message", "code")//
								.stringProperty("code", true)//
								.textProperty("text", "english", true).toString())//
				.end()//
				.build();

		SpaceResponse spaceResponse = SpaceRequest.post("/v1/batch").basicAuth("test", "hi test").body(batch).go(200);

		int accountGetStatus = Json.get(spaceResponse.jsonNode(), "0.status").asInt();

		if (accountGetStatus == 200)
			spaceResponse.assertEquals(200, "0.status")//
					.assertEquals("test", "0.content.backendId")//
					.assertEquals(200, "1.status");
		else
			spaceResponse.assertEquals(404, "0.status")//
					.assertEquals(404, "1.status");

		spaceResponse.assertTrue("2.success")//
				.assertEquals("test", "2.id")//
				.assertEquals("account", "2.type")//
				.assertTrue("3.success")//
				.assertEquals("message", "3.id")//
				.assertEquals("schema", "3.type");

		io.spacedog.client.SpaceDogHelper.Account testAccount = new io.spacedog.client.SpaceDogHelper.Account(//
				"test", "test", "hi test", "test@dog.com", spaceResponse.getFromJson("2.backendKey").asText());

		// should succeed to create dave and vince users and fetch them with
		// simple backend key credentials

		batch = Json.arrayBuilder()//
				.object()//
				.put("method", "POST")//
				.put("path", "/v1/user")//
				.object("body")//
				.put("username", "vince")//
				.put("password", "hi vince")//
				.put("email", "vince@dog.com")//
				.end()//
				.end()//
				.object()//
				.put("method", "POST")//
				.put("path", "/v1/user")//
				.object("body")//
				.put("username", "dave")//
				.put("password", "hi dave")//
				.put("email", "dave@dog.com")//
				.end()//
				.end()//
				.object()//
				.put("method", "GET")//
				.put("path", "/v1/user/vince")//
				.end()//
				.object()//
				.put("method", "GET")//
				.put("path", "/v1/data/user/dave")//
				.end()//
				.build();

		SpaceRequest.post("/v1/batch").backendKey(testAccount).body(batch).go(200)//
				.assertEquals("vince", "0.id")//
				.assertEquals("dave", "1.id")//
				.assertEquals("vince", "2.content.username")//
				.assertEquals("dave", "3.content.username");

		// should succeed to returns errors when batch requests are invalid, not
		// found, unauthorized, ...

		batch = Json.arrayBuilder()//
				.object()//
				.put("method", "POST")//
				.put("path", "/v1/user")//
				.object("body")//
				.put("username", "fred")//
				.put("password", "hi fred")//
				.end()//
				.end()//
				.object()//
				.put("method", "GET")//
				.put("path", "/v1/toto")//
				.end()//
				.object()//
				.put("method", "GET")//
				.put("path", "/v1/user/vince")//
				.end()//
				.object()//
				.put("method", "POST")//
				.put("path", "/v1/user")//
				.end()//
				.object()//
				.put("method", "DELETE")//
				.put("path", "/v1/user")//
				.end()//
				.build();

		SpaceRequest.post("/v1/batch").backendKey(testAccount).body(batch).go(200)//
				.assertFalse("0.success")//
				.assertEquals(400, "0.status")//
				.assertFalse("1.success")//
				.assertEquals(404, "1.status")//
				.assertEquals("vince", "2.content.username")//
				.assertFalse("3.success")//
				.assertEquals(400, "3.status")//
				.assertFalse("4.success")//
				.assertEquals(401, "4.status");

		// should succeed to create and update messages by batch

		batch = Json.arrayBuilder()//
				.object()//
				.put("method", "POST")//
				.put("path", "/v1/data/message")//
				.object("body")//
				.put("code", "1")//
				.put("text", "Hi guys!")//
				.end()//
				.end()//
				.object()//
				.put("method", "POST")//
				.put("path", "/v1/data/message")//
				.object("body")//
				.put("code", "2")//
				.put("text", "Pretty cool, huhh?")//
				.end()//
				.end()//
				.object()//
				.put("method", "GET")//
				.put("path", "/v1/data/message")//
				.object("parameters")//
				.put("refresh", true)//
				.end()//
				.end()//
				.object()//
				.put("method", "PUT")//
				.put("path", "/v1/data/message/1")//
				.object("body")//
				.put("code", "0")//
				.put("text", "Hi guys, what's up?")//
				.end()//
				.end()//
				.object()//
				.put("method", "PUT")//
				.put("path", "/v1/data/message/2")//
				.object("body")//
				.put("text", "Pretty cool, huhhhhh?")//
				.end()//
				.end()//
				.object()//
				.put("method", "GET")//
				.put("path", "/v1/data/message")//
				.object("parameters")//
				.put("refresh", true)//
				.end()//
				.end()//
				.build();

		SpaceResponse response = SpaceRequest.post("/v1/batch").backendKey(testAccount).body(batch).go(200)//
				.assertEquals(201, "0.status")//
				.assertEquals("1", "0.id")//
				.assertEquals(201, "1.status")//
				.assertEquals("2", "1.id")//
				.assertEquals(200, "2.status")//
				.assertEquals(2, "2.content.total")//
				.assertEquals(200, "3.status")//
				.assertEquals(200, "4.status")//
				.assertEquals(200, "5.status")//
				.assertEquals(2, "5.content.total");

		assertEquals(Sets.newHashSet("Hi guys, what's up?", "Pretty cool, huhhhhh?"),
				Sets.newHashSet(response.getFromJson("5.content.results.0.text").asText(),
						response.getFromJson("5.content.results.1.text").asText()));

		assertEquals(Sets.newHashSet("0", "2"),
				Sets.newHashSet(response.getFromJson("5.content.results.0.code").asText(),
						response.getFromJson("5.content.results.1.code").asText()));

		// should succeed to stop on first batch request error

		batch = Json.arrayBuilder()//
				.object()//
				.put("method", "GET")//
				.put("path", "/v1/data/message")//
				.end()//
				.object()//
				.put("method", "GET")//
				.put("path", "/v1/data/XXX")//
				.end()//
				.object()//
				.put("method", "GET")//
				.put("path", "/v1/data/message")//
				.end()//
				.build();

		SpaceRequest.post("/v1/batch?stopOnError=true").backendKey(testAccount).body(batch).go(200)//
				.assertEquals(200, "0.status")//
				.assertEquals(404, "1.status")//
				.assertSizeEquals(2);
	}
}
