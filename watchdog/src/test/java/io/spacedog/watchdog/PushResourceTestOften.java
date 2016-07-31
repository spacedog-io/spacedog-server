/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceClient.Backend;
import io.spacedog.client.SpaceClient.User;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.DataPermission;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.Schema;
import io.spacedog.utils.Schema.DataTypeAccessControl;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class PushResourceTestOften extends Assert {

	private static final String PUSHED_TO = "pushedTo";
	private static final String FAILURES = "failures";
	private static final String USERS_ONLY = "usersOnly";
	private static final String ID = "id";
	private static final String MESSAGE = "message";
	private static final String APNS = "APNS";
	private static final String GCM = "GCM";
	private static final String VALUE = "value";
	private static final String KEY = "key";
	private static final String TAGS = "tags";
	private static final String USER_ID = "userId";
	private static final String ENDPOINT = "endpoint";
	private static final String TOKEN = "token";
	private static final String PUSH_SERVICE = "pushService";
	private static final String APP_ID = "appId";

	@Test
	public void usersInstallAppAndPush() throws Exception {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();

		// prepare users
		User dave = SpaceClient.newCredentials(test, "dave", "hi dave");
		User vince = SpaceClient.newCredentials(test, "vince", "hi vince");
		User fred = SpaceClient.newCredentials(test, "fred", "hi fred");
		User nath = SpaceClient.newCredentials(test, "nath", "hi nath");

		// prepare installation schema
		SpaceClient.initPushDefaultSchema(test);

		// add create permission to guest requests
		Schema schema = SpaceClient.getSchema("installation", test);
		DataTypeAccessControl acl = new DataTypeAccessControl();
		acl.put("key", Sets.newHashSet(DataPermission.create, DataPermission.update));
		acl.put("user", Sets.newHashSet(DataPermission.create, DataPermission.read, //
				DataPermission.update));
		acl.put("admin", Sets.newHashSet(DataPermission.create, DataPermission.update_all, //
				DataPermission.search, DataPermission.delete_all));
		schema.acl(acl);
		SpaceClient.setSchema(schema, test);

		// non authenticated user installs joho
		// and fails to set installation userId and endpoint fields

		String unknownInstallId = SpaceRequest.post("/1/installation")//
				.backend(test)//
				.body(TOKEN, "token-unknown", APP_ID, "joho", PUSH_SERVICE, GCM, //
						USER_ID, "XXX", ENDPOINT, "XXX")//
				.go(201)//
				.objectNode().get(ID).asText();

		SpaceRequest.get("/1/installation/" + unknownInstallId)//
				.adminAuth(test).go(200)//
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

		// nath installs birdee

		String nathInstallId = installApplication("birdee", APNS, test, nath);

		// vince updates its installation

		SpaceRequest.put("/1/installation/" + vinceInstallId)//
				.backend(test).userAuth(vince)//
				.body(TOKEN, "super-token-vince", APP_ID, "joho", PUSH_SERVICE, GCM)//
				.go(200);

		SpaceRequest.get("/1/installation/" + vinceInstallId)//
				.backend(test).userAuth(vince)//
				.go(200)//
				.assertEquals("joho", APP_ID)//
				.assertEquals("super-token-vince", TOKEN)//
				.assertEquals("vince", USER_ID)//
				.assertNotPresent(TAGS);

		// vince fails to get all installations since not admin

		SpaceRequest.get("/1/installation")//
				.backend(test).userAuth(vince)//
				.go(401);

		// admin gets all installations

		SpaceRequest.get("/1/installation").refresh()//
				.adminAuth(test)//
				.go(200)//
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

		SpaceRequest.post("/1/push")//
				.refresh()//
				.userAuth(vince)//
				.body(push)//
				.go(200)//
				.assertFalse(FAILURES)//
				.assertSizeEquals(4, PUSHED_TO)//
				.assertContainsValue(unknownInstallId, ID)//
				.assertContainsValue("dave", USER_ID)//
				.assertContainsValue("vince", USER_ID)//
				.assertContainsValue("fred", USER_ID);

		// vince pushes to all joho users
		// this means excluding anonymous installations

		push.put(USERS_ONLY, true);

		SpaceRequest.post("/1/push").refresh()//
				.userAuth(vince)//
				.body(push)//
				.go(200)//
				.assertFalse(FAILURES)//
				.assertSizeEquals(3, PUSHED_TO)//
				.assertContainsValue("dave", USER_ID)//
				.assertContainsValue("vince", USER_ID)//
				.assertContainsValue("fred", USER_ID);

		// vince pushes to APNS only joho users

		push.put(PUSH_SERVICE, APNS);

		SpaceRequest.post("/1/push").refresh()//
				.userAuth(vince)//
				.body(push)//
				.go(200)//
				.assertFalse(FAILURES)//
				.assertSizeEquals(2, PUSHED_TO)//
				.assertContainsValue("dave", USER_ID)//
				.assertContainsValue("fred", USER_ID);

		// vince pushes to APNS only joho users with tag bonjour/toi

		push.set(TAGS, toJsonTag("bonjour", "toi"));

		SpaceRequest.post("/1/push")//
				.userAuth(vince)//
				.body(push)//
				.go(200)//
				.assertFalse(FAILURES)//
				.assertSizeEquals(1, PUSHED_TO)//
				.assertContainsValue("fred", USER_ID);

		// vince pushes to all joho users with tag bonjour/toi

		push.remove(PUSH_SERVICE);

		SpaceRequest.post("/1/push")//
				.userAuth(vince)//
				.body(push)//
				.go(200)//
				.assertFalse(FAILURES)//
				.assertSizeEquals(2, PUSHED_TO)//
				.assertContainsValue("vince", USER_ID)//
				.assertContainsValue("fred", USER_ID);

		// vince pushes to all joho users with tags bonjour/toi and hi/there

		push.set(TAGS, toJsonTags("bonjour", "toi", "hi", "there"));

		SpaceRequest.post("/1/push")//
				.userAuth(vince)//
				.body(push)//
				.go(200)//
				.assertFalse(FAILURES)//
				.assertSizeEquals(1, PUSHED_TO)//
				.assertContainsValue("vince", USER_ID);

		// vince gets 404 when he pushes to invalid app id

		push = Json.object(APP_ID, "XXX", MESSAGE, "This is a push!");

		SpaceRequest.post("/1/push")//
				.userAuth(vince)//
				.body(push)//
				.go(404);

		// vince can not read, update nor delete dave's installation

		SpaceRequest.get("/1/data/installation/" + daveInstallId)//
				.userAuth(vince).go(403);

		SpaceRequest.put("/1/data/installation/" + daveInstallId)//
				.userAuth(vince)//
				.body(APP_ID, "joho2")//
				.go(403);
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

	private String installApplication(String appId, String pushService, Backend backend, User user) throws Exception {

		String installId = SpaceRequest.post("/1/installation")//
				.userAuth(user)//
				.body(TOKEN, "token-" + user.username, APP_ID, appId, PUSH_SERVICE, pushService)//
				.go(201)//
				.objectNode().get(ID).asText();

		SpaceRequest.get("/1/installation/" + installId)//
				.adminAuth(backend).go(200)//
				.assertEquals(appId, APP_ID)//
				.assertEquals(pushService, PUSH_SERVICE)//
				.assertEquals("token-" + user.username, TOKEN)//
				.assertEquals(user.username, USER_ID)//
				.assertNotPresent(TAGS);

		return installId;
	}
}
