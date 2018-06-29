/**
 * © David Attias 2015
 */
package io.spacedog.test;

import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.batch.ServiceCall;
import io.spacedog.client.credentials.CredentialsCreateRequest;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.Roles;
import io.spacedog.client.data.DataSettings;
import io.spacedog.client.http.SpaceMethod;
import io.spacedog.client.http.SpaceResponse;
import io.spacedog.utils.Json;

public class BatchRestyTest extends SpaceTest {

	@Test
	public void test() {

		// prepare
		prepareTest(true, true);
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearServer();
		superadmin.credentials().enableGuestSignUp(true);

		DataSettings dataSettings = new DataSettings();
		dataSettings.acl().put(Message.TYPE, Roles.user, //
				Permission.create, Permission.updateMine, Permission.search);

		// should succeed to reset test account and create message schema with
		// admin credentials
		List<ServiceCall> batch = Lists.newArrayList(//
				new ServiceCall(SpaceMethod.PUT, "/1/schemas/message")//
						.withPayload(Message.schema().mapping()),
				new ServiceCall(SpaceMethod.PUT, "/1/settings/data")//
						.withPayload(dataSettings),
				new ServiceCall(SpaceMethod.GET, "/1/login"));

		superadmin.batch().execute(batch)//
				.assertEquals("message", "responses.0.id")//
				.assertEquals("schemas", "responses.0.type")//
				.assertEquals("data", "responses.1.id")//
				.assertEquals("settings", "responses.1.type")//
				.assertEquals("superadmin", "responses.2.credentials.username")//
				.assertEquals(1, "debug.batchCredentialChecks");

		// should succeed to create dave and vince users and fetch them with
		// simple backend key credentials

		batch = Lists.newArrayList();
		CredentialsCreateRequest ccr = new CredentialsCreateRequest()//
				.username("vince").password("hi vince").email("vince@dog.com");
		batch.add(new ServiceCall(SpaceMethod.POST, "/1/credentials")//
				.withPayload(ccr));
		ccr = new CredentialsCreateRequest()//
				.username("dave").password("hi dave").email("dave@dog.com");
		batch.add(new ServiceCall(SpaceMethod.POST, "/1/credentials")//
				.withPayload(ccr));

		ObjectNode node = superadmin.batch().execute(batch).asJsonObject();

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
				.assertEquals("text", "schema.message.properties.text.type");

		// should succeed to return errors when batch requests are invalid, not
		// found, unauthorized, ...

		batch = Lists.newArrayList();
		batch.add(new ServiceCall(SpaceMethod.POST, "/1/credentials")//
				.withPayload(new CredentialsCreateRequest()//
						.username("fred").password("hi fred")));
		batch.add(new ServiceCall(SpaceMethod.GET, "/1/toto"));
		batch.add(new ServiceCall(SpaceMethod.DELETE, "/1/credentials/vince"));
		batch.add(new ServiceCall(SpaceMethod.POST, "/1/credentials/vince/_set_password")//
				.withPayload(Json.object("password", "hi vince 2")));

		guest.batch().execute(batch)//
				.assertEquals(400, "responses.0.status")//
				.assertEquals(404, "responses.1.status")//
				.assertEquals(401, "responses.2.status")//
				.assertEquals(401, "responses.3.status")//
				.assertEquals(1, "debug.batchCredentialChecks");

		// should succeed to create and update messages by batch

		batch = Lists.newArrayList();
		batch.add(new ServiceCall(SpaceMethod.PUT, "/1/data/message/1")//
				.withPayload(Json.object("text", "Hi guys!"))//
				.withParams("strict", true));

		batch.add(new ServiceCall(SpaceMethod.PUT, "/1/data/message/2")//
				.withPayload(Json.object("text", "Pretty cool, huhh?"))//
				.withParams("strict", true));

		batch.add(new ServiceCall(SpaceMethod.GET, "/1/data/message")//
				.withParams("refresh", true));

		batch.add(new ServiceCall(SpaceMethod.PUT, "/1/data/message/1")//
				.withPayload(Json.object("text", "Hi guys, what's up?")));

		batch.add(new ServiceCall(SpaceMethod.PUT, "/1/data/message/2")//
				.withPayload(Json.object("text", "Pretty cool, huhhhhh?")));

		batch.add(new ServiceCall(SpaceMethod.GET, "/1/data/message")//
				.withParams("refresh", true));

		SpaceDog vince = SpaceDog.dog().username("vince").password("hi vince");

		SpaceResponse response = vince.batch().execute(batch)
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
				Sets.newHashSet(response.getString("responses.5.objects.0.source.text"),
						response.getString("responses.5.objects.1.source.text")));

		assertEquals(Sets.newHashSet("1", "2"), //
				Sets.newHashSet(response.getString("responses.5.objects.0.id"),
						response.getString("responses.5.objects.1.id")));

		// should succeed to stop on first batch request error

		batch = Lists.newArrayList();
		batch.add(new ServiceCall(SpaceMethod.GET, "/1/data/message"));
		batch.add(new ServiceCall(SpaceMethod.GET, "/1/data/XXX"));
		batch.add(new ServiceCall(SpaceMethod.GET, "/1/data/message"));

		vince.batch().execute(batch, true)//
				.assertEquals(2, "responses.0.total")//
				.assertEquals(403, "responses.1.status")//
				.assertSizeEquals(2, "responses")//
				.assertEquals(1, "debug.batchCredentialChecks");

		// should fail since batch are limited to 10 sub requests

		List<ServiceCall> bigBatch = Lists.newArrayList();
		for (int i = 0; i < 21; i++)
			bigBatch.add(new ServiceCall(SpaceMethod.GET, "/1/login"));

		assertHttpError(400, () -> guest.batch().execute(bigBatch))//
				.spaceResponse()//
				.assertEquals("batch-limit-exceeded", "error.code");
	}
}
