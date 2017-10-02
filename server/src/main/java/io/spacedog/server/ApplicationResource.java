/**
 * © David Attias 2015
 */
package io.spacedog.server;

import java.util.Optional;

import org.elasticsearch.common.Strings;

import com.amazonaws.services.sns.model.CreatePlatformApplicationRequest;
import com.amazonaws.services.sns.model.DeletePlatformApplicationRequest;
import com.amazonaws.services.sns.model.PlatformApplication;

import io.spacedog.model.PushService;
import io.spacedog.utils.Json;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;

public class ApplicationResource extends Resource {

	//
	// Routes
	//

	@Get("/1/applications")
	@Get("/1/applications/")
	public Payload getApplications() {
		SpaceContext.credentials().checkAtLeastAdmin();
		return JsonPayload.ok().build();
	}

	private static class ApplicationPushCredentials {
		public String principal;
		public String credentials;
	}

	@Put("/1/applications/:name/:pushService")
	@Put("/1/applications/:name/:pushService/")
	public Payload putApplication(String name, String pushService, String body) {

		SpaceContext.credentials().checkAtLeastAdmin();

		ApplicationPushCredentials credentials = Json.toPojo(//
				body, ApplicationPushCredentials.class);

		CreatePlatformApplicationRequest request = new CreatePlatformApplicationRequest()//
				.withName(toApplicationId(name))//
				.withPlatform(pushService);

		request.addAttributesEntry("PlatformCredential", credentials.credentials);

		if (!Strings.isNullOrEmpty(credentials.principal))
			request.addAttributesEntry("PlatformPrincipal", credentials.principal);

		AwsSnsPusher.getSnsClient().createPlatformApplication(request);

		return JsonPayload.ok().build();
	}

	@Delete("/1/applications/:name/:pushService")
	@Delete("/1/applications/:name/:pushService/")
	public Payload deleteApplication(String name, String pushService) {

		SpaceContext.credentials().checkAtLeastAdmin();

		Optional<PlatformApplication> application = AwsSnsPusher.getApplication(//
				toApplicationId(name), PushService.valueOf(pushService));

		if (application.isPresent()) {
			String applicationArn = application.get().getPlatformApplicationArn();

			AwsSnsPusher.getSnsClient().deletePlatformApplication(//
					new DeletePlatformApplicationRequest()//
							.withPlatformApplicationArn(applicationArn));
		}

		return JsonPayload.ok().build();
	}

	//
	// Implementation
	//

	private String toApplicationId(String name) {
		return SpaceContext.backendId() + '-' + name;
	}

	//
	// Singleton
	//

	private static ApplicationResource singleton = new ApplicationResource();

	static ApplicationResource get() {
		return singleton;
	}

	private ApplicationResource() {
	}
}
