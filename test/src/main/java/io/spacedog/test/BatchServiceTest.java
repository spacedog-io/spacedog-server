/**
 * © David Attias 2015
 */
package io.spacedog.test;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.Roles;
import io.spacedog.client.http.SpaceRequest;
import io.spacedog.client.http.SpaceResponse;
import io.spacedog.model.Schema;
import io.spacedog.utils.Json;

public class BatchServiceTest extends SpaceTest {

	@Test
	public void executeBatch() {

		// we need to make sure the test account is reset and exists before to
		// be able to reset it again by batch requests
		prepareTest();
		SpaceDog superadmin = clearRootBackend();
		superadmin.credentials().enableGuestSignUp(true);
		Schema schema = Schema.builder("message").text("text")//
				.acl(Roles.user, Permission.create, Permission.updateMine, Permission.search).build();

		// should succeed to reset test account and create message schema with
		// admin credentials
		ArrayNode batch = Json.builder().array()//
				.object()//
				.add("method", "PUT").add("path", "/1/schemas/message")//
				.add("content", schema.node())//
				.end()//

				.object()//
				.add("method", "GET").add("path", "/1/login")//
				.end()//
				.build();

		superadmin.post("/1/batch").debugServer().bodyJson(batch).go(200)//
				.assertEquals("message", "responses.0.id")//
				.assertEquals("schema", "responses.0.type")//
				.assertEquals("superadmin", "responses.1.credentials.username")//
				.assertEquals(1, "debug.batchCredentialChecks");

		// should succeed to create dave and vince users and fetch them with
		// simple backend key credentials

		batch = Json.builder().array()//
				.object()//
				.add("method", "POST").add("path", "/1/credentials")//
				.object("content")//
				.add("username", "vince")//
				.add("password", "hi vince")//
				.add("email", "vince@dog.com")//
				.end()//
				.end()//

				.object()//
				.add("method", "POST").add("path", "/1/credentials")//
				.object("content")//
				.add("username", "dave")//
				.add("password", "hi dave")//
				.add("email", "dave@dog.com")//
				.end()//
				.end()//

				.build();

		ObjectNode node = superadmin.post("/1/batch")//
				.debugServer().bodyJson(batch).go(200).asJsonObject();

		String vinceId = Json.get(node, "responses.0.id").asText();
		String daveId = Json.get(node, "responses.1.id").asText();

		// should succeed to fetch dave and vince credentials
		// and the message schema
		superadmin.get("/1/batch")//
				.queryParam("vince", "/credentials/" + vinceId) //
				.queryParam("dave", "/credentials/" + daveId) //
				.queryParam("schema", "/schemas/message") //
				.go(200)//
				.assertEquals(vinceId, "vince.id")//
				.assertEquals("vince", "vince.username")//
				.assertEquals(daveId, "dave.id")//
				.assertEquals("dave", "dave.username")//
				.assertEquals("text", "schema.message.text._type");

		// should succeed to return errors when batch requests are invalid, not
		// found, unauthorized, ...

		batch = Json.builder().array()//
				.object()//
				.add("method", "POST").add("path", "/1/credentials")//
				.object("content")//
				.add("username", "fred")//
				.add("password", "hi fred")//
				.end()//
				.end()//

				.object()//
				.add("method", "GET").add("path", "/1/toto")//
				.end()//

				.object()//
				.add("method", "DELETE").add("path", "/1/credentials/vince")//
				.end()//

				.object()//
				.add("method", "PUT").add("path", "/1/credentials/vince/password")//
				.add("content", "hi vince 2")//
				.end()//
				.build();

		SpaceRequest.post("/1/batch").debugServer()//
				.backend(superadmin).bodyJson(batch).go(200)//
				.assertEquals(400, "responses.0.status")//
				.assertEquals(404, "responses.1.status")//
				.assertEquals(403, "responses.2.status")//
				.assertEquals(403, "responses.3.status")//
				.assertEquals(1, "debug.batchCredentialChecks");

		// should succeed to create and update messages by batch

		batch = Json.builder().array()//
				.object()//
				.add("method", "PUT").add("path", "/1/data/message/1")//
				.object("content")//
				.add("text", "Hi guys!")//
				.end()//
				.object("parameters")//
				.add("strict", true)//
				.end()//
				.end()//

				.object()//
				.add("method", "PUT").add("path", "/1/data/message/2")//
				.object("content")//
				.add("text", "Pretty cool, huhh?")//
				.end()//
				.object("parameters")//
				.add("strict", true)//
				.end()//
				.end()//

				.object()//
				.add("method", "GET").add("path", "/1/data/message")//
				.object("parameters")//
				.add("refresh", true)//
				.end()//
				.end()//

				.object()//
				.add("method", "PUT").add("path", "/1/data/message/1")//
				.object("content")//
				.add("text", "Hi guys, what's up?")//
				.end()//
				.end()//

				.object()//
				.add("method", "PUT").add("path", "/1/data/message/2")//
				.object("content")//
				.add("text", "Pretty cool, huhhhhh?")//
				.end()//
				.end()//

				.object()//
				.add("method", "GET").add("path", "/1/data/message")//
				.object("parameters")//
				.add("refresh", true)//
				.end()//
				.end()//
				.build();

		SpaceResponse response = SpaceRequest.post("/1/batch")//
				.debugServer().backend(superadmin)//
				.basicAuth("vince", "hi vince")//
				.bodyJson(batch).go(200)//
				// .assertEquals(201, "responses.0.status")//
				.assertEquals("1", "responses.0.id")//
				.assertEquals(1, "responses.0.version")//
				// .assertEquals(201, "responses.1.status")//
				.assertEquals("2", "responses.1.id")//
				.assertEquals(1, "responses.1.version")//
				// .assertEquals(200, "responses.2.status")//
				.assertEquals(2, "responses.2.total")//
				// .assertEquals(200, "responses.3.status")//
				.assertEquals("1", "responses.3.id")//
				.assertEquals(2, "responses.3.version")//
				// .assertEquals(200, "responses.4.status")//
				.assertEquals("2", "responses.4.id")//
				.assertEquals(2, "responses.4.version")//
				// .assertEquals(200, "responses.5.status")//
				.assertEquals(2, "responses.5.total")//
				.assertEquals(1, "debug.batchCredentialChecks");

		assertEquals(Sets.newHashSet("Hi guys, what's up?", "Pretty cool, huhhhhh?"), //
				Sets.newHashSet(response.getString("responses.5.results.0.source.text"),
						response.getString("responses.5.results.1.source.text")));

		assertEquals(Sets.newHashSet("1", "2"), //
				Sets.newHashSet(response.getString("responses.5.results.0.id"),
						response.getString("responses.5.results.1.id")));

		// should succeed to stop on first batch request error

		batch = Json.builder().array()//
				.object()//
				.add("method", "GET").add("path", "/1/data/message")//
				.end()//

				.object()//
				.add("method", "GET").add("path", "/1/data/XXX")//
				.end()//

				.object()//
				.add("method", "GET").add("path", "/1/data/message")//
				.end()//
				.build();

		SpaceRequest.post("/1/batch").debugServer()//
				.queryParam("stopOnError", true).backend(superadmin)//
				.basicAuth("vince", "hi vince").bodyJson(batch).go(200)//
				.assertEquals(2, "responses.0.total")//
				.assertEquals(403, "responses.1.status")//
				.assertSizeEquals(2, "responses")//
				.assertEquals(1, "debug.batchCredentialChecks");

		// should fail since batch are limited to 10 sub requests

		ArrayNode bigBatch = Json.array();
		for (int i = 0; i < 11; i++)
			bigBatch.add(Json.object("method", "GET", "path", "/1/login"));

		SpaceRequest.post("/1/batch").backend(superadmin).bodyJson(bigBatch).go(400)//
				.assertEquals("batch-limit-exceeded", "error.code");
	}
}
