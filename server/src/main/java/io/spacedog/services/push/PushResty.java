package io.spacedog.services.push;

import org.elasticsearch.common.lucene.uid.Versions;

import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.data.DataWrap;
import io.spacedog.client.push.Installation;
import io.spacedog.client.push.PushRequest;
import io.spacedog.client.push.PushResponse;
import io.spacedog.client.push.PushSettings;
import io.spacedog.server.JsonPayload;
import io.spacedog.server.Server;
import io.spacedog.server.Services;
import io.spacedog.server.SpaceResty;
import net.codestory.http.Context;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;

@Prefix("/1")
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
				.version(context.query().getLong(VERSION_PARAM, Versions.MATCH_ANY));
		wrap = Services.push().saveInstallationIfAuthorized(wrap);
		return JsonPayload.saved(wrap).build();
	}

	/**
	 * Check this page for specific json messages:
	 * http://docs.aws.amazon.com/sns/latest/dg/mobile-push-send-custommessage. html
	 */
	@Post("/push")
	@Post("/push/")
	public Payload postPushRequest(PushRequest request, Context context) {

		PushSettings settings = Services.settings().get(PushSettings.class).get();
		Credentials credentials = Server.context().credentials();
		credentials.checkIfAuthorized(settings.authorizedRoles);

		PushResponse response = Services.push().push(request);
		return JsonPayload.ok().withContent(response).build();
	}

}
