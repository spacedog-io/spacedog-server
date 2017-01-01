package io.spacedog.services;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Sets;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceClient.Backend;
import io.spacedog.client.SpaceClient.User;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.SmsSettings;
import io.spacedog.utils.SmsSettings.TwilioSettings;

public class SmsResourceTest extends Assert {

	@Test
	public void sendSms() throws IOException {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();

		// superadmin creates user vince with 'sms' role
		User vince = SpaceClient.signUp(test, "vince", "hi vince");
		SpaceClient.setRole(test.adminUser, vince, "sms");

		// no sms settings => nobody can send any sms
		SpaceRequest.post("/1/sms").backend(test).go(403);
		SpaceRequest.post("/1/sms").userAuth(vince).go(403);
		SpaceRequest.post("/1/sms").adminAuth(test).go(403);

		// superadmin sets the list of roles allowed to send sms
		SmsSettings settings = new SmsSettings();
		settings.rolesAllowedToSendSms = Sets.newHashSet("sms");
		SpaceClient.saveSettings(test, settings);

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
		settings.twilio.accountSid = SpaceRequest.configuration().getProperty("twilio.accountSid");
		settings.twilio.authToken = SpaceRequest.configuration().getProperty("twilio.authToken");
		settings.twilio.defaultFrom = SpaceRequest.configuration().getProperty("twilio.defaultFrom");
		SpaceClient.saveSettings(test, settings);

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
