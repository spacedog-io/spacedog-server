/**
 * © David Attias 2015
 */
package io.spacedog.examples;

import org.junit.Test;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceTarget;
import io.spacedog.services.LafargeCesioResource;

public class LafargeCesio extends SpaceClient {

	@Test
	public void initBackend() {

		SpaceRequest.configuration().target(SpaceTarget.production);

		Backend backend = new Backend(//
				"cesio", SpaceRequest.configuration().cesioSuperAdminUsername(), //
				SpaceRequest.configuration().cesioSuperAdminPassword(), //
				"david@spacedog.io");

		resetBackend(backend);
		resetSchema(LafargeCesioResource.playerSchema(), backend);
	}
}
