/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceTest;
import io.spacedog.utils.DataPermission;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.Schema;
import io.spacedog.utils.Schema.SchemaAcl;

//@TestOften
public class PushResourceTestOften2 extends SpaceTest {

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
	private static final String CREDENTIALS_ID = "credentialsId";
	private static final String CREDENTIALS_NAME = "credentialsName";
	private static final String ENDPOINT = "endpoint";
	private static final String TOKEN = "token";
	private static final String BADGE = "badge";
	private static final String BADGE_STRATEGY = "badgeStrategy";
	private static final String PUSH_SERVICE = "pushService";
	private static final String APP_ID = "appId";

	// @Test
	public void usersInstallAppAndPush() {

		// prepare
		prepareTest();
		Backend test = resetTestBackend();

		// prepare users
		User dave = signUp(test, "dave", "hi dave");
		User vince = signUp(test, "vince", "hi vince");
		User fred = signUp(test, "fred", "hi fred");
		User nath = signUp(test, "nath", "hi nath");

		// prepare installation schema
		initPushDefaultSchema(test);

		// add create permission to guest requests
		Schema schema = getSchema("installation", test);
		schema.acl(new SchemaAcl()//
				.set("key", DataPermission.create, DataPermission.read, DataPermission.update)//
				.set("user", DataPermission.create, DataPermission.read, DataPermission.update)//
				.set("admin", DataPermission.create, DataPermission.update_all, //
						DataPermission.search, DataPermission.delete_all));

		setSchema(schema, test);

		// anonymous fails to installs joho
		// because trying to set credentials id is forbidden
		SpaceRequest.post("/1/installation").backend(test)//
				.body(TOKEN, "token-unknown", APP_ID, "joho", PUSH_SERVICE, GCM, //
						CREDENTIALS_ID, "XXX")//
				.go(400);

		// dave fails to installs joho
		// because trying to set credentials name is forbidden
		SpaceRequest.post("/1/installation").userAuth(dave)//
				.body(TOKEN, "token-unknown", APP_ID, "joho", PUSH_SERVICE, GCM, //
						CREDENTIALS_NAME, "XXX")//
				.go(400);

		// admin fails to installs joho
		// because trying to set endpoint is forbidden
		SpaceRequest.post("/1/installation").adminAuth(test)//
				.body(TOKEN, "token-unknown", APP_ID, "joho", PUSH_SERVICE, GCM, //
						ENDPOINT, "XXX")//
				.go(400);

		// guest user installs joho
		String unknownInstallId = SpaceRequest.post("/1/installation")//
				.backend(test)//
				.body(TOKEN, "token-unknown", APP_ID, "joho", PUSH_SERVICE, GCM)//
				.go(201)//
				.getString(ID);

		SpaceRequest.get("/1/installation/" + unknownInstallId)//
				// .adminAuth(test)//
				.backend(test).go(200)//
				.assertEquals("joho", APP_ID)//
				.assertEquals(GCM, PUSH_SERVICE)//
				.assertEquals("token-unknown", TOKEN)//
				.assertNotPresent(CREDENTIALS_ID)//
				.assertNotPresent(CREDENTIALS_NAME)//
				.assertNotPresent(TAGS);

		// vince and fred install joho
		String vinceInstallId = installApplication("joho", GCM, test, vince);
		String fredInstallId = installApplication("joho", APNS, test, fred);
		String daveInstallId = installApplication("joho", APNS, test, dave);

		// vince pushes a simple message to fred
		SpaceRequest.post("/1/installation/" + fredInstallId + "/push")//
				.userAuth(vince).body(MESSAGE, "coucou").go(200)//
				.assertEquals(fredInstallId, "pushedTo.0.installationId")//
				.assertEquals(fred.username, "pushedTo.0.userId");

		// vince pushes a complex object message to dave
		SpaceRequest.post("/1/installation/" + daveInstallId + "/push")//
				.userAuth(vince)//
				.body(MESSAGE,
						Json.object("APNS",
								Json.object("aps", //
										Json.object("alert", "coucou"))))//
				.go(200);

		// vince fails to push to invalid installation id
		SpaceRequest.post("/1/installation/XXX/push")//
				.userAuth(vince).body(MESSAGE, "coucou").go(404);

		// nath installs birdee
		String nathInstallId = installApplication("birdee", APNS, test, nath);

		// vince updates its installation
		SpaceRequest.put("/1/installation/" + vinceInstallId)//
				.backend(test).userAuth(vince)//
				.body(TOKEN, "super-token-vince", APP_ID, "joho", PUSH_SERVICE, GCM)//
				.go(200);

		SpaceRequest.get("/1/installation/" + vinceInstallId)//
				.backend(test).userAuth(vince).go(200)//
				.assertEquals("joho", APP_ID)//
				.assertEquals("super-token-vince", TOKEN)//
				.assertEquals(vince.username, CREDENTIALS_NAME)//
				.assertEquals(vince.id, CREDENTIALS_ID)//
				.assertNotPresent(TAGS);

		// vince fails to get all installations since not admin
		SpaceRequest.get("/1/installation")//
				.backend(test).userAuth(vince)//
				.go(403);

		// admin gets all installations
		SpaceRequest.get("/1/installation").refresh().adminAuth(test).go(200)//
				.assertSizeEquals(5, "results")//
				.assertContainsValue(unknownInstallId, ID)//
				.assertContainsValue(daveInstallId, ID)//
				.assertContainsValue(vinceInstallId, ID)//
				.assertContainsValue(fredInstallId, ID)//
				.assertContainsValue(nathInstallId, ID);

		// nath adds bonjour/toi tag to her install
		SpaceRequest.post("/1/installation/" + nathInstallId + "/tags")//
				.backend(test).userAuth(nath)//
				.body(toJsonTag("bonjour", "toi")).go(200);

		SpaceRequest.get("/1/installation/" + nathInstallId + "/tags")//
				.backend(test).userAuth(nath).go(200)//
				.assertContains(toJsonTag("bonjour", "toi"))//
				.assertSizeEquals(1);

		// nath adds again the same tag and it changes nothing
		// there is no duplicate as a result
		SpaceRequest.post("/1/installation/" + nathInstallId + "/tags")//
				.backend(test).userAuth(nath)//
				.body(toJsonTag("bonjour", "toi")).go(200);

		SpaceRequest.get("/1/installation/" + nathInstallId + "/tags")//
				.backend(test).userAuth(nath).go(200)//
				.assertContains(toJsonTag("bonjour", "toi"))//
				.assertSizeEquals(1);

		// vince adds bonjour/toi tag to his install
		SpaceRequest.post("/1/installation/" + vinceInstallId + "/tags")//
				.backend(test).userAuth(vince)//
				.body(toJsonTag("bonjour", "toi")).go(200);

		SpaceRequest.get("/1/installation/" + vinceInstallId + "/tags")//
				.backend(test).userAuth(vince).go(200)//
				.assertContains(toJsonTag("bonjour", "toi"))//
				.assertSizeEquals(1);

		// vince adds hi/there tag to his install
		SpaceRequest.post("/1/installation/" + vinceInstallId + "/tags")//
				.backend(test).userAuth(vince)//
				.body(toJsonTag("hi", "there")).go(200);

		SpaceRequest.get("/1/installation/" + vinceInstallId + "/tags")//
				.backend(test).userAuth(vince).go(200)//
				.assertContains(toJsonTag("bonjour", "toi"))//
				.assertContains(toJsonTag("hi", "there"))//
				.assertSizeEquals(2);

		// vince deletes bonjour/toi tag from his install
		SpaceRequest.delete("/1/installation/" + vinceInstallId + "/tags")//
				.backend(test).userAuth(vince)//
				.body(toJsonTag("bonjour", "toi")).go(200);

		SpaceRequest.get("/1/installation/" + vinceInstallId + "/tags")//
				.backend(test).userAuth(vince).go(200)//
				.assertContains(toJsonTag("hi", "there"))//
				.assertSizeEquals(1);

		// vince deletes hi/there tag from his install
		SpaceRequest.delete("/1/installation/" + vinceInstallId + "/tags")//
				.backend(test).userAuth(vince)//
				.body(toJsonTag("hi", "there")).go(200);

		SpaceRequest.get("/1/installation/" + vinceInstallId + "/tags")//
				.backend(test).userAuth(vince).go(200)//
				.assertSizeEquals(0);

		// vince sets all his install tags to bonjour/toi and hi/there
		SpaceRequest.put("/1/installation/" + vinceInstallId + "/tags")//
				.backend(test).userAuth(vince)//
				.body(toJsonTags("hi", "there", "bonjour", "toi")).go(200);

		SpaceRequest.get("/1/installation/" + vinceInstallId + "/tags")//
				.backend(test).userAuth(vince).go(200)//
				.assertContains(toJsonTag("bonjour", "toi"))//
				.assertContains(toJsonTag("hi", "there"))//
				.assertSizeEquals(2);

		// fred sets all his install tags to bonjour/toi
		SpaceRequest.put("/1/installation/" + fredInstallId + "/tags")//
				.backend(test).userAuth(fred)//
				.body(toJsonTags("bonjour", "toi")).go(200);

		SpaceRequest.get("/1/installation/" + fredInstallId + "/tags")//
				.backend(test).userAuth(fred).go(200)//
				.assertContains(toJsonTag("bonjour", "toi"))//
				.assertSizeEquals(1);

		// vince pushes to all joho installations
		// this means users and anonymous installations
		ObjectNode push = Json.object(APP_ID, "joho", MESSAGE, "This is a push!");
		SpaceRequest.post("/1/push").refresh().userAuth(vince).body(push).go(200)//
				.assertFalse(FAILURES)//
				.assertSizeEquals(4, PUSHED_TO)//
				.assertContainsValue(unknownInstallId, INSTALLATION_ID)//
				.assertContainsValue("dave", USER_ID)//
				.assertContainsValue("vince", USER_ID)//
				.assertContainsValue("fred", USER_ID)//
				.assertContainsValue("dave", CREDENTIALS_NAME)//
				.assertContainsValue("vince", CREDENTIALS_NAME)//
				.assertContainsValue("fred", CREDENTIALS_NAME);

		// vince pushes to all joho users
		// this means excluding anonymous installations
		push.put(USERS_ONLY, true);
		SpaceRequest.post("/1/push").refresh().userAuth(vince).body(push).go(200)//
				.assertFalse(FAILURES)//
				.assertSizeEquals(3, PUSHED_TO)//
				.assertContainsValue("dave", CREDENTIALS_NAME)//
				.assertContainsValue("vince", CREDENTIALS_NAME)//
				.assertContainsValue("fred", CREDENTIALS_NAME);

		// vince pushes to APNS only joho users
		push.put(PUSH_SERVICE, APNS);
		SpaceRequest.post("/1/push").refresh().userAuth(vince).body(push).go(200)//
				.assertFalse(FAILURES)//
				.assertSizeEquals(2, PUSHED_TO)//
				.assertContainsValue("dave", CREDENTIALS_NAME)//
				.assertContainsValue("fred", CREDENTIALS_NAME);

		// vince pushes to APNS only joho users with tag bonjour/toi
		push.set(TAGS, toJsonTag("bonjour", "toi"));
		SpaceRequest.post("/1/push").userAuth(vince).body(push).go(200)//
				.assertFalse(FAILURES)//
				.assertSizeEquals(1, PUSHED_TO)//
				.assertContainsValue("fred", CREDENTIALS_NAME);

		// vince pushes to all joho users with tag bonjour/toi
		push.remove(PUSH_SERVICE);
		SpaceRequest.post("/1/push").userAuth(vince).body(push).go(200)//
				.assertFalse(FAILURES)//
				.assertSizeEquals(2, PUSHED_TO)//
				.assertContainsValue("vince", CREDENTIALS_NAME)//
				.assertContainsValue("fred", CREDENTIALS_NAME);

		// vince pushes to all joho users with tags bonjour/toi and hi/there
		push.set(TAGS, toJsonTags("bonjour", "toi", "hi", "there"));
		SpaceRequest.post("/1/push").userAuth(vince).body(push).go(200)//
				.assertFalse(FAILURES)//
				.assertSizeEquals(1, PUSHED_TO)//
				.assertContainsValue("vince", CREDENTIALS_NAME);

		// vince gets 404 when he pushes to invalid app id
		push = Json.object(APP_ID, "XXX", MESSAGE, "This is a push!");
		SpaceRequest.post("/1/push").userAuth(vince).body(push).go(404);

		// vince can not read, update nor delete dave's installation
		SpaceRequest.get("/1/installation/" + daveInstallId)//
				.userAuth(vince).go(403);

		SpaceRequest.put("/1/installation/" + daveInstallId)//
				.userAuth(vince).body(BADGE, 2).go(403);

		// also true with /data/installation route
		SpaceRequest.get("/1/data/installation/" + daveInstallId)//
				.userAuth(vince).go(403);

		SpaceRequest.put("/1/data/installation/" + daveInstallId)//
				.userAuth(vince).body(BADGE, 2).go(403);

		// dave can not update his installation app id
		// if he does not provide the token
		// this is true for push service and endpoint
		SpaceRequest.put("/1/installation/" + daveInstallId)//
				.userAuth(dave).body(APP_ID, "joho2").go(400);
	}

	// @Test
	public void badgeManagement() {

		// prepare
		prepareTest();
		Backend test = resetTestBackend();

		// prepare users
		User dave = signUp(test, "dave", "hi dave");
		User vince = signUp(test, "vince", "hi vince");

		// prepare installation schema
		initPushDefaultSchema(test);

		// vince and dave install joho
		String vinceInstallId = installApplication("joho", APNS, test, vince);
		String daveInstallId = installApplication("joho", APNS, test, dave);

		// vince pushes a message to dave with manual badge = 3
		SpaceRequest.post("/1/installation/" + daveInstallId + "/push")//
				.userAuth(vince)//
				.body(MESSAGE,
						Json.object(APNS, //
								Json.object("aps", //
										Json.object("alert", "coucou", BADGE, 3))))//
				.go(200)//
				.assertEquals(daveInstallId, "pushedTo.0.installationId");

		// badge is not set in installation
		// since badge management is still manual
		SpaceRequest.get("/1/installation/" + daveInstallId)//
				.userAuth(dave).go(200)//
				.assertNotPresent(BADGE);

		// vince pushes a message to all with automatic badging
		// this means installation.badge is incremented on the server
		// and installation.badge is sent to each installation
		ObjectNode push = Json.object(APP_ID, "joho", BADGE_STRATEGY, "auto", //
				MESSAGE, "Badge is the new trend!");

		SpaceRequest.post("/1/push")//
				.refresh().adminAuth(test)//
				.body(push).go(200)//
				.assertSizeEquals(2, PUSHED_TO)//
				.assertContainsValue(vinceInstallId, INSTALLATION_ID)//
				.assertContainsValue(daveInstallId, INSTALLATION_ID);

		// check badge is 1 in dave's installation
		SpaceRequest.get("/1/installation/" + daveInstallId).userAuth(dave).go(200)//
				.assertEquals(1, BADGE);

		// check badge is 1 in vince's installation
		SpaceRequest.get("/1/installation/" + vinceInstallId).userAuth(vince).go(200)//
				.assertEquals(1, BADGE);

		// vince reads the push and resets its installation badge
		SpaceRequest.put("/1/installation/" + vinceInstallId)//
				.userAuth(vince).body(BADGE, 0).go(200);

		SpaceRequest.get("/1/installation/" + vinceInstallId)//
				.userAuth(vince).go(200).assertEquals(0, BADGE);

		// admin pushes again to all with automatic badging
		SpaceRequest.post("/1/push")//
				.refresh().adminAuth(test).body(push).go(200)//
				.assertSizeEquals(2, PUSHED_TO);

		// check badge is 2 in dave's installation
		SpaceRequest.get("/1/installation/" + daveInstallId).userAuth(dave).go(200)//
				.assertEquals(2, BADGE);

		// check badge is 1 in vince's installation
		SpaceRequest.get("/1/installation/" + vinceInstallId).userAuth(vince).go(200)//
				.assertEquals(1, BADGE);

		// admin pushes again to all but with semi automatic badging
		push.put(BADGE_STRATEGY, "semi");
		SpaceRequest.post("/1/push")//
				.refresh().adminAuth(test).body(push).go(200)//
				.assertSizeEquals(2, PUSHED_TO);

		// check badge is 2 in dave's installation
		SpaceRequest.get("/1/installation/" + daveInstallId).userAuth(dave).go(200)//
				.assertEquals(2, BADGE);

		// check badge is 1 in vince's installation
		SpaceRequest.get("/1/installation/" + vinceInstallId).userAuth(vince).go(200)//
				.assertEquals(1, BADGE);
	}

	private ObjectNode toJsonTag(String key, String value) {
		return Json.object(KEY, key, VALUE, value);
	}

	private ArrayNode toJsonTags(String... strings) {
		JsonBuilder<ArrayNode> array = Json.arrayBuilder();
		for (int i = 0; i < strings.length; i = i + 2)
			array.object().put(KEY, strings[i]).put(VALUE, strings[i + 1]).end();
		return array.build();
	}

	private String installApplication(String appId, String pushService, Backend backend, User user) {

		String installId = SpaceRequest.post("/1/installation")//
				.userAuth(user)//
				.body(TOKEN, "token-" + user.username, APP_ID, appId, PUSH_SERVICE, pushService)//
				.go(201)//
				.getString(ID);

		SpaceRequest.get("/1/installation/" + installId)//
				.adminAuth(backend).go(200)//
				.assertEquals(appId, APP_ID)//
				.assertEquals(pushService, PUSH_SERVICE)//
				.assertEquals("token-" + user.username, TOKEN)//
				.assertEquals(user.username, CREDENTIALS_NAME)//
				.assertEquals(user.id, CREDENTIALS_ID)//
				.assertNotPresent(TAGS);

		return installId;
	}
}
