package io.spacedog.caremen;

import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.spacedog.client.SpaceEnv;
import io.spacedog.client.SpaceTarget;
import io.spacedog.sdk.DataObject;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Json;

public class FixCarerecDrivers {

	@Test
	public void run() {

		SpaceEnv env = SpaceEnv.defaultEnv();
		env.target(SpaceTarget.production);

		SpaceDog carerec = SpaceDog.backend("carerec").username("carerec")//
				.login(env.get("caremen_test_superadmin_password"));

		List<DataObject> drivers = carerec.dataEndpoint().getAllRequest()//
				.type("driver").size(20).load();

		for (DataObject driver : drivers) {

			JsonNode credentialsId = driver.node().get("credentialsId");

			if (Json.isNull(credentialsId))
				driver.delete();
			else {
				Credentials driverCredentials = carerec.credentials().get(//
						credentialsId.asText());

				if (!driverCredentials.enabled()) {
					driver.node().put("status", "disabled");
					driver.save();
				}
			}
		}
	}

}
