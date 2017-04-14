/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.model.DataPermission;
import io.spacedog.model.Schema;
import io.spacedog.model.Schema.SchemaAcl;
import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.Json7;
import io.spacedog.utils.JsonBuilder;

public class PushResourceTestOften extends SpaceTest {

	private static final String PUSHED_TO = "pushedTo";
	private static final String FAILURES = "failures";
	private static final String USERS_ONLY = "usersOnly";
	private static final String ID = "id";
	private static final String INSTALLATION_ID = "installationId";
	private static final String MESSAGE = "message";
	private static final String APNS = "APNS";
	private static final String GCM = "GCM";
	private static final String VALUE = "value";
	private static final String KEY = "key";
	private static final String TAGS = "tags";
	private static final String USER_ID = "userId";
	private static final String ENDPOINT = "endpoint";
	private static final String TOKEN = "token";
	private static final String BADGE = "badge";
	private static final String BADGE_STRATEGY = "badgeStrategy";
	private static final String PUSH_SERVICE = "pushService";
	private static final String APP_ID = "appId";

	@Test
	public void usersInstallAppAndPush() {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();

		// prepare users
		SpaceDog dave = signUp(test, "dave", "hi dave");
		SpaceDog vince = signUp(test, "vince", "hi vince");
		SpaceDog fred = signUp(test, "fred", "hi fred");
		SpaceDog nath = signUp(test, "nath", "hi nath");

		// prepare installation schema
		test.schema().setDefault("installation");

		// add create permission to guest requests
		Schema schema = test.schema().get("installation");
		schema.acl(new SchemaAcl()//
				.set("key", DataPermission.create, DataPermission.update)//
				.set("user", DataPermission.create, DataPermission.read, DataPermission.update)//
				.set("admin", DataPermission.create, DataPermission.update_all, //
						DataPermission.search, DataPermission.delete_all));
		test.schema().set(schema);

		// non authenticated user installs joho
		// and fails to set installation userId and endpoint fields
		String unknownInstallId = SpaceRequest.post("/1/installation")//
				.backend(test)//
				.bodyJson(TOKEN, "token-unknown", APP_ID, "joho", PUSH_SERVICE, GCM, //
						USER_ID, "XXX", ENDPOINT, "XXX")//
				.go(201)//
				.objectNode().get(ID).asText();

		SpaceRequest.get("/1/installation/" + unknownInstallId).auth(test).go(200)//
				.assertEquals("joho", APP_ID)//
				.assertEquals(GCM, PUSH_SERVICE)//
				.assertEquals("token-unknown", TOKEN)//
				.assertEquals("FAKE_ENDPOINT_FOR_TESTING", ENDPOINT)//
				.assertNotPresent(USER_ID)//
				.assertNotPresent(TAGS);

		// vince and fred install joho
		String vinceInstallId = installApplication("joho", GCM, test, vince);
		String fredInstallId = installApplication("joho", APNS, test, fred);
		String daveInstallId = installApplication("joho", APNS, test, dave);

		// vince pushes a simple message to fred
		SpaceRequest.post("/1/installation/" + fredInstallId + "/push").auth(vince).bodyJson(MESSAGE, "coucou").go(200)//
				.assertEquals(fredInstallId, "pushedTo.0.installationId")//
				.assertEquals(fred.username(), "pushedTo.0.userId");

		// vince pushes a complex object message to dave
		SpaceRequest.post("/1/installation/" + daveInstallId + "/push").auth(vince)//
				.bodyJson(MESSAGE,
						Json7.object("APNS",
								Json7.object("aps", //
										Json7.object("alert", "coucou"))))//
				.go(200);

		// vince fails to push to invalid installation id
		SpaceRequest.post("/1/installation/XXX/push").auth(vince).bodyJson(MESSAGE, "coucou").go(404);

		// nath installs birdee
		String nathInstallId = installApplication("birdee", APNS, test, nath);

		// vince updates its installation
		SpaceRequest.put("/1/installation/" + vinceInstallId)//
		.backend(test).auth(vince)//
				.bodyJson(TOKEN, "super-token-vince", APP_ID, "joho", PUSH_SERVICE, GCM)//
				.go(200);

		SpaceRequest.get("/1/installation/" + vinceInstallId)//
		.backend(test).auth(vince).go(200)//
				.assertEquals("joho", APP_ID)//
				.assertEquals("super-token-vince", TOKEN)//
				.assertEquals("vince", USER_ID)//
				.assertNotPresent(TAGS);

		// vince fails to get all installations since not admin
		SpaceRequest.get("/1/installation")//
		.backend(test).auth(vince)//
				.go(403);

		// admin gets all installations
		SpaceRequest.get("/1/installation").refresh().auth(test).go(200)//
				.assertSizeEquals(5, "results")//
				.assertContainsValue(unknownInstallId, ID)//
				.assertContainsValue(daveInstallId, ID)//
				.assertContainsValue(vinceInstallId, ID)//
				.assertContainsValue(fredInstallId, ID)//
				.assertContainsValue(nathInstallId, ID);

		// nath adds bonjour/toi tag to her install
		SpaceRequest.post("/1/installation/" + nathInstallId + "/tags")//
		.backend(test).auth(nath)//
				.bodyJson(toJsonTag("bonjour", "toi")).go(200);

		SpaceRequest.get("/1/installation/" + nathInstallId + "/tags")//
		.backend(test).auth(nath).go(200)//
				.assertContains(toJsonTag("bonjour", "toi"))//
				.assertSizeEquals(1);

		// nath adds again the same tag and it changes nothing
		// there is no duplicate as a result
		SpaceRequest.post("/1/installation/" + nathInstallId + "/tags")//
		.backend(test).auth(nath)//
				.bodyJson(toJsonTag("bonjour", "toi")).go(200);

		SpaceRequest.get("/1/installation/" + nathInstallId + "/tags")//
		.backend(test).auth(nath).go(200)//
				.assertContains(toJsonTag("bonjour", "toi"))//
				.assertSizeEquals(1);

		// vince adds bonjour/toi tag to his install
		SpaceRequest.post("/1/installation/" + vinceInstallId + "/tags")//
		.backend(test).auth(vince)//
				.bodyJson(toJsonTag("bonjour", "toi")).go(200);

		SpaceRequest.get("/1/installation/" + vinceInstallId + "/tags")//
		.backend(test).auth(vince).go(200)//
				.assertContains(toJsonTag("bonjour", "toi"))//
				.assertSizeEquals(1);

		// vince adds hi/there tag to his install
		SpaceRequest.post("/1/installation/" + vinceInstallId + "/tags")//
		.backend(test).auth(vince)//
				.bodyJson(toJsonTag("hi", "there")).go(200);

		SpaceRequest.get("/1/installation/" + vinceInstallId + "/tags")//
		.backend(test).auth(vince).go(200)//
				.assertContains(toJsonTag("bonjour", "toi"))//
				.assertContains(toJsonTag("hi", "there"))//
				.assertSizeEquals(2);

		// vince deletes bonjour/toi tag from his install
		SpaceRequest.delete("/1/installation/" + vinceInstallId + "/tags")//
		.backend(test).auth(vince)//
				.bodyJson(toJsonTag("bonjour", "toi")).go(200);

		SpaceRequest.get("/1/installation/" + vinceInstallId + "/tags")//
		.backend(test).auth(vince).go(200)//
				.assertContains(toJsonTag("hi", "there"))//
				.assertSizeEquals(1);

		// vince deletes hi/there tag from his install
		SpaceRequest.delete("/1/installation/" + vinceInstallId + "/tags")//
		.backend(test).auth(vince)//
				.bodyJson(toJsonTag("hi", "there")).go(200);

		SpaceRequest.get("/1/installation/" + vinceInstallId + "/tags")//
		.backend(test).auth(vince).go(200)//
				.assertSizeEquals(0);

		// vince sets all his install tags to bonjour/toi and hi/there
		SpaceRequest.put("/1/installation/" + vinceInstallId + "/tags")//
		.backend(test).auth(vince)//
				.bodyJson(toJsonTags("hi", "there", "bonjour", "toi")).go(200);

		SpaceRequest.get("/1/installation/" + vinceInstallId + "/tags")//
		.backend(test).auth(vince).go(200)//
				.assertContains(toJsonTag("bonjour", "toi"))//
				.assertContains(toJsonTag("hi", "there"))//
				.assertSizeEquals(2);

		// fred sets all his install tags to bonjour/toi
		SpaceRequest.put("/1/installation/" + fredInstallId + "/tags")//
		.backend(test).auth(fred)//
				.bodyJson(toJsonTags("bonjour", "toi")).go(200);

		SpaceRequest.get("/1/installation/" + fredInstallId + "/tags")//
		.backend(test).auth(fred).go(200)//
				.assertContains(toJsonTag("bonjour", "toi"))//
				.assertSizeEquals(1);

		// vince pushes to all joho installations
		// this means users and anonymous installations
		ObjectNode push = Json7.object(APP_ID, "joho", MESSAGE, "This is a push!");
		SpaceRequest.post("/1/push").refresh().auth(vince).bodyJson(push).go(200)//
				.assertFalse(FAILURES)//
				.assertSizeEquals(4, PUSHED_TO)//
				.assertContainsValue(unknownInstallId, INSTALLATION_ID)//
				.assertContainsValue("dave", USER_ID)//
				.assertContainsValue("vince", USER_ID)//
				.assertContainsValue("fred", USER_ID);

		// vince pushes to all joho users
		// this means excluding anonymous installations
		push.put(USERS_ONLY, true);
		SpaceRequest.post("/1/push").refresh().auth(vince).bodyJson(push).go(200)//
				.assertFalse(FAILURES)//
				.assertSizeEquals(3, PUSHED_TO)//
				.assertContainsValue("dave", USER_ID)//
				.assertContainsValue("vince", USER_ID)//
				.assertContainsValue("fred", USER_ID);

		// vince pushes to APNS only joho users
		push.put(PUSH_SERVICE, APNS);
		SpaceRequest.post("/1/push").refresh().auth(vince).bodyJson(push).go(200)//
				.assertFalse(FAILURES)//
				.assertSizeEquals(2, PUSHED_TO)//
				.assertContainsValue("dave", USER_ID)//
				.assertContainsValue("fred", USER_ID);

		// vince pushes to APNS only joho users with tag bonjour/toi
		push.set(TAGS, toJsonTag("bonjour", "toi"));
		SpaceRequest.post("/1/push").auth(vince).bodyJson(push).go(200)//
				.assertFalse(FAILURES)//
				.assertSizeEquals(1, PUSHED_TO)//
				.assertContainsValue("fred", USER_ID);

		// vince pushes to all joho users with tag bonjour/toi
		push.remove(PUSH_SERVICE);
		SpaceRequest.post("/1/push").auth(vince).bodyJson(push).go(200)//
				.assertFalse(FAILURES)//
				.assertSizeEquals(2, PUSHED_TO)//
				.assertContainsValue("vince", USER_ID)//
				.assertContainsValue("fred", USER_ID);

		// vince pushes to all joho users with tags bonjour/toi and hi/there
		push.set(TAGS, toJsonTags("bonjour", "toi", "hi", "there"));
		SpaceRequest.post("/1/push").auth(vince).bodyJson(push).go(200)//
				.assertFalse(FAILURES)//
				.assertSizeEquals(1, PUSHED_TO)//
				.assertContainsValue("vince", USER_ID);

		// vince gets 404 when he pushes to invalid app id
		push = Json7.object(APP_ID, "XXX", MESSAGE, "This is a push!");
		SpaceRequest.post("/1/push").auth(vince).bodyJson(push).go(404);

		// vince can not read, update nor delete dave's installation
		SpaceRequest.get("/1/installation/" + daveInstallId).auth(vince).go(403);

		SpaceRequest.put("/1/installation/" + daveInstallId).auth(vince).bodyJson(BADGE, 2).go(403);

		// also true with /data/installation route
		SpaceRequest.get("/1/data/installation/" + daveInstallId).auth(vince).go(403);

		SpaceRequest.put("/1/data/installation/" + daveInstallId).auth(vince).bodyJson(BADGE, 2).go(403);

		// dave can not update his installation app id
		// if he does not provide the token
		// this is true for push service and endpoint
		SpaceRequest.put("/1/installation/" + daveInstallId).auth(dave).bodyJson(APP_ID, "joho2").go(400);
	}

	@Test
	public void badgeManagement() {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();

		// prepare users
		SpaceDog dave = signUp(test, "dave", "hi dave");
		SpaceDog vince = signUp(test, "vince", "hi vince");

		// prepare installation schema
		test.schema().setDefault("installation");

		// vince and dave install joho
		String vinceInstallId = installApplication("joho", APNS, test, vince);
		String daveInstallId = installApplication("joho", APNS, test, dave);

		// vince pushes a message to dave with manual badge = 3
		SpaceRequest.post("/1/installation/" + daveInstallId + "/push").auth(vince)//
				.bodyJson(MESSAGE,
						Json7.object(APNS, //
								Json7.object("aps", //
										Json7.object("alert", "coucou", BADGE, 3))))//
				.go(200)//
				.assertEquals(daveInstallId, "pushedTo.0.installationId");

		// badge is not set in installation
		// since badge management is still manual
		SpaceRequest.get("/1/installation/" + daveInstallId).auth(dave).go(200)//
				.assertNotPresent(BADGE);

		// vince pushes a message to all with automatic badging
		// this means installation.badge is incremented on the server
		// and installation.badge is sent to each installation
		ObjectNode push = Json7.object(APP_ID, "joho", BADGE_STRATEGY, "auto", //
				MESSAGE, "Badge is the new trend!");

		SpaceRequest.post("/1/push")//
		.refresh().auth(test)//
				.bodyJson(push).go(200)//
				.assertSizeEquals(2, PUSHED_TO)//
				.assertContainsValue(vinceInstallId, INSTALLATION_ID)//
				.assertContainsValue(daveInstallId, INSTALLATION_ID);

		// check badge is 1 in dave's installation
		SpaceRequest.get("/1/installation/" + daveInstallId).auth(dave).go(200)//
				.assertEquals(1, BADGE);

		// check badge is 1 in vince's installation
		SpaceRequest.get("/1/installation/" + vinceInstallId).auth(vince).go(200)//
				.assertEquals(1, BADGE);

		// vince reads the push and resets its installation badge
		SpaceRequest.put("/1/installation/" + vinceInstallId).auth(vince).bodyJson(BADGE, 0).go(200);

		SpaceRequest.get("/1/installation/" + vinceInstallId).auth(vince).go(200).assertEquals(0, BADGE);

		// admin pushes again to all with automatic badging
		SpaceRequest.post("/1/push")//
		.refresh().auth(test).bodyJson(push).go(200)//
				.assertSizeEquals(2, PUSHED_TO);

		// check badge is 2 in dave's installation
		SpaceRequest.get("/1/installation/" + daveInstallId).auth(dave).go(200)//
				.assertEquals(2, BADGE);

		// check badge is 1 in vince's installation
		SpaceRequest.get("/1/installation/" + vinceInstallId).auth(vince).go(200)//
				.assertEquals(1, BADGE);

		// admin pushes again to all but with semi automatic badging
		push.put(BADGE_STRATEGY, "semi");
		SpaceRequest.post("/1/push")//
		.refresh().auth(test).bodyJson(push).go(200)//
				.assertSizeEquals(2, PUSHED_TO);

		// check badge is 2 in dave's installation
		SpaceRequest.get("/1/installation/" + daveInstallId).auth(dave).go(200)//
				.assertEquals(2, BADGE);

		// check badge is 1 in vince's installation
		SpaceRequest.get("/1/installation/" + vinceInstallId).auth(vince).go(200)//
				.assertEquals(1, BADGE);

	}

	private ObjectNode toJsonTag(String key, String value) {
		return Json7.object(KEY, key, VALUE, value);
	}

	private ArrayNode toJsonTags(String... strings) {
		JsonBuilder<ArrayNode> array = Json7.arrayBuilder();
		for (int i = 0; i < strings.length; i = i + 2)
			array.object().put(KEY, strings[i]).put(VALUE, strings[i + 1]).end();
		return array.build();
	}

	private String installApplication(String appId, String pushService, SpaceDog backend, SpaceDog user) {

		String installId = SpaceRequest.post("/1/installation").auth(user)//
				.bodyJson(TOKEN, "token-" + user.username(), APP_ID, appId, PUSH_SERVICE, pushService)//
				.go(201)//
				.objectNode().get(ID).asText();

		SpaceRequest.get("/1/installation/" + installId).auth(backend).go(200)//
				.assertEquals(appId, APP_ID)//
				.assertEquals(pushService, PUSH_SERVICE)//
				.assertEquals("token-" + user.username(), TOKEN)//
				.assertEquals(user.username(), USER_ID)//
				.assertNotPresent(TAGS);

		return installId;
	}
}
