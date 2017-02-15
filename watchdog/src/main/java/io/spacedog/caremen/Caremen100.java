/**
 * © David Attias 2015
 */
package io.spacedog.caremen;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceEnv;
import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceTarget;
import io.spacedog.client.SpaceTest;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.Json;
import io.spacedog.utils.SettingsSettings;
import io.spacedog.utils.SettingsSettings.SettingsAcl;

public class Caremen100 extends SpaceTest {

	static final SpaceDog DEV;
	static final SpaceDog RECETTE;
	static final SpaceDog PRODUCTION;

	static {
		String testPassword = SpaceEnv.defaultEnv().get("caremen_test_superadmin_password");
		String prodPassword = SpaceEnv.defaultEnv().get("caremen_prod_superadmin_password");

		DEV = SpaceDog.backend("caredev").username("caredev")//
				.password(testPassword).email("platform@spacedog.io");
		RECETTE = SpaceDog.backend("carerec").username("carerec")//
				.password(testPassword).email("platform@spacedog.io");
		PRODUCTION = SpaceDog.backend("caremen").username("caremen")//
				.password(prodPassword).email("platform@spacedog.io");
	}

	private SpaceDog backend;

	@Test
	public void initCaremenBackend() throws IOException {

		backend = DEV;
		SpaceRequest.env().target(SpaceTarget.production);

		initAppConfigurationSettings();
		initFlatRatePrices();
		initSettingsSettings();
	}

	void initAppConfigurationSettings() {
		AppConfigurationSettings settings = backend.settings()//
				.get(AppConfigurationSettings.class);
		settings.backendVersion = "1.0.0";
		backend.settings().save(settings);
	}

	void initSettingsSettings() {
		SettingsSettings settings = backend.settings().get(SettingsSettings.class);

		// remove obsolete appcustomer settings
		settings.remove("appcustomer");

		// add new flatrateprices settings
		SettingsAcl acl = new SettingsAcl();
		acl.read("operator", "key", "admin", "user");
		acl.update("operator");
		settings.put("flatrateprices", acl);

		backend.settings().save(settings);
	}

	void initFlatRatePrices() throws IOException {

		ObjectNode classic = Json.object("ORY", 40, "CDG", 45, "LBG", 45);
		ObjectNode premium = Json.object("ORY", 60, "CDG", 70, "LBG", 70);
		ObjectNode green = Json.object("ORY", 80, "CDG", 90, "LBG", 90);
		ObjectNode breakk = Json.object("ORY", 40, "CDG", 45, "LBG", 45);
		ObjectNode van = Json.object("ORY", 60, "CDG", 70, "LBG", 70);
		ObjectNode settings = Json.object("classic", classic, "premium", premium, //
				"green", green, "break", breakk, "van", van);

		backend.settings().save("flatrateprices", settings);
	}

}
