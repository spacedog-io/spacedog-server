/**
 * © David Attias 2015
 */
package io.spacedog.services;

import io.spacedog.server.JsonPayload;
import io.spacedog.server.Server;
import io.spacedog.server.SpaceResty;
import io.spacedog.services.credentials.CredentialsResty;
import net.codestory.http.annotations.Get;
import net.codestory.http.payload.Payload;

public class HealthCheckResty extends SpaceResty {

	//
	// Routes
	//

	@Get("")
	@Get("/")
	public Payload getPing() {
		if (elastic().exists(CredentialsResty.credentialsIndex()))
			return JsonPayload.ok()//
					.withContent(Server.get().info()).build();
		return Payload.notFound();
	}

	//
	// Singleton
	//

	private static HealthCheckResty singleton = new HealthCheckResty();

	public static HealthCheckResty get() {
		return singleton;
	}

	private HealthCheckResty() {
	}

}