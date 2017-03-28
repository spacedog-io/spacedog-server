package io.spacedog.test;

import java.io.IOException;
import java.util.Collections;

import org.junit.Test;

import com.google.common.collect.Maps;

import io.spacedog.model.Schema;
import io.spacedog.model.SmsSettings;
import io.spacedog.model.SmsTemplate;
import io.spacedog.model.SmsSettings.TwilioSettings;
import io.spacedog.rest.SpaceEnv;
import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;

public class SmsTemplateResourceTest extends SpaceTest {

	@Test
	public void sendTemplatedSms() throws IOException {

		// prepare
		prepareTest();
		SpaceDog test = resetTestBackend();

		// superadmin creates user vince with 'sms' role
		SpaceDog vince = signUp(test, "vince", "hi vince");
		setRole(test, vince, "sms");

		// superadmin creates a customer schema
		Schema schema = Schema.builder("customer")//
				.text("name").string("phone").close().build();

		test.schema().set(schema);

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
		test.settings().save(settings);

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
