/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Collections;

import org.joda.time.DateTime;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceTarget;
import io.spacedog.utils.SendPulseSettings;

public class SendPulseResourceTest extends SpaceClient {

	public void testSendPulseResource() {

		// this must come before preparing the test
		// or else the test flag is set
		// and the sendpulse backend files are not reset
		initSendPulseTestWebApp();

		// prepare
		SpaceClient.prepareTest();
		SpaceRequest.env().target(SpaceTarget.local);
		Backend test = SpaceClient.resetTestBackend();
		User fred = SpaceClient.signUp(test, "fred", "hi fred");

		// set sendpulse test settings
		SendPulseSettings settings = new SendPulseSettings();
		settings.clientId = SpaceRequest.env().get("spacedog.test.sendpulse.client.id");
		settings.clientSecret = SpaceRequest.env().get("spacedog.test.sendpulse.client.secret");
		settings.authorizedRoles = Collections.singleton("user");
		SpaceClient.saveSettings(test, settings);

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
		Backend sendpulse = new Backend("sendpulse", "sendpulse", "hi sendpulse", "david@spacedog.io");

		// if sendpulde test web app already initialized then return
		if (SpaceRequest.get("/1/file/www/index.html").backend(sendpulse)//
				.go(200, 404).httpResponse().getStatus() == 200)
			return;

		sendpulse = SpaceClient.resetBackend(sendpulse);

		// upload web site
		upload(sendpulse, "index.html");
		upload(sendpulse, "sp-push-manifest.json");
		upload(sendpulse, "sp-push-worker.js");
	}

	private void upload(Backend backend, String name) {
		SpaceRequest.put("/1/file/www/" + name)//
				.bodyResource("io/spacedog/services/sendpulse/" + name)//
				.adminAuth(backend)//
				.go(200);
	}
}
