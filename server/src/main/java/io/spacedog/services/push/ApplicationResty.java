/**
 * © David Attias 2015
 */
package io.spacedog.services.push;

import java.util.List;

import io.spacedog.client.push.PushApplication;
import io.spacedog.client.push.PushProtocol;
import io.spacedog.services.JsonPayload;
import io.spacedog.services.Server;
import io.spacedog.services.Services;
import io.spacedog.services.SpaceResty;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;

@Prefix("/2/push/applications")
public class ApplicationResty extends SpaceResty {

	@Get("")
	@Get("/")
	public Payload getApplications() {
		Server.context().credentials().checkAtLeastAdmin();
		List<PushApplication> pushApps = Services.push().listApps();
		return JsonPayload.ok().withContent(pushApps).build();
	}

	@Put("/:name/:protocol")
	@Put("/:name/:protocol/")
	public Payload putApplication(String name, PushProtocol protocol, //
			PushApplication.Credentials credentials) {

		Server.context().credentials().checkAtLeastAdmin();
		Services.push().saveApp(name, protocol, credentials);
		return JsonPayload.ok().build();
	}

	@Delete("/:name/:protocol")
	@Delete("/:name/:protocol/")
	public Payload deleteApplication(String name, PushProtocol protocol) {

		Server.context().credentials().checkAtLeastSuperAdmin();
		Services.push().deleteApp(name, protocol);
		return JsonPayload.ok().build();
	}

}
