/**
 * © David Attias 2015
 */
package io.spacedog.services;

import io.spacedog.server.JsonPayload;
import io.spacedog.server.Server;
import io.spacedog.server.SpaceService;
import io.spacedog.services.credentials.CredentialsService;
import net.codestory.http.annotations.Get;
import net.codestory.http.payload.Payload;

public class HealthCheckService extends SpaceService {

	//
	// Routes
	//

	@Get("")
	@Get("/")
	public Payload getPing() {
		if (elastic().exists(CredentialsService.credentialsIndex()))
			return JsonPayload.ok()//
					.withContent(Server.get().info()).build();
		return Payload.notFound();
	}

	//
	// Singleton
	//

	private static HealthCheckService singleton = new HealthCheckService();

	public static HealthCheckService get() {
		return singleton;
	}

	private HealthCheckService() {
	}

}