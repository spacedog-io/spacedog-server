/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Optional;

import org.elasticsearch.common.Strings;

import com.amazonaws.services.sns.model.CreatePlatformApplicationRequest;
import com.amazonaws.services.sns.model.DeletePlatformApplicationRequest;
import com.amazonaws.services.sns.model.PlatformApplication;

import io.spacedog.core.Json8;
import io.spacedog.model.PushService;
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
		SpaceContext.checkAdminCredentials();
		return JsonPayload.success();
	}

	private static class ApplicationPushCredentials {
		public String principal;
		public String credentials;
	}

	@Put("/1/applications/:name/:pushService")
	@Put("/1/applications/:name/:pushService/")
	public Payload putApplication(String name, String pushService, String body) {

		String backendId = SpaceContext.checkAdminCredentials().backendId();

		ApplicationPushCredentials credentials = Json8.readObject(//
				body, ApplicationPushCredentials.class);

		CreatePlatformApplicationRequest request = new CreatePlatformApplicationRequest()//
				.withName(toApplicationId(backendId, name))//
				.withPlatform(pushService);

		request.addAttributesEntry("PlatformCredential", credentials.credentials);

		if (!Strings.isNullOrEmpty(credentials.principal))
			request.addAttributesEntry("PlatformPrincipal", credentials.principal);

		PushResource.get().getSnsClient().createPlatformApplication(request);

		return JsonPayload.success();
	}

	@Delete("/1/applications/:name/:pushService")
	@Delete("/1/applications/:name/:pushService/")
	public Payload deleteApplication(String name, String pushService) {

		String backendId = SpaceContext.checkAdminCredentials().backendId();

		Optional<PlatformApplication> application = PushResource.get()//
				.getApplication(toApplicationId(backendId, name), //
						PushService.valueOf(pushService));

		if (application.isPresent()) {
			String applicationArn = application.get().getPlatformApplicationArn();

			PushResource.get().getSnsClient().deletePlatformApplication(//
					new DeletePlatformApplicationRequest()//
							.withPlatformApplicationArn(applicationArn));
		}

		return JsonPayload.success();
	}

	//
	// Implementation
	//

	private String toApplicationId(String backendId, String name) {
		return backendId + '-' + name;
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