/**
 * © David Attias 2015
 */
package io.spacedog.test.bulk;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.bulk.ServiceCall;
import io.spacedog.client.bulk.ServiceResponse;
import io.spacedog.client.credentials.CredentialsCreateRequest;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.Roles;
import io.spacedog.client.data.DataSettings;
import io.spacedog.client.http.SpaceException;
import io.spacedog.client.http.SpaceMethod;
import io.spacedog.test.Message;
import io.spacedog.test.SpaceTest;
import io.spacedog.utils.Json;

public class BulkRestyTest extends SpaceTest {

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
		List<ServiceCall> bulk = Lists.newArrayList(//
				new ServiceCall(SpaceMethod.PUT, "/2/schemas/message")//
						.withPayload(Message.schema()),
				new ServiceCall(SpaceMethod.PUT, "/2/settings/data")//
						.withPayload(dataSettings),
				new ServiceCall(SpaceMethod.POST, "/2/credentials/_login"));

		List<ServiceResponse> responses = superadmin.bulk().execute(bulk);

		assertEquals("message", responses.get(0).content.get("id").asText());
		assertEquals("schemas", responses.get(0).content.get("type").asText());
		assertEquals("data", responses.get(1).content.get("id").asText());
		assertEquals("settings", responses.get(1).content.get("type").asText());
		assertEquals("superadmin", responses.get(2).content.get("credentials").get("username").asText());

		// assertEquals(Json.object("batchCredentialChecks", 1), response.debug());

		// should succeed to create dave and vince users and fetch them with
		// simple backend key credentials

		bulk = Lists.newArrayList();
		CredentialsCreateRequest ccr = new CredentialsCreateRequest()//
				.username("vince").password("hi vince").email("vince@dog.com");
		bulk.add(new ServiceCall(SpaceMethod.POST, "/2/credentials")//
				.withPayload(ccr));
		ccr = new CredentialsCreateRequest()//
				.username("dave").password("hi dave").email("dave@dog.com");
		bulk.add(new ServiceCall(SpaceMethod.POST, "/2/credentials")//
				.withPayload(ccr));

		responses = superadmin.bulk().execute(bulk);

		String vinceId = responses.get(0).content.get("id").asText();
		String daveId = responses.get(1).content.get("id").asText();

		// should succeed to fetch dave and vince credentials
		// and the message schema
		superadmin.get("/2/bulk")//
				.queryParam("vince", "/credentials/" + vinceId) //
				.queryParam("dave", "/credentials/" + daveId) //
				.queryParam("schema", "/schemas/message") //
				.go(200)//
				.assertEquals(vinceId, "vince.id")//
				.assertEquals("vince", "vince.username")//
				.assertEquals(daveId, "dave.id")//
				.assertEquals("dave", "dave.username")//
				.assertEquals("message", "schema.name")//
				.assertEquals("text", "schema.mapping.properties.text.type");

		// should succeed to return errors when batch requests are invalid, not
		// found, unauthorized, ...

		bulk = Lists.newArrayList();
		bulk.add(new ServiceCall(SpaceMethod.POST, "/2/credentials")//
				.withPayload(new CredentialsCreateRequest()//
						.username("fred").password("hi fred")));
		bulk.add(new ServiceCall(SpaceMethod.GET, "/2/toto"));
		bulk.add(new ServiceCall(SpaceMethod.DELETE, "/2/credentials/vince"));
		bulk.add(new ServiceCall(SpaceMethod.POST, "/2/credentials/vince/_set_password")//
				.withPayload(Json.object("password", "hi vince 2")));

		responses = guest.bulk().execute(bulk);

		assertEquals(400, responses.get(0).status);
		assertEquals(404, responses.get(1).status);
		assertEquals(401, responses.get(2).status);
		assertEquals(401, responses.get(3).status);

		// assertEquals(Json.object("batchCredentialChecks", 1), response.debug());

		// should succeed to create and update messages by batch

		bulk = Lists.newArrayList();
		bulk.add(new ServiceCall(SpaceMethod.PUT, "/2/data/message/1")//
				.withPayload(Json.object("text", "Hi guys!"))//
				.withParams("strict", true));

		bulk.add(new ServiceCall(SpaceMethod.PUT, "/2/data/message/2")//
				.withPayload(Json.object("text", "Pretty cool, huhh?"))//
				.withParams("strict", true));

		bulk.add(new ServiceCall(SpaceMethod.GET, "/2/data/message")//
				.withParams("refresh", true));

		bulk.add(new ServiceCall(SpaceMethod.PUT, "/2/data/message/1")//
				.withPayload(Json.object("text", "Hi guys, what's up?")));

		bulk.add(new ServiceCall(SpaceMethod.PUT, "/2/data/message/2")//
				.withPayload(Json.object("text", "Pretty cool, huhhhhh?")));

		bulk.add(new ServiceCall(SpaceMethod.GET, "/2/data/message")//
				.withParams("refresh", true));

		SpaceDog vince = SpaceDog.dog().username("vince").password("hi vince");

		responses = vince.bulk().execute(bulk);
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

		bulk = Lists.newArrayList();
		bulk.add(new ServiceCall(SpaceMethod.GET, "/2/data/message"));
		bulk.add(new ServiceCall(SpaceMethod.GET, "/2/data/XXX"));
		bulk.add(new ServiceCall(SpaceMethod.GET, "/2/data/message"));

		responses = vince.bulk().execute(bulk, true);
		assertEquals(2, responses.get(0).content.get("total").asLong());
		assertEquals(403, responses.get(1).status);
		assertEquals(2, responses.size());

		// assertEquals(Json.object("batchCredentialChecks", 1), response.debug());

		// should fail since batch are limited to 10 sub requests

		List<ServiceCall> bigBulk = Lists.newArrayList();
		for (int i = 0; i < 21; i++)
			bigBulk.add(new ServiceCall(SpaceMethod.GET, "/2/login"));

		SpaceException exc = assertHttpError(400, () -> guest.bulk().execute(bigBulk));
		assertEquals("bulk-limit-exceeded", exc.code());
	}
}
