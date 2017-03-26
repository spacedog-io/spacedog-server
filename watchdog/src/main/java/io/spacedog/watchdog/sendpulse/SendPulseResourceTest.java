/**
 * © David Attias 2015
 */
package io.spacedog.watchdog.sendpulse;

import java.util.Collections;

import org.joda.time.DateTime;

import io.spacedog.model.SendPulseSettings;
import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceTarget;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;

public class SendPulseResourceTest extends SpaceTest {

	public void testSendPulseResource() {

		// this must come before preparing the test
		// or else the test flag is set
		// and the sendpulse backend files are not reset
		initSendPulseTestWebApp();

		// prepare
		prepareTest();
		SpaceRequest.env().target(SpaceTarget.local);
		SpaceDog test = resetTestBackend();
		SpaceDog fred = SpaceDog.backend("test").username("fred").signUp("hi fred");

		// set sendpulse test settings
		SendPulseSettings settings = new SendPulseSettings();
		settings.clientId = SpaceRequest.env().get("spacedog.test.sendpulse.client.id");
		settings.clientSecret = SpaceRequest.env().get("spacedog.test.sendpulse.client.secret");
		settings.authorizedRoles = Collections.singleton("user");
		test.settings().save(settings);

		// fred can get all websites
		SpaceRequest.get("/1/sendpulse/push/websites").userAuth(fred).go(200);

		// fred can create a web push to sendpulse.www.spacedog.io
		SpaceRequest.post("/1/sendpulse/push/tasks")//
				.userAuth(fred)//
				.formField("title", "Hello")//
				.formField("body", DateTime.now().toString())//
				.go(400)//
				.assertEquals("sendpulse-1255", "error.code");

		// fred can create a web push to sendpulse.www.spacedog.io
		SpaceRequest.post("/1/sendpulse/push/tasks")//
				.userAuth(fred)//
				.formField("title", "Hello")//
				.formField("body", DateTime.now().toString())//
				.formField("website_id", "22573")//
				.formField("ttl", "300")// 5 minutes
				.go(200);

		// settings forbid admins to get all websites nor create push tasks
		SpaceRequest.get("/1/sendpulse/push/websites").adminAuth(test).go(403);
		SpaceRequest.post("/1/sendpulse/push/tasks").adminAuth(test).go(403);
	}

	private void initSendPulseTestWebApp() {

		// prepare
		SpaceRequest.env().target(SpaceTarget.production);

		// if sendpulde test web app already initialized then return
		if (SpaceRequest.get("/1/file/www/index.html").backendId("sendpulse")//
				.go(200, 404).httpResponse().getStatus() == 200)
			return;

		SpaceDog sendpulse = resetBackend("sendpulse", "sendpulse", "hi sendpulse");

		// upload web site
		upload(sendpulse, "index.html");
		upload(sendpulse, "sp-push-manifest.json");
		upload(sendpulse, "sp-push-worker.js");
	}

	private void upload(SpaceDog backend, String name) {
		SpaceRequest.put("/1/file/www/" + name)//
				.bodyResource(this.getClass(), name)//
				.adminAuth(backend)//
				.go(200);
	}
}
