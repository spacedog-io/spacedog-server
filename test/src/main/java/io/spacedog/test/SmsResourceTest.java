package io.spacedog.test;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Sets;

import io.spacedog.model.SmsSettings;
import io.spacedog.model.SmsSettings.TwilioSettings;
import io.spacedog.rest.SpaceEnv;
import io.spacedog.rest.SpaceRequestException;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;

public class SmsResourceTest extends SpaceTest {

	@Test
	public void sendSms() throws IOException {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();
		SpaceDog guest = SpaceDog.backend(test);

		// superadmin creates user vince with 'sms' role
		SpaceDog vince = signUp(test, "vince", "hi vince");
		test.credentials().setRole(vince.id(), "sms");

		// no sms settings => nobody can send any sms
		guest.post("/1/sms").go(401);
		vince.post("/1/sms").go(403);
		test.post("/1/sms").go(403);

		// superadmin sets the list of roles allowed to send sms
		SmsSettings settings = new SmsSettings();
		settings.rolesAllowedToSendSms = Sets.newHashSet("sms");
		test.settings().save(settings);

		// vince is now allowed to send sms
		// since he's got the 'sms' role
		// but he fails since no sms provider settings are set
		AssertFails.assertHttpStatus(400, () -> vince.sms().send("?", "?"));
		vince.post("/1/sms").go(400);

		// only user with sms role are allowed to send sms
		// anonymous and admin fail to send sms
		AssertFails.assertHttpStatus(401, () -> guest.sms().send("?", "?"));
		AssertFails.assertHttpStatus(403, () -> test.sms().send("?", "?"));
		// guest.post("/1/sms").go(403);
		// test.post("/1/sms").go(403);

		// superadmin sets twilio settings
		SpaceEnv env = SpaceEnv.defaultEnv();
		settings.twilio = new TwilioSettings();
		settings.twilio.accountSid = env.get("caremen.twilio.accountSid");
		settings.twilio.authToken = env.get("caremen.twilio.authToken");
		settings.twilio.defaultFrom = env.get("caremen.twilio.defaultFrom");
		test.settings().save(settings);

		// vince sends an sms
		String messageId = vince.sms().send("33662627520", "Hi from SpaceDog");

		// vince gets info on the previously sent sms
		// since he's got the 'sms' role
		vince.sms().get(messageId);

		// anonymous and superadmin don't have 'sms' role
		// they fail to get sms info
		AssertFails.assertHttpStatus(401, () -> guest.sms().get(messageId));
		AssertFails.assertHttpStatus(403, () -> test.sms().get(messageId));
		// guest.get("/1/sms/" + messageId).go(403);//
		// test.get("/1/sms/" + messageId).go(403);//

		// vince sends an sms to invalid mobile number
		SpaceRequestException exception = AssertFails.assertHttpStatus(400, //
				() -> vince.sms().send("33162627520", "Hi from SpaceDog"));
		Assert.assertEquals("twilio:21614", exception.serverErrorCode());
	}
}
