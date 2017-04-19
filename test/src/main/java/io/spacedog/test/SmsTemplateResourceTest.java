package io.spacedog.test;

import java.io.IOException;
import java.util.Collections;

import org.junit.Test;

import com.google.common.collect.Maps;

import io.spacedog.model.Schema;
import io.spacedog.model.SmsSettings;
import io.spacedog.model.SmsSettings.TwilioSettings;
import io.spacedog.model.SmsTemplate;
import io.spacedog.rest.SpaceEnv;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;

public class SmsTemplateResourceTest extends SpaceTest {

	@Test
	public void sendTemplatedSms() throws IOException {

		// prepare
		prepareTest();
		SpaceDog test = SpaceDog.backend("test");
		SpaceDog superadmin = resetTestBackend();

		// superadmin creates user vince with 'sms' role
		SpaceDog vince = signUp(test, "vince", "hi vince");
		superadmin.credentials().setRole(vince.id(), "sms");

		// superadmin creates a customer schema
		Schema schema = Schema.builder("customer")//
				.text("name").string("phone").close().build();

		superadmin.schema().set(schema);

		// superadmin creates a customer
		String customerId = superadmin.data().object("customer")//
				.node("name", "David A.", "phone", "+33662627520").save().id();

		// prepare Twilio SMS settings
		SmsSettings settings = new SmsSettings();
		settings.twilio = new TwilioSettings();
		SpaceEnv env = SpaceEnv.defaultEnv();
		settings.twilio.accountSid = env.get("caremen.twilio.accountSid");
		settings.twilio.authToken = env.get("caremen.twilio.authToken");
		settings.twilio.defaultFrom = env.get("caremen.twilio.defaultFrom");

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
		superadmin.settings().save(settings);

		// nobody is allowed to send simple sms
		test.post("/1/sms").go(403);
		vince.post("/1/sms").go(403);
		superadmin.post("/1/sms").go(403);

		// vince sends 'hello' templated SMS
		vince.sms().sendTemplated("hello", "customer", customerId);

		// anonymous and superadmin don't have the 'sms' role
		// they are not allowed to use the hello SMS template
		test.post("/1/sms/template/hello").go(403);
		superadmin.post("/1/sms/template/hello").go(403);
	}

}
