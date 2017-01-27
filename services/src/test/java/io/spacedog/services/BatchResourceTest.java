/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;

import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceResponse;
import io.spacedog.client.SpaceTest;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.Json;
import io.spacedog.utils.Schema;

public class BatchResourceTest extends SpaceTest {

	@Test
	public void executeBatch() {

		// we need to make sure the test account is reset and exists before to
		// be able to reset it again by batch requests

		prepareTest();
		resetTestBackend();

		// should succeed to reset test account and create message schema with
		// admin credentials

		ArrayNode batch = Json.arrayBuilder()//
				.object()//
				.put("method", "DELETE").put("path", "/1/backend")//
				.end()//

				.object()//
				.put("method", "POST").put("path", "/1/backend/test")//
				.object("parameters")//
				.put("notif", false)//
				.end()//
				.object("content")//
				.put("username", "test")//
				.put("password", "hi test")//
				.put("email", "test@dog.com")//
				.end()//
				.end()//

				.object()//
				.put("method", "GET").put("path", "/1/backend")//
				.end()//

				.object()//
				.put("method", "POST").put("path", "/1/schema/message")//
				.node("content",
						Schema.builder("message").id("code")//
								.string("code").text("text").toString())//
				.end()//

				.object()//
				.put("method", "GET").put("path", "/1/login")//
				.end()//
				.build();

		SpaceRequest.post("/1/batch").debugServer().superdogAuth("test").body(batch).go(200)//
				.assertEquals(201, "responses.1.status")//
				.assertEquals("test", "responses.1.id")//
				.assertEquals(200, "responses.2.status")//
				.assertEquals("test", "responses.2.content.results.0.backendId")//
				.assertEquals("test", "responses.2.content.results.0.username")//
				.assertEquals("test@dog.com", "responses.2.content.results.0.email")//
				.assertEquals(201, "responses.3.status")//
				.assertEquals("message", "responses.3.id")//
				.assertEquals("schema", "responses.3.type")//
				.assertEquals(200, "responses.4.status")//
				.assertEquals(1, "debug.batchCredentialChecks");

		SpaceDog test = SpaceDog.backend("test").username("test").password("hi test").email("test@dog.com");

		// should succeed to create dave and vince users and fetch them with
		// simple backend key credentials

		batch = Json.arrayBuilder()//
				.object()//
				.put("method", "POST").put("path", "/1/credentials")//
				.object("content")//
				.put("username", "vince")//
				.put("password", "hi vince")//
				.put("email", "vince@dog.com")//
				.end()//
				.end()//

				.object()//
				.put("method", "POST").put("path", "/1/credentials")//
				.object("content")//
				.put("username", "dave")//
				.put("password", "hi dave")//
				.put("email", "dave@dog.com")//
				.end()//
				.end()//

				.build();

		ObjectNode node = SpaceRequest.post("/1/batch")//
				.debugServer().adminAuth(test).body(batch).go(200)//
				.objectNode();

		String vinceId = Json.get(node, "responses.0.id").asText();
		String daveId = Json.get(node, "responses.1.id").asText();

		// should succeed to fetch dave and vince credentials
		// and the message schema
		SpaceRequest.get("/1/batch").adminAuth(test)//
				.queryParam("vince", "/credentials/" + vinceId) //
				.queryParam("dave", "/credentials/" + daveId) //
				.queryParam("schema", "/schema/message") //
				.go(200)//
				.assertEquals(vinceId, "vince.id")//
				.assertEquals("vince", "vince.username")//
				.assertEquals(daveId, "dave.id")//
				.assertEquals("dave", "dave.username")//
				.assertEquals("string", "schema.message.code._type");

		// should succeed to return errors when batch requests are invalid, not
		// found, unauthorized, ...

		batch = Json.arrayBuilder()//
				.object()//
				.put("method", "POST").put("path", "/1/credentials")//
				.object("content")//
				.put("username", "fred")//
				.put("password", "hi fred")//
				.end()//
				.end()//

				.object()//
				.put("method", "GET").put("path", "/1/toto")//
				.end()//

				.object()//
				.put("method", "DELETE").put("path", "/1/credentials/vince")//
				.end()//

				.object()//
				.put("method", "PUT").put("path", "/1/credentials/vince/password")//
				.put("content", "hi vince 2")//
				.end()//
				.build();

		SpaceRequest.post("/1/batch").debugServer().backend(test).body(batch).go(200)//
				.assertEquals(400, "responses.0.status")//
				.assertEquals(404, "responses.1.status")//
				.assertEquals(403, "responses.2.status")//
				.assertEquals(403, "responses.3.status")//
				.assertEquals(1, "debug.batchCredentialChecks");

		// should succeed to create and update messages by batch

		batch = Json.arrayBuilder()//
				.object()//
				.put("method", "POST").put("path", "/1/data/message")//
				.object("content")//
				.put("code", "1")//
				.put("text", "Hi guys!")//
				.end()//
				.end()//

				.object()//
				.put("method", "POST").put("path", "/1/data/message")//
				.object("content")//
				.put("code", "2")//
				.put("text", "Pretty cool, huhh?")//
				.end()//
				.end()//

				.object()//
				.put("method", "GET").put("path", "/1/data/message")//
				.object("parameters")//
				.put("refresh", true)//
				.end()//
				.end()//

				.object()//
				.put("method", "PUT").put("path", "/1/data/message/1")//
				.object("content")//
				.put("code", "0")//
				.put("text", "Hi guys, what's up?")//
				.end()//
				.end()//

				.object()//
				.put("method", "PUT").put("path", "/1/data/message/2")//
				.object("content")//
				.put("text", "Pretty cool, huhhhhh?")//
				.end()//
				.end()//

				.object()//
				.put("method", "GET").put("path", "/1/data/message")//
				.object("parameters")//
				.put("refresh", true)//
				.end()//
				.end()//
				.build();

		SpaceResponse response = SpaceRequest.post("/1/batch")//
				.debugServer().basicAuth("test", "vince", "hi vince").body(batch).go(200)//
				.assertEquals(201, "responses.0.status")//
				.assertEquals("1", "responses.0.id")//
				.assertEquals(201, "responses.1.status")//
				.assertEquals("2", "responses.1.id")//
				.assertEquals(200, "responses.2.status")//
				.assertEquals(2, "responses.2.content.total")//
				.assertEquals(400, "responses.3.status")//
				.assertEquals(200, "responses.4.status")//
				.assertEquals(200, "responses.5.status")//
				.assertEquals(2, "responses.5.content.total")//
				.assertEquals(1, "debug.batchCredentialChecks");

		assertEquals(Sets.newHashSet("Hi guys!", "Pretty cool, huhhhhh?"),
				Sets.newHashSet(response.getString("responses.5.content.results.0.text"),
						response.getString("responses.5.content.results.1.text")));

		assertEquals(Sets.newHashSet("1", "2"),
				Sets.newHashSet(response.getString("responses.5.content.results.0.code"),
						response.getString("responses.5.content.results.1.code")));

		// should succeed to stop on first batch request error

		batch = Json.arrayBuilder()//
				.object()//
				.put("method", "GET").put("path", "/1/data/message")//
				.end()//

				.object()//
				.put("method", "GET").put("path", "/1/data/XXX")//
				.end()//

				.object()//
				.put("method", "GET").put("path", "/1/data/message")//
				.end()//
				.build();

		SpaceRequest.post("/1/batch?stopOnError=true")//
				.debugServer().basicAuth("test", "vince", "hi vince").body(batch).go(200)//
				.assertEquals(200, "responses.0.status")//
				.assertEquals(404, "responses.1.status")//
				.assertSizeEquals(2, "responses")//
				.assertEquals(1, "debug.batchCredentialChecks");

		// should fail since batch are limited to 10 sub requests

		ArrayNode bigBatch = Json.array();
		for (int i = 0; i < 11; i++)
			bigBatch.add(Json.object("method", "GET", "path", "/1/login"));

		SpaceRequest.post("/1/batch").backend(test).body(bigBatch).go(400)//
				.assertEquals("batch-limit-exceeded", "error.code");
	}
}
