package io.spacedog.test;

import java.io.IOException;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.http.SpaceEnv;
import io.spacedog.client.http.SpaceRequestException;
import io.spacedog.client.schema.Schema;
import io.spacedog.client.sms.SmsSettings;
import io.spacedog.client.sms.SmsSettings.TwilioSettings;
import io.spacedog.client.sms.SmsTemplate;
import io.spacedog.client.sms.SmsTemplateRequest;
import io.spacedog.utils.Json;

public class SmsRestyTest extends SpaceTest {

	@Test
	public void sendSmsBasicRequest() throws IOException {

		// prepare
		prepareTest();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearServer();
		SpaceDog vince = createTempDog(superadmin, "vince");

		// nobody is authorized to send sms
		// since no authorized role defined
		assertHttpError(401, () -> guest.sms().send("?", "?"));
		assertHttpError(403, () -> vince.sms().send("?", "?"));
		assertHttpError(403, () -> superadmin.sms().send("?", "?"));

		// superadmin authorizes users with 'sms' role to send sms
		SmsSettings settings = new SmsSettings();
		settings.authorizedRoles = Sets.newHashSet("sms");
		superadmin.settings().save(settings);

		// nobody is authorized to send sms
		// since nobody has the right role
		assertHttpError(401, () -> guest.sms().send("?", "?"));
		assertHttpError(403, () -> vince.sms().send("?", "?"));
		assertHttpError(403, () -> superadmin.sms().send("?", "?"));

		// superadmin gives vince the sms role
		superadmin.credentials().setRole(vince.id(), "sms");

		// vince is now authorized but no sms provider set
		assertHttpError(400, () -> vince.sms().send("?", "?"));

		// superadmin sets twilio settings
		settings.twilio = twilioSettings();
		superadmin.settings().save(settings);

		// since sends an sms
		String messageId = vince.sms().send("33662627520", "Hi from Vince");

		// vince gets info on the previously sent sms
		// since he's got the 'sms' role
		vince.sms().get(messageId);

		// superadmin is not authorized to get sms info
		assertHttpError(403, () -> superadmin.sms().get(messageId));

		// vince sends an sms to invalid mobile number
		SpaceRequestException exception = assertHttpError(400, //
				() -> vince.sms().send("33162627520", "Hi from SpaceDog"));
		Assert.assertEquals("twilio:21614", exception.serverErrorCode());
	}

	@Test
	public void senddSmsTemplateRequest() throws IOException {

		// prepare
		prepareTest();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearServer();

		// superadmin creates user vince with 'sms' role
		SpaceDog vince = createTempDog(superadmin, "vince");
		superadmin.credentials().setRole(vince.id(), "sms");

		// superadmin creates a customer schema
		Schema schema = Schema.builder("customer")//
				.text("name").keyword("phone").build();
		superadmin.schemas().set(schema);

		// superadmin creates a customer
		String customerId = superadmin.data().save("customer", //
				Json.object("name", "David A.", "phone", "+33662627520")).id();

		// superadmin sets Twilio sms settings
		SmsSettings settings = new SmsSettings();
		settings.twilio = twilioSettings();
		superadmin.settings().save(settings);

		// superadmin saves 'hello' sms template
		SmsTemplate template = new SmsTemplate();
		template.name = "hello";
		template.to = "{{customer.phone}}";
		template.body = "Hello {{customer.name}}";
		template.model = Maps.newHashMap();
		template.model.put("customer", "customer");
		template.roles = Collections.singleton("sms");
		superadmin.sms().saveTemplate(template);

		// nobody is allowed to send simple sms
		assertHttpError(401, () -> guest.sms().send("?", "?"));
		assertHttpError(403, () -> vince.sms().send("?", "?"));
		assertHttpError(403, () -> superadmin.sms().send("?", "?"));

		// vince sends request to 'hello' sms template
		SmsTemplateRequest request = new SmsTemplateRequest();
		request.templateName = template.name;
		request.parameters = Maps.newHashMap();
		request.parameters.put("customer", customerId);
		vince.sms().send(request);

		// anonymous and superadmin don't have the 'sms' role,
		// they are not allowed to use the 'hello' sms template
		assertHttpError(401, () -> guest.sms().send(request));
		assertHttpError(403, () -> superadmin.sms().send(request));

		// superadmin deletes 'hello' sms template
		superadmin.sms().deleteTemplate(template.name);

		// vince can not send sms request to 'hello' template
		// since template is gone
		assertHttpError(404, () -> vince.sms().send(request));
	}

	private TwilioSettings twilioSettings() {
		TwilioSettings settings = new TwilioSettings();
		SpaceEnv env = SpaceEnv.env();
		settings.accountSid = env.getOrElseThrow("caremen.twilio.accountSid");
		settings.authToken = env.getOrElseThrow("caremen.twilio.authToken");
		settings.defaultFrom = env.getOrElseThrow("caremen.twilio.defaultFrom");
		return settings;
	}

}
