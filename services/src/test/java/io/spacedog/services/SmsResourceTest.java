package io.spacedog.services;

import java.io.IOException;

import org.junit.Test;

import com.google.common.collect.Sets;

import io.spacedog.client.SpaceEnv;
import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceTest;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.SmsSettings;
import io.spacedog.utils.SmsSettings.TwilioSettings;

public class SmsResourceTest extends SpaceTest {

	@Test
	public void sendSms() throws IOException {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();

		// superadmin creates user vince with 'sms' role
		SpaceDog vince = signUp(test, "vince", "hi vince");
		setRole(test, vince, "sms");

		// no sms settings => nobody can send any sms
		SpaceRequest.post("/1/sms").backend(test).go(403);
		SpaceRequest.post("/1/sms").userAuth(vince).go(403);
		SpaceRequest.post("/1/sms").adminAuth(test).go(403);

		// superadmin sets the list of roles allowed to send sms
		SmsSettings settings = new SmsSettings();
		settings.rolesAllowedToSendSms = Sets.newHashSet("sms");
		test.settings().save(settings);

		// vince is now allowed to send sms
		// since he's got the 'sms' role
		// but he fails since no sms provider settings are set
		SpaceRequest.post("/1/sms").userAuth(vince).go(400);

		// only user with sms role are allowed to send sms
		// anonymous and admin fail to send sms
		SpaceRequest.post("/1/sms").backend(test).go(403);
		SpaceRequest.post("/1/sms").adminAuth(test).go(403);

		// superadmin sets twilio settings
		settings.twilio = new TwilioSettings();
		SpaceEnv configuration = SpaceRequest.env();
		settings.twilio.accountSid = configuration.get("spacedog.twilio.accountSid");
		settings.twilio.authToken = configuration.get("spacedog.twilio.authToken");
		settings.twilio.defaultFrom = configuration.get("spacedog.twilio.defaultFrom");
		test.settings().save(settings);

		// vince sends an sms
		String messageId = SpaceRequest.post("/1/sms").userAuth(vince)//
				.formField("To", "33662627520").formField("Body", "Hi from SpaceDog")//
				.go(200).getString("sid");

		// vince gets info on the previously sent sms
		// since he's got the 'sms' role
		SpaceRequest.get("/1/sms/" + messageId).userAuth(vince).go(200);

		// anonymous and superadmin don't have 'sms' role
		// they fail to get sms info
		SpaceRequest.get("/1/sms/" + messageId).backend(test).go(403);//
		SpaceRequest.get("/1/sms/" + messageId).adminAuth(test).go(403);//
	}
}
