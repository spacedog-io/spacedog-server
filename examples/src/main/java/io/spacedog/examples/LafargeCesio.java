/**
 * © David Attias 2015
 */
package io.spacedog.examples;

import org.junit.Test;

import io.spacedog.client.SpaceDog;
import io.spacedog.http.SpaceBackend;
import io.spacedog.http.SpaceEnv;
import io.spacedog.model.MailSettings;
import io.spacedog.model.MailSettings.SmtpSettings;
import io.spacedog.test.SpaceTest;

public class LafargeCesio extends SpaceTest {

	@Test
	public void initBackend() {

		SpaceEnv env = SpaceEnv.defaultEnv();
		env.target(SpaceBackend.production);

		SpaceDog backend = SpaceDog.backendId("cesio")//
				.username(env.getOrElseThrow("spacedog.cesio.superadmin.username"))//
				.password(env.getOrElseThrow("spacedog.cesio.superadmin.password"));

		// resetBackend(backend);
		// resetSchema(LafargeCesioResource.playerSchema(), backend);

		MailSettings settings = new MailSettings();
		settings.enableUserFullAccess = false;
		settings.smtp = new SmtpSettings();
		settings.smtp.startTlsRequired = true;
		settings.smtp.sslOnConnect = true;
		settings.smtp.host = env.getOrElseThrow("spacedog.cesio.smtp.host");
		settings.smtp.login = env.getOrElseThrow("spacedog.cesio.smtp.login");
		settings.smtp.password = env.getOrElseThrow("spacedog.cesio.smtp.password");

		backend.settings().save(settings);
	}
}
