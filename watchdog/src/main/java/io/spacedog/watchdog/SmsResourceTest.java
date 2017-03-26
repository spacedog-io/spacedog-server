package io.spacedog.watchdog;

import java.io.IOException;

import org.junit.Test;

import com.google.common.collect.Sets;

import io.spacedog.model.SmsSettings;
import io.spacedog.model.SmsSettings.TwilioSettings;
import io.spacedog.rest.SpaceEnv;
import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;

public class SmsResourceTest extends SpaceTest {

	@Test
	public void sendSms() throws IOException {

		// prepare
		prepareTest();
		SpaceDog guest = SpaceDog.backend("test");
		SpaceDog test = resetTestBackend();

		// superadmin creates user vince with 'sms' role
		SpaceDog vince = signUp(test, "vince", "hi vince");
		test.credentials().setRole(vince.id(), "sms");

		// no sms settings => nobody can send any sms
		guest.post("/1/sms").go(403);
		vince.post("/1/sms").go(403);
		test.post("/1/sms").go(403);

		// superadmin sets the list of roles allowed to send sms
		SmsSettings settings = new SmsSettings();
		settings.rolesAllowedToSendSms = Sets.newHashSet("sms");
		test.settings().save(settings);

		// vince is now allowed to send sms
		// since he's got the 'sms' role
		// but he fails since no sms provider settings are set
		vince.post("/1/sms").go(400);

		// only user with sms role are allowed to send sms
		// anonymous and admin fail to send sms
		guest.post("/1/sms").go(403);
		test.post("/1/sms").go(403);

		// superadmin sets twilio settings
		settings.twilio = new TwilioSettings();
		SpaceEnv configuration = SpaceRequest.env();
		settings.twilio.accountSid = configuration.get("spacedog.twilio.accountSid");
		settings.twilio.authToken = configuration.get("spacedog.twilio.authToken");
		settings.twilio.defaultFrom = configuration.get("spacedog.twilio.defaultFrom");
		test.settings().save(settings);

		// vince sends an sms
		String messageId = vince.post("/1/sms").formField("To", "33662627520")//
				.formField("Body", "Hi from SpaceDog").go(200).getString("sid");

		// vince gets info on the previously sent sms
		// since he's got the 'sms' role
		vince.get("/1/sms/" + messageId).go(200);

		// anonymous and superadmin don't have 'sms' role
		// they fail to get sms info
		guest.get("/1/sms/" + messageId).go(403);//
		test.get("/1/sms/" + messageId).go(403);//

		// vince sends an sms to invalid mobile number
		vince.post("/1/sms").formField("To", "33162627520")//
				.formField("Body", "Hi from SpaceDog").go(200)//
				.assertEquals(400, "status")//
				.assertEquals("twilio:21614", "error.code");
	}
}
