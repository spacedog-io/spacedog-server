/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.elasticsearch.common.Strings;

import com.amazonaws.services.sns.model.CreatePlatformApplicationRequest;

import io.spacedog.core.Json8;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;

public class ApplicationResource extends Resource {

	private static final String PLATFORM_CREDENTIAL_ATTRIBUTE = "PlatformCredential";
	private static final String PLATFORM_PRINCIPAL_ATTRIBUTE = "PlatformPrincipal";

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

	@Put("/1/applications/:name/:platform")
	@Put("/1/applications/:name/:platform/")
	public Payload putApplication(String name, String platform, String body) {

		String backendId = SpaceContext.checkAdminCredentials().backendId();

		ApplicationPushCredentials credentials = Json8.readObject(//
				body, ApplicationPushCredentials.class);

		CreatePlatformApplicationRequest request = new CreatePlatformApplicationRequest()//
				.withName(backendId + '-' + name).withPlatform(platform);

		request.addAttributesEntry(PLATFORM_CREDENTIAL_ATTRIBUTE, credentials.credentials);

		if (!Strings.isNullOrEmpty(credentials.principal))
			request.addAttributesEntry(PLATFORM_PRINCIPAL_ATTRIBUTE, credentials.principal);

		PushResource.get().getSnsClient().createPlatformApplication(request);

		return JsonPayload.success();
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
