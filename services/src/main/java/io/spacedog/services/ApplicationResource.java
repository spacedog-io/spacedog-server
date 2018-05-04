/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.elasticsearch.common.Strings;

import com.amazonaws.services.sns.model.CreatePlatformApplicationRequest;
import com.amazonaws.services.sns.model.DeletePlatformApplicationRequest;
import com.amazonaws.services.sns.model.ListPlatformApplicationsResult;
import com.amazonaws.services.sns.model.PlatformApplication;
import com.amazonaws.services.sns.model.SetPlatformApplicationAttributesRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.spacedog.core.Json8;
import io.spacedog.model.PushService;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json7;
import io.spacedog.utils.Utils;
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
		String backendId = SpaceContext.checkAdminCredentials().backendId();
		ListPlatformApplicationsResult applications = PushResource.get()//
				.getSnsClient().listPlatformApplications();

		List<PushApplication> pushApps = applications.getPlatformApplications().stream()//
				.map(application -> toPushApplication(application))//
				.filter(application -> backendId.equals(application.backendId))//
				.collect(Collectors.toList());

		return JsonPayload.json(Json7.toString(pushApps), 200);
	}

	@Put("/1/applications/:name/:pushService")
	@Put("/1/applications/:name/:pushService/")
	public Payload putApplication(String name, String pushService, String body) {

		io.spacedog.utils.Credentials credentials = SpaceContext.checkAdminCredentials();

		PushApplication pushApp = new PushApplication();
		pushApp.backendId = credentials.backendId();
		pushApp.name = name;
		pushApp.service = PushService.valueOf(pushService);
		pushApp.credentials = Json8.readObject(body, PushApplication.Credentials.class);

		Optional<PlatformApplication> application = getPlatformApplication(pushApp);

		if (application.isPresent()) {
			SetPlatformApplicationAttributesRequest request = new SetPlatformApplicationAttributesRequest()//
					.withPlatformApplicationArn(application.get().getPlatformApplicationArn());

			if (!Strings.isNullOrEmpty(pushApp.credentials.credentials))
				request.addAttributesEntry("PlatformCredential", pushApp.credentials.credentials);

			if (!Strings.isNullOrEmpty(pushApp.credentials.principal))
				request.addAttributesEntry("PlatformPrincipal", pushApp.credentials.principal);

			PushResource.get().getSnsClient().setPlatformApplicationAttributes(request);

		} else {

			CreatePlatformApplicationRequest request = new CreatePlatformApplicationRequest()//
					.withName(pushApp.applicationId())//
					.withPlatform(pushService);

			if (!Strings.isNullOrEmpty(pushApp.credentials.credentials))
				request.addAttributesEntry("PlatformCredential", pushApp.credentials.credentials);

			if (!Strings.isNullOrEmpty(pushApp.credentials.principal))
				request.addAttributesEntry("PlatformPrincipal", pushApp.credentials.principal);

			PushResource.get().getSnsClient().createPlatformApplication(request);
		}

		return JsonPayload.success();
	}

	@Delete("/1/applications/:name/:pushService")
	@Delete("/1/applications/:name/:pushService/")
	public Payload deleteApplication(String name, String pushService) {

		io.spacedog.utils.Credentials credentials = SpaceContext.checkAdminCredentials();

		PushApplication pushApp = new PushApplication();
		pushApp.backendId = credentials.backendId();
		pushApp.name = name;
		pushApp.service = PushService.valueOf(pushService);

		Optional<PlatformApplication> application = getPlatformApplication(pushApp);

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

	private Optional<PlatformApplication> getPlatformApplication(PushApplication pushApp) {
		return PushResource.get().getApplication(pushApp.applicationId(), pushApp.service);
	}

	private PushApplication fromApplicationArn(String arn) {
		PushApplication pushApp = new PushApplication();
		String[] splitedArn = Utils.splitBySlash(arn);
		if (splitedArn.length != 3)
			throw Exceptions.runtime("invalid amazon platform application arn [%s]", arn);
		pushApp.service = PushService.valueOf(splitedArn[1]);
		String[] splitedAppId = Utils.splitByDash(splitedArn[2]);
		if (splitedAppId.length == 2) {
			pushApp.backendId = splitedAppId[0];
			pushApp.name = splitedAppId[1];
		} else
			pushApp.name = splitedAppId[0];
		return pushApp;
	}

	private PushApplication toPushApplication(PlatformApplication application) {
		PushApplication pushApp = fromApplicationArn(application.getPlatformApplicationArn());
		pushApp.attributes = application.getAttributes();
		return pushApp;
	}

	public static class PushApplication {
		public String name;
		public String backendId;
		public PushService service;
		public Map<String, String> attributes;
		@JsonIgnore
		public Credentials credentials;

		public static class Credentials {
			public String principal;
			public String credentials;
		}

		public String applicationId() {
			return backendId + '-' + name;
		}

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
