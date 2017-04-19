/**
 * © David Attias 2015
 */
package io.spacedog.test.sendpulse;

import java.util.Collections;

import org.joda.time.DateTime;

import io.spacedog.model.SendPulseSettings;
import io.spacedog.rest.SpaceEnv;
import io.spacedog.rest.SpaceRequestException;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.Utils;

public class SendPulseResourceTest extends SpaceTest {

	// @Test
	public void testSendPulseResource() {

		// prepare
		prepareTest();

		// this must come before preparing the test
		// or else the test flag is set
		// and the sendpulse backend files are not reset
		initSendPulseTestWebApp();

		// reset test backend
		SpaceDog superadmin = resetTestBackend();
		SpaceDog fred = signUp("test", "fred", "hi fred");
		SpaceDog vince = signUp("test", "vince", "hi vince");
		superadmin.credentials().setRole(fred.id(), "pusher");

		// set sendpulse test settings
		SendPulseSettings settings = new SendPulseSettings();
		settings.clientId = SpaceEnv.defaultEnv()//
				.get("spacedog.test.sendpulse.client.id");
		settings.clientSecret = SpaceEnv.defaultEnv()//
				.get("spacedog.test.sendpulse.client.secret");
		settings.authorizedRoles = Collections.singleton("pusher");
		superadmin.settings().save(settings);

		// fred can get all websites
		fred.get("/1/sendpulse/push/websites").go(200);

		// fred fails to web push to sendpulse.www.spacedog.io
		// since no website_id parameter
		fred.post("/1/sendpulse/push/tasks")//
				.formField("title", "Hello")//
				.formField("body", DateTime.now().toString())//
				.go(400)//
				.assertEquals("sendpulse-1255", "error.code");

		// fred web pushes to sendpulse.www.spacedog.io
		fred.post("/1/sendpulse/push/tasks")//
				.formField("title", "Hello")//
				.formField("body", DateTime.now().toString())//
				.formField("website_id", "22573")//
				.formField("ttl", "300")// 5 minutes
				.go(200);

		// vince fails to get all websites nor push tasks
		// since he hasn't got the 'pusher' role
		vince.get("/1/sendpulse/push/websites").go(403);
		vince.post("/1/sendpulse/push/tasks").go(403);
	}

	private void initSendPulseTestWebApp() {

		// prepare
		String password = SpaceEnv.defaultEnv()//
				.get("spacedog.test.sendpulse.backend.password");
		SpaceDog sendpulse = SpaceDog.backend("https://sendpulse.spacedog.io")//
				.username("sendpulse");

		try {
			sendpulse.login(password);
		} catch (SpaceRequestException e) {
			if (e.httpStatus() == 401) {
				sendpulse.admin().createBackend(//
						"sendpulse", password, "platform@spacedog.io", false);
				sendpulse.login(password);
			} else
				throw e;
		}

		// upload web site
		upload(sendpulse, "index.html");
		upload(sendpulse, "sp-push-manifest.json");
		upload(sendpulse, "sp-push-worker.js");
	}

	private void upload(SpaceDog dog, String name) {
		dog.file().save("/www/" + name, Utils.readResource(this.getClass(), name));
	}
}
