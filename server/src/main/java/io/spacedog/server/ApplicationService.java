/**
 * © David Attias 2015
 */
package io.spacedog.server;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.amazonaws.services.sns.model.CreatePlatformApplicationRequest;
import com.amazonaws.services.sns.model.DeletePlatformApplicationRequest;
import com.amazonaws.services.sns.model.ListPlatformApplicationsResult;
import com.amazonaws.services.sns.model.PlatformApplication;
import com.amazonaws.services.sns.model.SetPlatformApplicationAttributesRequest;
import com.google.common.base.Strings;

import io.spacedog.model.PushApplication;
import io.spacedog.model.PushService;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;

@Prefix("/1/push/applications")
public class ApplicationService extends SpaceService {

	//
	// Routes
	//

	@Get("")
	@Get("/")
	public Payload getApplications() {

		SpaceContext.credentials().checkAtLeastAdmin();
		String backendId = SpaceContext.backendId();

		ListPlatformApplicationsResult applications = AwsSnsPusher.getSnsClient()//
				.listPlatformApplications();

		List<PushApplication> pushApps = applications.getPlatformApplications().stream()//
				.map(application -> toPushApplication(application))//
				.filter(application -> backendId.equals(application.backendId))//
				.collect(Collectors.toList());

		return JsonPayload.ok().withObject(Json.toJsonNode(pushApps)).build();
	}

	@Put("/:name/:service")
	@Put("/:name/:service/")
	public Payload putApplication(String name, String service, String body) {

		SpaceContext.credentials().checkAtLeastAdmin();

		PushApplication pushApp = new PushApplication();
		pushApp.backendId = SpaceContext.backendId();
		pushApp.name = name;
		pushApp.service = PushService.valueOf(service);
		pushApp.credentials = Json.toPojo(body, PushApplication.Credentials.class);

		Optional<PlatformApplication> application = getPlatformApplication(pushApp);

		if (application.isPresent()) {
			SetPlatformApplicationAttributesRequest request = new SetPlatformApplicationAttributesRequest()//
					.withPlatformApplicationArn(application.get().getPlatformApplicationArn());

			if (!Strings.isNullOrEmpty(pushApp.credentials.credentials))
				request.addAttributesEntry("PlatformCredential", pushApp.credentials.credentials);

			if (!Strings.isNullOrEmpty(pushApp.credentials.principal))
				request.addAttributesEntry("PlatformPrincipal", pushApp.credentials.principal);

			AwsSnsPusher.getSnsClient().setPlatformApplicationAttributes(request);

		} else {

			CreatePlatformApplicationRequest request = new CreatePlatformApplicationRequest()//
					.withName(pushApp.id())//
					.withPlatform(service);

			if (!Strings.isNullOrEmpty(pushApp.credentials.credentials))
				request.addAttributesEntry("PlatformCredential", pushApp.credentials.credentials);

			if (!Strings.isNullOrEmpty(pushApp.credentials.principal))
				request.addAttributesEntry("PlatformPrincipal", pushApp.credentials.principal);

			AwsSnsPusher.getSnsClient().createPlatformApplication(request);
		}

		return JsonPayload.ok().build();
	}

	@Delete("/:name/:service")
	@Delete("/:name/:service/")
	public Payload deleteApplication(String name, String service) {

		SpaceContext.credentials().checkAtLeastSuperAdmin();

		PushApplication pushApp = new PushApplication();
		pushApp.backendId = SpaceContext.backendId();
		pushApp.name = name;
		pushApp.service = PushService.valueOf(service);

		Optional<PlatformApplication> application = getPlatformApplication(pushApp);

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

	private Optional<PlatformApplication> getPlatformApplication(PushApplication pushApp) {
		return AwsSnsPusher.getApplication(pushApp.id(), pushApp.service);
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

	//
	// Singleton
	//

	private static ApplicationService singleton = new ApplicationService();

	static ApplicationService get() {
		return singleton;
	}

	private ApplicationService() {
	}
}
