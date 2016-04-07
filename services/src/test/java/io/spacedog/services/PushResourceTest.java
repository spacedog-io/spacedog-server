/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceDogHelper.Backend;
import io.spacedog.client.SpaceDogHelper.User;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.SchemaBuilder2;

public class PushResourceTest extends Assert {

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
		SpaceDogHelper.prepareTest();
		Backend testBackend = SpaceDogHelper.resetTestBackend();
		SpaceDogHelper.initUserDefaultSchema(testBackend);
		User dave = SpaceDogHelper.createUser(testBackend, "dave", "hi dave");
		User vince = SpaceDogHelper.createUser(testBackend, "vince", "hi vince");
		User fred = SpaceDogHelper.createUser(testBackend, "fred", "hi fred");
		User nath = SpaceDogHelper.createUser(testBackend, "nath", "hi nath");

		// create installation schema

		ObjectNode installationSchema = SchemaBuilder2.builder("installation")//
				.stringProperty(APP_ID, true)//
				.stringProperty(PUSH_SERVICE, true)//
				.stringProperty(TOKEN, true)//
				.stringProperty(ENDPOINT, true)//
				.stringProperty(USER_ID, false)//
				.startObjectProperty(TAGS, false)//
				.stringProperty(KEY, true)//
				.stringProperty(VALUE, true)//
				.endObjectProperty()//
				.build();

		SpaceRequest.put("/1/schema/installation")//
				.adminAuth(testBackend).body(installationSchema).go(201);

		// unauthenticated user installs joho
		// and fails to set installation userId and endpoint fields

		String unknownInstallId = SpaceRequest.post("/1/installation")//
				.backend(testBackend)//
				.body(Json.object(TOKEN, "token-unknown", APP_ID, "joho", PUSH_SERVICE, GCM, //
						USER_ID, "XXX", ENDPOINT, "XXX"))//
				.go(201)//
				.objectNode().get(ID).asText();

		SpaceRequest.get("/1/installation/" + unknownInstallId)//
				.adminAuth(testBackend).go(200)//
				.assertEquals("joho", APP_ID)//
				.assertEquals(GCM, PUSH_SERVICE)//
				.assertEquals("token-unknown", TOKEN)//
				.assertEquals("FAKE_ENDPOINT_FOR_TESTING", ENDPOINT)//
				.assertNotPresent(USER_ID)//
				.assertNotPresent(TAGS);

		// vince and fred install joho

		String vinceInstallId = installApplication("joho", GCM, testBackend, vince);
		String fredInstallId = installApplication("joho", APNS, testBackend, fred);
		String daveInstallId = installApplication("joho", APNS, testBackend, dave);

		// nath installs birdee

		String nathInstallId = installApplication("birdee", APNS, testBackend, nath);

		// vince updates its installation

		SpaceRequest.put("/1/installation/" + vinceInstallId)//
				.backend(testBackend).userAuth(vince)//
				.body(Json.object(TOKEN, "super-token-vince", APP_ID, "joho", PUSH_SERVICE, GCM))//
				.go(200);

		SpaceRequest.get("/1/installation/" + vinceInstallId)//
				.backend(testBackend).userAuth(vince)//
				.go(200)//
				.assertEquals("joho", APP_ID)//
				.assertEquals("super-token-vince", TOKEN)//
				.assertEquals("vince", USER_ID)//
				.assertNotPresent(TAGS);

		// vince fails to get all installations since not admin

		SpaceRequest.get("/1/installation")//
				.backend(testBackend).userAuth(vince)//
				.go(401);

		// admin gets all installations

		SpaceRequest.get("/1/installation?refresh=true")//
				.adminAuth(testBackend)//
				.go(200)//
				.assertSizeEquals(5, "results")//
				.assertContainsValue(unknownInstallId, ID)//
				.assertContainsValue(daveInstallId, ID)//
				.assertContainsValue(vinceInstallId, ID)//
				.assertContainsValue(fredInstallId, ID)//
				.assertContainsValue(nathInstallId, ID);

		// nath adds bonjour/toi tag to her install

		SpaceRequest.post("/1/installation/" + nathInstallId + "/tags")//
				.backend(testBackend).userAuth(nath)//
				.body(toJsonTag("bonjour", "toi")).go(200);

		SpaceRequest.get("/1/installation/" + nathInstallId + "/tags")//
				.backend(testBackend).userAuth(nath).go(200)//
				.assertContains(toJsonTag("bonjour", "toi"))//
				.assertSizeEquals(1);

		// nath adds again the same tag and it changes nothing
		// there is no duplicate as a result

		SpaceRequest.post("/1/installation/" + nathInstallId + "/tags")//
				.backend(testBackend).userAuth(nath)//
				.body(toJsonTag("bonjour", "toi")).go(200);

		SpaceRequest.get("/1/installation/" + nathInstallId + "/tags")//
				.backend(testBackend).userAuth(nath).go(200)//
				.assertContains(toJsonTag("bonjour", "toi"))//
				.assertSizeEquals(1);

		// vince adds bonjour/toi tag to his install

		SpaceRequest.post("/1/installation/" + vinceInstallId + "/tags")//
				.backend(testBackend).userAuth(vince)//
				.body(toJsonTag("bonjour", "toi")).go(200);

		SpaceRequest.get("/1/installation/" + vinceInstallId + "/tags")//
				.backend(testBackend).userAuth(vince).go(200)//
				.assertContains(toJsonTag("bonjour", "toi"))//
				.assertSizeEquals(1);

		// vince adds hi/there tag to his install

		SpaceRequest.post("/1/installation/" + vinceInstallId + "/tags")//
				.backend(testBackend).userAuth(vince)//
				.body(toJsonTag("hi", "there")).go(200);

		SpaceRequest.get("/1/installation/" + vinceInstallId + "/tags")//
				.backend(testBackend).userAuth(vince).go(200)//
				.assertContains(toJsonTag("bonjour", "toi"))//
				.assertContains(toJsonTag("hi", "there"))//
				.assertSizeEquals(2);

		// vince deletes bonjour/toi tag from his install

		SpaceRequest.delete("/1/installation/" + vinceInstallId + "/tags")//
				.backend(testBackend).userAuth(vince)//
				.body(toJsonTag("bonjour", "toi")).go(200);

		SpaceRequest.get("/1/installation/" + vinceInstallId + "/tags")//
				.backend(testBackend).userAuth(vince).go(200)//
				.assertContains(toJsonTag("hi", "there"))//
				.assertSizeEquals(1);

		// vince deletes hi/there tag from his install

		SpaceRequest.delete("/1/installation/" + vinceInstallId + "/tags")//
				.backend(testBackend).userAuth(vince)//
				.body(toJsonTag("hi", "there")).go(200);

		SpaceRequest.get("/1/installation/" + vinceInstallId + "/tags")//
				.backend(testBackend).userAuth(vince).go(200)//
				.assertSizeEquals(0);

		// vince sets all his install tags to bonjour/toi and hi/there

		SpaceRequest.put("/1/installation/" + vinceInstallId + "/tags")//
				.backend(testBackend).userAuth(vince)//
				.body(toJsonTags("hi", "there", "bonjour", "toi")).go(200);

		SpaceRequest.get("/1/installation/" + vinceInstallId + "/tags")//
				.backend(testBackend).userAuth(vince).go(200)//
				.assertContains(toJsonTag("bonjour", "toi"))//
				.assertContains(toJsonTag("hi", "there"))//
				.assertSizeEquals(2);

		// fred sets all his install tags to bonjour/toi

		SpaceRequest.put("/1/installation/" + fredInstallId + "/tags")//
				.backend(testBackend).userAuth(fred)//
				.body(toJsonTags("bonjour", "toi")).go(200);

		SpaceRequest.get("/1/installation/" + fredInstallId + "/tags")//
				.backend(testBackend).userAuth(fred).go(200)//
				.assertContains(toJsonTag("bonjour", "toi"))//
				.assertSizeEquals(1);

		// vince pushes to all joho installations
		// this means users and anonymous installations

		ObjectNode push = Json.object(APP_ID, "joho", MESSAGE, "This is a push!");

		SpaceRequest.post("/1/push?refresh=true")//
				.userAuth(vince)//
				.body(push)//
				.go(200)//
				.assertSizeEquals(4, "log")//
				.assertContainsValue(unknownInstallId, ID)//
				.assertContainsValue("dave", USER_ID)//
				.assertContainsValue("vince", USER_ID)//
				.assertContainsValue("fred", USER_ID);

		// vince pushes to all joho users
		// this means excluding anonymous installations

		push.put(USERS_ONLY, true);

		SpaceRequest.post("/1/push?refresh=true")//
				.userAuth(vince)//
				.body(push)//
				.go(200)//
				.assertSizeEquals(3, "log")//
				.assertContainsValue("dave", USER_ID)//
				.assertContainsValue("vince", USER_ID)//
				.assertContainsValue("fred", USER_ID);

		// vince pushes to APNS only joho users

		push.put(PUSH_SERVICE, APNS);

		SpaceRequest.post("/1/push?refresh=true")//
				.userAuth(vince)//
				.body(push)//
				.go(200)//
				.assertSizeEquals(2, "log")//
				.assertContainsValue("dave", USER_ID)//
				.assertContainsValue("fred", USER_ID);

		// vince pushes to APNS only joho users with tag bonjour/toi

		push.set(TAGS, toJsonTag("bonjour", "toi"));

		SpaceRequest.post("/1/push")//
				.userAuth(vince)//
				.body(push)//
				.go(200)//
				.assertSizeEquals(1, "log")//
				.assertContainsValue("fred", USER_ID);

		// vince pushes to all joho users with tag bonjour/toi

		push.remove(PUSH_SERVICE);

		SpaceRequest.post("/1/push")//
				.userAuth(vince)//
				.body(push)//
				.go(200)//
				.assertSizeEquals(2, "log")//
				.assertContainsValue("vince", USER_ID)//
				.assertContainsValue("fred", USER_ID);

		// vince pushes to all joho users with tags bonjour/toi and hi/there

		push.set(TAGS, toJsonTags("bonjour", "toi", "hi", "there"));

		SpaceRequest.post("/1/push")//
				.userAuth(vince)//
				.body(push)//
				.go(200)//
				.assertSizeEquals(1, "log")//
				.assertContainsValue("vince", USER_ID);

		// vince gets 404 when he pushes to invalid app id

		push = Json.object(APP_ID, "XXX", MESSAGE, "This is a push!");

		SpaceRequest.post("/1/push")//
				.userAuth(vince)//
				.body(push)//
				.go(404);
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

	private String installApplication(String appId, String pushService, Backend account, User user) throws Exception {

		String installId = SpaceRequest.post("/1/installation")//
				.backend(account).userAuth(user)//
				.body(Json.object(TOKEN, "token-" + user.username, APP_ID, appId, PUSH_SERVICE, pushService))//
				.go(201)//
				.objectNode().get(ID).asText();

		SpaceRequest.get("/1/installation/" + installId)//
				.adminAuth(account).go(200)//
				.assertEquals(appId, APP_ID)//
				.assertEquals(pushService, PUSH_SERVICE)//
				.assertEquals("token-" + user.username, TOKEN)//
				.assertEquals(user.username, USER_ID)//
				.assertNotPresent(TAGS);

		return installId;
	}
}
