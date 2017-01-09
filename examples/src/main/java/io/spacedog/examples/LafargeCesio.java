/**
 * © David Attias 2015
 */
package io.spacedog.examples;

import org.junit.Test;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceEnv;
import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceTarget;
import io.spacedog.utils.MailSettings;
import io.spacedog.utils.MailSettings.SmtpSettings;

public class LafargeCesio extends SpaceClient {

	@Test
	public void initBackend() {

		SpaceRequest.env().target(SpaceTarget.production);

		Backend backend = new Backend(//
				"cesio", SpaceEnv.defaultEnv().get("spacedog.cesio.superadmin.username"), //
				SpaceEnv.defaultEnv().get("spacedog.cesio.superadmin.password"), //
				"david@spacedog.io");

		// resetBackend(backend);
		// resetSchema(LafargeCesioResource.playerSchema(), backend);

		MailSettings settings = new MailSettings();
		settings.enableUserFullAccess = false;
		settings.smtp = new SmtpSettings();
		settings.smtp.startTlsRequired = true;
		settings.smtp.sslOnConnect = true;
		settings.smtp.host = SpaceRequest.env()//
				.get("spacedog.cesio.smtp.host");
		settings.smtp.login = SpaceRequest.env()//
				.get("spacedog.cesio.smtp.login");
		settings.smtp.password = SpaceRequest.env()//
				.get("spacedog.cesio.smtp.password");

		SpaceClient.saveSettings(backend, settings);
	}
}
