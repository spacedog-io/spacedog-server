/**
 * © David Attias 2015
 */
package io.spacedog.test.batch;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.batch.ServiceResponse;
import io.spacedog.client.batch.ServiceCall;
import io.spacedog.client.credentials.CredentialsCreateRequest;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.Roles;
import io.spacedog.client.data.DataSettings;
import io.spacedog.client.http.SpaceMethod;
import io.spacedog.test.Message;
import io.spacedog.test.SpaceTest;
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

		List<ServiceResponse> responses = superadmin.batch().execute(batch);

		assertEquals("message", responses.get(0).content.get("id").asText());
		assertEquals("schemas", responses.get(0).content.get("type").asText());
		assertEquals("data", responses.get(1).content.get("id").asText());
		assertEquals("settings", responses.get(1).content.get("type").asText());
		assertEquals("superadmin", responses.get(2).content.get("credentials").get("username").asText());

		// assertEquals(Json.object("batchCredentialChecks", 1), response.debug());

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

		responses = superadmin.batch().execute(batch);

		String vinceId = responses.get(0).content.get("id").asText();
		String daveId = responses.get(1).content.get("id").asText();

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

		responses = guest.batch().execute(batch);

		assertEquals(400, responses.get(0).status);
		assertEquals(404, responses.get(1).status);
		assertEquals(401, responses.get(2).status);
		assertEquals(401, responses.get(3).status);

		// assertEquals(Json.object("batchCredentialChecks", 1), response.debug());

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

		responses = vince.batch().execute(batch);
		assertEquals("1", responses.get(0).content.get("id").asText());
		assertEquals(1, responses.get(0).content.get("version").asLong());
		assertEquals("2", responses.get(1).content.get("id").asText());
		assertEquals(1, responses.get(1).content.get("version").asLong());
		assertEquals(2, responses.get(2).content.get("total").asLong());
		assertEquals("1", responses.get(3).content.get("id").asText());
		assertEquals(2, responses.get(3).content.get("version").asLong());
		assertEquals("2", responses.get(4).content.get("id").asText());
		assertEquals(2, responses.get(4).content.get("version").asLong());
		assertEquals(2, responses.get(5).content.get("total").asLong());

		// assertEquals(Json.object("batchCredentialChecks", 1), response.debug());

		assertEquals(Sets.newHashSet("Hi guys, what's up?", "Pretty cool, huhhhhh?"), //
				Sets.newHashSet(Json.get(responses.get(5).content, "objects.0.source.text").asText(),
						Json.get(responses.get(5).content, "objects.1.source.text").asText()));

		assertEquals(Sets.newHashSet("1", "2"), //
				Sets.newHashSet(Json.get(responses.get(5).content, "objects.0.id").asText(),
						Json.get(responses.get(5).content, "objects.1.id").asText()));

		// should succeed to stop on first batch request error

		batch = Lists.newArrayList();
		batch.add(new ServiceCall(SpaceMethod.GET, "/1/data/message"));
		batch.add(new ServiceCall(SpaceMethod.GET, "/1/data/XXX"));
		batch.add(new ServiceCall(SpaceMethod.GET, "/1/data/message"));

		responses = vince.batch().execute(batch, true);
		assertEquals(2, responses.get(0).content.get("total").asLong());
		assertEquals(403, responses.get(1).status);
		assertEquals(2, responses.size());

		// assertEquals(Json.object("batchCredentialChecks", 1), response.debug());

		// should fail since batch are limited to 10 sub requests

		List<ServiceCall> bigBatch = Lists.newArrayList();
		for (int i = 0; i < 21; i++)
			bigBatch.add(new ServiceCall(SpaceMethod.GET, "/1/login"));

		assertHttpError(400, () -> guest.batch().execute(bigBatch))//
				.spaceResponse()//
				.assertEquals("batch-limit-exceeded", "error.code");
	}
}
