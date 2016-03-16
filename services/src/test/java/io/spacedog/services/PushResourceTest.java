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

	@Test
	public void usersInstallAppAndPush() throws Exception {

		// prepare
		SpaceDogHelper.prepareTest();
		Backend testBackend = SpaceDogHelper.resetTestBackend();
		SpaceDogHelper.initUserDefaultSchema(testBackend);
		User vince = SpaceDogHelper.createUser(testBackend, "vince", "hi vince");
		User fred = SpaceDogHelper.createUser(testBackend, "fred", "hi fred");
		User nath = SpaceDogHelper.createUser(testBackend, "nath", "hi nath");

		// create installation schema

		ObjectNode installationSchema = SchemaBuilder2.builder("installation")//
				.stringProperty("appId", true)//
				.stringProperty("deviceToken", true)//
				.stringProperty("providerId", true)//
				.stringProperty("userId", false)//
				.startObjectProperty("tags", false)//
				.stringProperty("key", true)//
				.stringProperty("value", true)//
				.endObjectProperty()//
				.build();

		SpaceRequest.put("/1/schema/installation")//
				.basicAuth(testBackend).body(installationSchema).go(201);

		// unauthenticated user installs myairport
		// and fails to set unauthenticated userId

		String installId = SpaceRequest.post("/1/installation")//
				.backend(testBackend)//
				.body(Json.object("deviceToken", "token-unknown", "appId", "myairport", "userId", "david"))//
				.go(201)//
				.objectNode().get("id").asText();

		SpaceRequest.get("/1/installation/" + installId)//
				.basicAuth(testBackend).go(200)//
				.assertEquals("myairport", "appId")//
				.assertEquals("token-unknown", "deviceToken")//
				.assertNotPresent("userId")//
				.assertNotPresent("tags");

		// vince and fred install joho

		String vinceInstallId = installApplication("joho", testBackend, vince);
		String fredInstallId = installApplication("joho", testBackend, fred);

		// vince and fred installs birdee

		String nathInstallId = installApplication("birdee", testBackend, nath);

		// install update should succeed

		SpaceRequest.put("/1/installation/" + vinceInstallId)//
				.backend(testBackend).basicAuth(vince)//
				.body(Json.object("deviceToken", "super-token-vince", "appId", "joho"))//
				.go(200);

		SpaceRequest.get("/1/installation/" + vinceInstallId)//
				.backend(testBackend).basicAuth(vince)//
				.go(200)//
				.assertEquals("joho", "appId")//
				.assertEquals("super-token-vince", "deviceToken")//
				.assertEquals("vince", "userId")//
				.assertNotPresent("tags");

		// user fails to get all installations

		SpaceRequest.get("/1/installation")//
				.backend(testBackend).basicAuth(vince)//
				.go(401);

		// admin succeed to get all installations

		SpaceRequest.get("/1/installation?refresh=true")//
				.basicAuth(testBackend)//
				.go(200)//
				.assertSizeEquals(4, "results")//
				.assertContainsValue("token-unknown", "deviceToken")//
				.assertContainsValue("super-token-vince", "deviceToken")//
				.assertContainsValue("token-fred", "deviceToken")//
				.assertContainsValue("token-nath", "deviceToken");

		// add bonjour/toi tag to nath install

		SpaceRequest.post("/1/installation/" + nathInstallId + "/tags")//
				.backend(testBackend).basicAuth(nath)//
				.body(toJsonTag("bonjour", "toi")).go(200);

		SpaceRequest.get("/1/installation/" + nathInstallId + "/tags")//
				.backend(testBackend).basicAuth(nath).go(200)//
				.assertContains(toJsonTag("bonjour", "toi"))//
				.assertSizeEquals(1);

		// add again the same tag is ok and there is no duplicate as a result

		SpaceRequest.post("/1/installation/" + nathInstallId + "/tags")//
				.backend(testBackend).basicAuth(nath)//
				.body(toJsonTag("bonjour", "toi")).go(200);

		SpaceRequest.get("/1/installation/" + nathInstallId + "/tags")//
				.backend(testBackend).basicAuth(nath).go(200)//
				.assertContains(toJsonTag("bonjour", "toi"))//
				.assertSizeEquals(1);

		// add bonjour/toi tag to vince install

		SpaceRequest.post("/1/installation/" + vinceInstallId + "/tags")//
				.backend(testBackend).basicAuth(vince)//
				.body(toJsonTag("bonjour", "toi")).go(200);

		SpaceRequest.get("/1/installation/" + vinceInstallId + "/tags")//
				.backend(testBackend).basicAuth(vince).go(200)//
				.assertContains(toJsonTag("bonjour", "toi"))//
				.assertSizeEquals(1);

		// add hi/there tag to vince install

		SpaceRequest.post("/1/installation/" + vinceInstallId + "/tags")//
				.backend(testBackend).basicAuth(vince)//
				.body(toJsonTag("hi", "there")).go(200);

		SpaceRequest.get("/1/installation/" + vinceInstallId + "/tags")//
				.backend(testBackend).basicAuth(vince).go(200)//
				.assertContains(toJsonTag("bonjour", "toi"))//
				.assertContains(toJsonTag("hi", "there"))//
				.assertSizeEquals(2);

		// delete bonjour/toi tag from vince install

		SpaceRequest.delete("/1/installation/" + vinceInstallId + "/tags")//
				.backend(testBackend).basicAuth(vince)//
				.body(toJsonTag("bonjour", "toi")).go(200);

		SpaceRequest.get("/1/installation/" + vinceInstallId + "/tags")//
				.backend(testBackend).basicAuth(vince).go(200)//
				.assertContains(toJsonTag("hi", "there"))//
				.assertSizeEquals(1);

		// delete hi/there tag from vince install

		SpaceRequest.delete("/1/installation/" + vinceInstallId + "/tags")//
				.backend(testBackend).basicAuth(vince)//
				.body(toJsonTag("hi", "there")).go(200);

		SpaceRequest.get("/1/installation/" + vinceInstallId + "/tags")//
				.backend(testBackend).basicAuth(vince).go(200)//
				.assertSizeEquals(0);

		// set all vince install tags to bonjour/toi and hi/there

		SpaceRequest.put("/1/installation/" + vinceInstallId + "/tags")//
				.backend(testBackend).basicAuth(vince)//
				.body(toJsonTags("hi", "there", "bonjour", "toi")).go(200);

		SpaceRequest.get("/1/installation/" + vinceInstallId + "/tags")//
				.backend(testBackend).basicAuth(vince).go(200)//
				.assertContains(toJsonTag("bonjour", "toi"))//
				.assertContains(toJsonTag("hi", "there"))//
				.assertSizeEquals(2);

		// set all fred install tags to bonjour/toi

		SpaceRequest.put("/1/installation/" + fredInstallId + "/tags")//
				.backend(testBackend).basicAuth(fred)//
				.body(toJsonTags("bonjour", "toi")).go(200);

		SpaceRequest.get("/1/installation/" + fredInstallId + "/tags")//
				.backend(testBackend).basicAuth(fred).go(200)//
				.assertContains(toJsonTag("bonjour", "toi"))//
				.assertSizeEquals(1);

		// push to all joho users

		ObjectNode push = Json.objectBuilder()//
				.put("appId", "joho")//
				.put("message", "This is a push!")//
				.build();

		SpaceRequest.post("/1/push?refresh=true")//
				.backend(testBackend).basicAuth(vince)//
				.body(push)//
				.go(200)//
				.assertSizeEquals(2, "pushedTo")//
				.assertContainsValue("vince", "userId")//
				.assertContainsValue("fred", "userId");

		// push to joho users with tag bonjour/toi

		push.set("tags", toJsonTag("bonjour", "toi"));

		SpaceRequest.post("/1/push")//
				.backend(testBackend).basicAuth(vince)//
				.body(push)//
				.go(200)//
				.assertSizeEquals(2, "pushedTo")//
				.assertContainsValue("vince", "userId")//
				.assertContainsValue("fred", "userId");

		// push to joho users with tags bonjour/toi and hi/there

		push.set("tags", toJsonTags("bonjour", "toi", "hi", "there"));

		SpaceRequest.post("/1/push")//
				.backend(testBackend).basicAuth(vince)//
				.body(push)//
				.go(200)//
				.assertSizeEquals(1, "pushedTo")//
				.assertContainsValue("vince", "userId");
	}

	private ObjectNode toJsonTag(String key, String value) {
		return Json.object("key", key, "value", value);
	}

	private ArrayNode toJsonTags(String... strings) {
		JsonBuilder<ArrayNode> array = Json.arrayBuilder();
		for (int i = 0; i < strings.length; i = i + 2)
			array.object().put("key", strings[i]).put("value", strings[i + 1]).end();
		return array.build();
	}

	private String installApplication(String appId, SpaceDogHelper.Backend account, SpaceDogHelper.User user)
			throws Exception {

		String installId = SpaceRequest.post("/1/installation")//
				.backend(account).basicAuth(user)//
				.body(Json.object("deviceToken", "token-" + user.username, "appId", appId))//
				.go(201)//
				.objectNode().get("id").asText();

		SpaceRequest.get("/1/installation/" + installId)//
				.basicAuth(account).go(200)//
				.assertEquals(appId, "appId")//
				.assertEquals("token-" + user.username, "deviceToken")//
				.assertEquals(user.username, "userId")//
				.assertNotPresent("tags");

		return installId;
	}
}
