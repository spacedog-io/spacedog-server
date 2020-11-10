package io.spacedog.services.push;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.amazonaws.services.sns.model.CreatePlatformApplicationRequest;
import com.amazonaws.services.sns.model.DeletePlatformApplicationRequest;
import com.amazonaws.services.sns.model.EndpointDisabledException;
import com.amazonaws.services.sns.model.PlatformApplication;
import com.amazonaws.services.sns.model.PlatformApplicationDisabledException;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.SetPlatformApplicationAttributesRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.data.DataWrap;
import io.spacedog.client.http.SpaceFields;
import io.spacedog.client.push.BadgeStrategy;
import io.spacedog.client.push.Installation;
import io.spacedog.client.push.PushApplication;
import io.spacedog.client.push.PushProtocol;
import io.spacedog.client.push.PushRequest;
import io.spacedog.client.push.PushResponse;
import io.spacedog.client.push.PushResponse.Notification;
import io.spacedog.client.push.PushSettings;
import io.spacedog.client.schema.Schema;
import io.spacedog.server.Server;
import io.spacedog.services.JsonPayload;
import io.spacedog.services.Services;
import io.spacedog.services.SpaceService;
import io.spacedog.services.data.DataResults;
import io.spacedog.services.elastic.ElasticIndex;
import io.spacedog.utils.Check;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;

public class PushService extends SpaceService implements SpaceFields {

	public static final String DATA_TYPE = "installation";

	// installation field names
	public static final String APP_ID = "appId";
	public static final String PROTOCOL = "protocol";
	public static final String TOKEN = "token";
	public static final String ENDPOINT = "endpoint";
	public static final String BADGE = "badge";
	public static final String TAGS = "tags";

	// push response field names
	public static final String PUSHED_TO = "pushedTo";
	public static final String FAILURES = "failures";

	//
	// Schema
	//

	public Schema getDefaultInstallationSchema() {
		return Schema.builder(DATA_TYPE)//
				.keyword(APP_ID)//
				.keyword(PROTOCOL)//
				.keyword(TOKEN)//
				.keyword(ENDPOINT)//
				.integer(BADGE)//
				.keyword(TAGS)//
				.build();
	}

	//
	// Applications
	//

	public List<PushApplication> listApps() {
		String backendId = Server.backend().id();

		List<PlatformApplication> applications = AwsSnsPusher.sns()//
				.listPlatformApplications()//
				.getPlatformApplications();

		return applications.stream()//
				.map(application -> toPushApplication(application))//
				.filter(application -> backendId.equals(application.backendId))//
				.collect(Collectors.toList());
	}

	public void saveApp(String name, PushProtocol protocol, //
			PushApplication.Credentials credentials) {

		PushApplication app = new PushApplication();
		app.name = name;
		app.backendId = Server.backend().id();
		app.protocol = protocol;
		app.credentials = credentials;
		deleteApp(app);

	}

	public void saveApp(PushApplication app) {
		Optional<PlatformApplication> application = getPlatformApplication(app);

		if (application.isPresent()) {
			SetPlatformApplicationAttributesRequest request = new SetPlatformApplicationAttributesRequest()//
					.withPlatformApplicationArn(application.get().getPlatformApplicationArn());

			if (!Strings.isNullOrEmpty(app.credentials.credentials))
				request.addAttributesEntry("PlatformCredential", app.credentials.credentials);

			if (!Strings.isNullOrEmpty(app.credentials.principal))
				request.addAttributesEntry("PlatformPrincipal", app.credentials.principal);

			AwsSnsPusher.sns().setPlatformApplicationAttributes(request);

		} else {

			CreatePlatformApplicationRequest request = new CreatePlatformApplicationRequest()//
					.withName(app.id())//
					.withPlatform(app.protocol.toString());

			if (!Strings.isNullOrEmpty(app.credentials.credentials))
				request.addAttributesEntry("PlatformCredential", app.credentials.credentials);

			if (!Strings.isNullOrEmpty(app.credentials.principal))
				request.addAttributesEntry("PlatformPrincipal", app.credentials.principal);

			AwsSnsPusher.sns().createPlatformApplication(request);
		}
	}

	public void deleteApp(String name, PushProtocol protocol) {
		PushApplication app = new PushApplication();
		app.name = name;
		app.backendId = Server.backend().id();
		app.protocol = protocol;
		deleteApp(app);
	}

	public void deleteApp(PushApplication app) {
		Optional<PlatformApplication> application = getPlatformApplication(app);

		if (application.isPresent()) {
			String applicationArn = application.get().getPlatformApplicationArn();

			AwsSnsPusher.sns().deletePlatformApplication(//
					new DeletePlatformApplicationRequest()//
							.withPlatformApplicationArn(applicationArn));
		}
	}

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
	// Push
	//

	public PushResponse push(PushRequest request) {
		BoolQueryBuilder query = QueryBuilders.boolQuery();

		if (!Strings.isNullOrEmpty(request.appId))
			query.must(QueryBuilders.termQuery(APP_ID, request.appId));

		if (!Utils.isNullOrEmpty(request.credentialsIds))
			query.must(QueryBuilders.termsQuery(OWNER_FIELD, request.credentialsIds));

		if (!Utils.isNullOrEmpty(request.installationIds))
			query.must(QueryBuilders.idsQuery().addIds(request.installationIds));

		if (request.protocol != null)
			query.must(QueryBuilders.termQuery(PROTOCOL, request.protocol));

		if (request.usersOnly)
			query.must(QueryBuilders.existsQuery(OWNER_FIELD))//
					.mustNot(QueryBuilders.termQuery(OWNER_FIELD, //
							Credentials.GUEST.id()));

		if (!Utils.isNullOrEmpty(request.tags))
			for (String tag : request.tags)
				query.must(QueryBuilders.termQuery(TAGS, tag));

		Services.data().refresh(request.refresh, DATA_TYPE);

		SearchSourceBuilder source = SearchSourceBuilder.searchSource()//
				.fetchSource(new String[] { OWNER_FIELD, ENDPOINT, PROTOCOL, BADGE }, null)//
				.query(query)//
				.from(0)//
				.size(1000)//
				.version(false);

		// TODO use a scroll to push to all installations found
		SearchHits hits = elastic().search(source, index())//
				.getHits();

		if (hits.getTotalHits().value > 1000)
			throw Exceptions.forbidden(Server.context().credentials(), //
					"push to [%s] installations is a premium feature", //
					hits.getTotalHits());

		PushResponse response = new PushResponse();

		for (SearchHit hit : hits.getHits()) {
			Installation installation = Json.toPojo(//
					BytesReference.toBytes(hit.getSourceRef()), //
					Installation.class);

			DataWrap<Installation> wrap = DataWrap.wrap(installation).id(hit.getId());

			pushToInstallation(response, wrap, //
					toJsonMessage(request), request.badgeStrategy);

			if (response.applicationDisabled)
				break;
		}
		return response;
	}

	//
	// Installations
	//

	public DataWrap<Installation> getInstallation(String id) {
		return Services.data().getWrapped(DATA_TYPE, id, Installation.class);
	}

	public DataWrap<Installation> fetchInstallation(DataWrap<Installation> wrap) {
		return Services.data().fetch(wrap);
	}

	public DataWrap<Installation> saveInstallationIfAuthorized(Installation source) {
		return saveInstallationIfAuthorized(DataWrap.wrap(source));
	}

	public DataWrap<Installation> saveInstallationIfAuthorized(DataWrap<Installation> wrap) {

		wrap.type(DATA_TYPE);
		Installation installation = wrap.source();

		Check.notNullOrEmpty(installation.token(), TOKEN);
		Check.notNullOrEmpty(installation.appId(), APP_ID);
		Check.notNull(installation.protocol(), PROTOCOL);

		installation.endpoint(//
				Server.context().isTest() ? "FAKE_ENDPOINT_FOR_TESTING" //
						: AwsSnsPusher.createApplicationEndpoint(installation.appId(), //
								installation.protocol(), installation.token()));

		return Services.data().saveIfAuthorized(wrap, false);
	}

	public long patchInstallation(String id, Object source) {
		return Services.data().patch(DATA_TYPE, id, source);
	}

	public long saveInstallationField(String id, String field, Object object) {
		return Services.data().save(DATA_TYPE, id, field, object);
	}

	public DataResults<Installation> searchInstallations(SearchSourceBuilder source) {
		return Services.data().search(Installation.class, source, DATA_TYPE);
	}

	public void deleteInstallation(String id) {
		deleteInstallation(id, true);
	}

	public void deleteInstallation(String id, boolean throwNotFound) {
		Services.data().delete(DATA_TYPE, id, throwNotFound);
	}

	//
	// Installation tags
	//

	public String[] getTags(String installationId) {
		return Services.data().get(DATA_TYPE, installationId, "tags", String[].class);
	}

	public long setTags(String installationId, String... tags) {
		return Services.data().save(DATA_TYPE, installationId, "tags", tags);
	}

	public long deleteTags(String installationId) {
		return Services.data().delete(DATA_TYPE, installationId, TAGS);
	}

	//
	// Index help methods
	//

	public ElasticIndex index() {
		return Services.data().index(DATA_TYPE);
	}

	//
	// Implementation
	//

	void pushToInstallation(PushResponse response, //
			DataWrap<Installation> installation, //
			ObjectNode jsonMessage, BadgeStrategy badgeStrategy) {

		Notification notification = new Notification();
		notification.installationId = installation.id();
		response.notifications.add(notification);

		try {

			if (!Strings.isNullOrEmpty(installation.owner()))
				notification.owner = installation.owner();

			jsonMessage = badgeObjectMessage(//
					installation, jsonMessage, badgeStrategy);

			ObjectNode snsMessage = toSnsMessage(installation.source().protocol(), jsonMessage);

			if (!Server.context().isTest()) {
				PublishRequest pushRequest = new PublishRequest()//
						.withTargetArn(installation.source().endpoint())//
						.withMessageStructure("json")//
						.withMessage(snsMessage.toString());

				AwsSnsPusher.sns().publish(pushRequest);
			} else
				notification.message = snsMessage;

		} catch (Exception e) {
			response.failures = response.failures + 1;
			notification.error = JsonPayload.toJson(e, //
					Server.context().debug().isTrue());

			if (e instanceof EndpointDisabledException //
					|| e.getMessage().contains(//
							"No endpoint found for the target arn specified")) {

				notification.installationDisabled = true;
				removeEndpointQuietly(installation.id());
			}

			if (e instanceof PlatformApplicationDisabledException)
				response.applicationDisabled = true;
		}
	}

	static ObjectNode toJsonMessage(PushRequest request) {

		return request.data == null //
				? Json.object("default", request.text, //
						"APNS", Json.object("aps", Json.object("alert", request.text)), //
						"APNS_SANDBOX", Json.object("aps", Json.object("alert", request.text)), //
						"GCM", Json.object("notification", Json.object("body", request.text)))//
				: request.data;
	}

	static ObjectNode toSnsMessage(PushProtocol protocol, ObjectNode message) {

		JsonNode node = message.get(protocol.toString());
		if (!Json.isNull(node))
			return Json.object(protocol.toString(), toSnsMessageStringValue(protocol, node));

		node = message.get("default");
		if (!Json.isNull(node))
			return Json.object("default", toSnsMessageStringValue(protocol, node));

		throw Exceptions.illegalArgument(//
				"no push message for default nor [%s] service", protocol);
	}

	static String toSnsMessageStringValue(PushProtocol protocol, JsonNode message) {
		if (message.isObject())
			return message.toString();

		// be careful not to 'toString' simple text values
		// because it would double quote stringified objects
		if (message.isValueNode())
			return message.asText();

		throw Exceptions.illegalArgument("push message [%s][%s] is invalid", protocol, message);
	}

	private ObjectNode badgeObjectMessage(DataWrap<Installation> installation, //
			ObjectNode message, BadgeStrategy badgeStrategy) {

		if (badgeStrategy == null || //
				BadgeStrategy.manual.equals(badgeStrategy))
			return message;

		if (PushProtocol.APNS.equals(installation.source().protocol())//
				|| PushProtocol.APNS_SANDBOX.equals(installation.source().protocol())) {

			if (BadgeStrategy.auto.equals(badgeStrategy)) {
				installation.source().badge(installation.source().badge() + 1);

				// update installation badge in data store
				Services.data().patch(installation.type(), //
						installation.id(), installation.version(), //
						Json.object(BADGE, installation.source().badge()));
			}

			message.with(installation.source().protocol().toString())//
					.with("aps").put(BADGE, installation.source().badge());
		}
		return message;
	}

	private void removeEndpointQuietly(String id) {
		try {
			deleteInstallation(id);
		} catch (Throwable t) {
			System.err.println(String.format(//
					"[Warning] failed to delete disabled installation [%s]", id));
			t.printStackTrace();
		}
	}

	public PushSettings settings() {
		return Services.settings().getOrThrow(PushSettings.class);
	}

}
