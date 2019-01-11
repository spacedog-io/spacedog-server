/**
 * © David Attias 2015
 */
package io.spacedog.services.admin;

import io.spacedog.server.Server;
import io.spacedog.server.Services;
import io.spacedog.server.SpaceResty;
import io.spacedog.services.JsonPayload;
import net.codestory.http.annotations.Get;
import net.codestory.http.payload.Payload;

public class HealthCheckResty extends SpaceResty {

	@Get("")
	@Get("/")
	public Payload getPing() {
		if (elastic().exists(Services.credentials().index()))
			return JsonPayload.ok()//
					.withContent(Server.get().info())//
					.build();

		return Payload.notFound();
	}

}