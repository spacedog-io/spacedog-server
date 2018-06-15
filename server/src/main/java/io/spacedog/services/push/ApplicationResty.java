/**
 * © David Attias 2015
 */
package io.spacedog.services.push;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.amazonaws.services.sns.model.CreatePlatformApplicationRequest;
import com.amazonaws.services.sns.model.DeletePlatformApplicationRequest;
import com.amazonaws.services.sns.model.ListPlatformApplicationsResult;
import com.amazonaws.services.sns.model.PlatformApplication;
import com.amazonaws.services.sns.model.SetPlatformApplicationAttributesRequest;
import com.google.common.base.Strings;

import io.spacedog.client.push.PushApplication;
import io.spacedog.client.push.PushProtocol;
import io.spacedog.server.JsonPayload;
import io.spacedog.server.Server;
import io.spacedog.server.SpaceResty;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;

@Prefix("/1/push/applications")
public class ApplicationResty extends SpaceResty {

	//
	// Routes
	//

	@Get("")
	@Get("/")
	public Payload getApplications() {

		Server.context().credentials().checkAtLeastAdmin();
		String backendId = Server.backend().backendId();

		ListPlatformApplicationsResult applications = AwsSnsPusher.sns()//
				.listPlatformApplications();

		List<PushApplication> pushApps = applications.getPlatformApplications().stream()//
				.map(application -> toPushApplication(application))//
				.filter(application -> backendId.equals(application.backendId))//
				.collect(Collectors.toList());

		return JsonPayload.ok().withContent(pushApps).build();
	}

	@Put("/:name/:protocol")
	@Put("/:name/:protocol/")
	public Payload putApplication(String name, String protocol, String body) {

		Server.context().credentials().checkAtLeastAdmin();

		PushApplication pushApp = new PushApplication();
		pushApp.backendId = Server.backend().backendId();
		pushApp.name = name;
		pushApp.protocol = PushProtocol.valueOf(protocol);
		pushApp.credentials = Json.toPojo(body, PushApplication.Credentials.class);

		Optional<PlatformApplication> application = getPlatformApplication(pushApp);

		if (application.isPresent()) {
			SetPlatformApplicationAttributesRequest request = new SetPlatformApplicationAttributesRequest()//
					.withPlatformApplicationArn(application.get().getPlatformApplicationArn());

			if (!Strings.isNullOrEmpty(pushApp.credentials.credentials))
				request.addAttributesEntry("PlatformCredential", pushApp.credentials.credentials);

			if (!Strings.isNullOrEmpty(pushApp.credentials.principal))
				request.addAttributesEntry("PlatformPrincipal", pushApp.credentials.principal);

			AwsSnsPusher.sns().setPlatformApplicationAttributes(request);

		} else {

			CreatePlatformApplicationRequest request = new CreatePlatformApplicationRequest()//
					.withName(pushApp.id())//
					.withPlatform(protocol);

			if (!Strings.isNullOrEmpty(pushApp.credentials.credentials))
				request.addAttributesEntry("PlatformCredential", pushApp.credentials.credentials);

			if (!Strings.isNullOrEmpty(pushApp.credentials.principal))
				request.addAttributesEntry("PlatformPrincipal", pushApp.credentials.principal);

			AwsSnsPusher.sns().createPlatformApplication(request);
		}

		return JsonPayload.ok().build();
	}

	@Delete("/:name/:protocol")
	@Delete("/:name/:protocol/")
	public Payload deleteApplication(String name, String protocol) {

		Server.context().credentials().checkAtLeastSuperAdmin();

		PushApplication pushApp = new PushApplication();
		pushApp.backendId = Server.backend().backendId();
		pushApp.name = name;
		pushApp.protocol = PushProtocol.valueOf(protocol);

		Optional<PlatformApplication> application = getPlatformApplication(pushApp);

		if (application.isPresent()) {
			String applicationArn = application.get().getPlatformApplicationArn();

			AwsSnsPusher.sns().deletePlatformApplication(//
					new DeletePlatformApplicationRequest()//
							.withPlatformApplicationArn(applicationArn));
		}

		return JsonPayload.ok().build();
	}

	//
	// Implementation
	//

	private Optional<PlatformApplication> getPlatformApplication(PushApplication pushApp) {
		return AwsSnsPusher.getApplication(pushApp.id(), pushApp.protocol);
	}

	private PushApplication fromApplicationArn(String arn) {
		PushApplication pushApp = new PushApplication();
		String[] splitedArn = Utils.splitBySlash(arn);
		if (splitedArn.length != 3)
			throw Exceptions.runtime("invalid amazon platform application arn [%s]", arn);
		pushApp.protocol = PushProtocol.valueOf(splitedArn[1]);
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

	private static ApplicationResty singleton = new ApplicationResty();

	public static ApplicationResty get() {
		return singleton;
	}

	private ApplicationResty() {
	}
}
