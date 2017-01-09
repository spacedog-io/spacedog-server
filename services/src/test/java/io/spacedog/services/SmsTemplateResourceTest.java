package io.spacedog.services;

import java.io.IOException;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Maps;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceClient.Backend;
import io.spacedog.client.SpaceClient.User;
import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceEnv;
import io.spacedog.utils.Schema;
import io.spacedog.utils.SmsSettings;
import io.spacedog.utils.SmsSettings.TwilioSettings;
import io.spacedog.utils.SmsTemplate;

public class SmsTemplateResourceTest extends Assert {

	@Test
	public void sendTemplatedSms() throws IOException {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();

		// superadmin creates user vince with 'sms' role
		User vince = SpaceClient.signUp(test, "vince", "hi vince");
		SpaceClient.setRole(test.adminUser, vince, "sms");

		// superadmin creates a customer schema
		Schema schema = Schema.builder("customer")//
				.text("name").string("phone").close().build();

		SpaceClient.setSchema(schema, test);

		// superadmin creates a customer
		String customerId = SpaceRequest.post("/1/data/customer").adminAuth(test)//
				.body("name", "David A.", "phone", "+33662627520").go(201).getString("id");

		// prepare Twilio SMS settings
		SmsSettings settings = new SmsSettings();
		settings.twilio = new TwilioSettings();
		SpaceEnv configuration = SpaceRequest.env();
		settings.twilio.accountSid = configuration.get("spacedog.twilio.accountSid");
		settings.twilio.authToken = configuration.get("spacedog.twilio.authToken");
		settings.twilio.defaultFrom = configuration.get("spacedog.twilio.defaultFrom");

		// prepare 'hello' SMS template
		SmsTemplate template = new SmsTemplate();
		template.to = "{{customer.phone}}";
		template.body = "Hello {{customer.name}}";
		template.model = Maps.newHashMap();
		template.model.put("customer", "customer");
		template.roles = Collections.singleton("sms");
		settings.templates = Maps.newHashMap();
		settings.templates.put("hello", template);

		// superadmin saves SMS settings
		SpaceClient.saveSettings(test, settings);

		// nobody is allowed to send simple sms
		SpaceRequest.post("/1/sms").backend(test).go(403);
		SpaceRequest.post("/1/sms").userAuth(vince).go(403);
		SpaceRequest.post("/1/sms").adminAuth(test).go(403);

		// vince sends 'hello' templated SMS
		SpaceRequest.post("/1/sms/template/hello").userAuth(vince)//
				.body("customer", customerId).go(200);

		// anonymous and superadmin don't have the 'sms' role
		// they are not allowed to use the hello SMS template
		SpaceRequest.post("/1/sms/template/hello").backend(test).go(403);
		SpaceRequest.post("/1/sms/template/hello").adminAuth(test).go(403);
	}

}
