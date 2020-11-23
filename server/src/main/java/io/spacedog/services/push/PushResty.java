package io.spacedog.services.push;

import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.data.DataWrap;
import io.spacedog.client.push.Installation;
import io.spacedog.client.push.PushRequest;
import io.spacedog.client.push.PushResponse;
import io.spacedog.client.push.PushSettings;
import io.spacedog.services.JsonPayload;
import io.spacedog.services.Server;
import io.spacedog.services.Services;
import io.spacedog.services.SpaceResty;
import net.codestory.http.Context;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;

@Prefix("/2")
public class PushResty extends SpaceResty {

	@Post("/push/installations")
	@Post("/push/installations/")
	@Post("/data/installation")
	@Post("/data/installation/")
	public Payload post(Installation installation, Context context) {
		DataWrap<Installation> wrap = Services.push().saveInstallationIfAuthorized(installation);
		return JsonPayload.saved(wrap).build();
	}

	@Put("/push/installations/:id")
	@Put("/push/installations/:id/")
	@Put("/data/installation/:id")
	@Put("/data/installation/:id/")
	public Payload put(String id, Installation installation, Context context) {
		DataWrap<Installation> wrap = DataWrap.wrap(installation).id(id)//
				.version(context.query().get(VERSION_PARAM));
		wrap = Services.push().saveInstallationIfAuthorized(wrap);
		return JsonPayload.saved(wrap).build();
	}

	/**
	 * Check this page for specific json messages:
	 * http://docs.aws.amazon.com/sns/latest/dg/mobile-push-send-custommessage. html
	 */
	@Post("/push")
	@Post("/push/")
	public PushResponse postPushRequest(PushRequest request, Context context) {
		PushSettings settings = Services.push().settings();
		Credentials credentials = Server.context().credentials();
		credentials.checkRoleAccess(settings.authorizedRoles);
		return Services.push().push(request);
	}

}
