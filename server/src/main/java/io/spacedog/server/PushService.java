package io.spacedog.server;

import java.util.Optional;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import com.amazonaws.services.sns.model.EndpointDisabledException;
import com.amazonaws.services.sns.model.PlatformApplicationDisabledException;
import com.amazonaws.services.sns.model.PublishRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import io.spacedog.client.PushRequest;
import io.spacedog.model.BadgeStrategy;
import io.spacedog.model.Credentials;
import io.spacedog.model.DataObject;
import io.spacedog.model.DataObjects;
import io.spacedog.model.Installation;
import io.spacedog.model.InstallationDataObject;
import io.spacedog.model.JsonDataObject;
import io.spacedog.model.Permission;
import io.spacedog.model.PushProtocol;
import io.spacedog.model.PushResponse;
import io.spacedog.model.PushResponse.Notification;
import io.spacedog.model.Roles;
import io.spacedog.model.Schema;
import io.spacedog.utils.Check;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;
import net.codestory.http.Context;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

@Prefix("/1")
public class PushService extends SpaceService {

	public static final String TYPE = "installation";

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

	public static Schema getDefaultInstallationSchema() {
		return Schema.builder(TYPE)//
				.acl(Roles.user, Permission.create, Permission.readMine, //
						Permission.updateMine, Permission.deleteMine) //

				.string(APP_ID)//
				.string(PROTOCOL)//
				.string(TOKEN)//
				.string(ENDPOINT)//
				.integer(BADGE)//
				.string(TAGS).array()//
				.close()//
				.build();
	}

	//
	// Routes
	//

	@Post("/push/installations")
	@Post("/push/installations/")
	@Post("/data/installation")
	@Post("/data/installation/")
	public Payload post(String body, Context context) {
		return upsertInstallation(Optional.empty(), body, context);
	}

	@Put("/push/installations/:id")
	@Put("/push/installations/:id/")
	@Put("/data/installation/:id")
	@Put("/data/installation/:id/")
	public Payload put(String id, String body, Context context) {
		return upsertInstallation(Optional.of(id), body, context);
	}

	/**
	 * Check this page for specific json messages:
	 * http://docs.aws.amazon.com/sns/latest/dg/mobile-push-send-custommessage. html
	 */
	@Post("/push")
	@Post("/push/")
	public Payload pushByTags(String body, Context context) {

		Credentials credentials = SpaceContext.credentials().checkAtLeastUser();
		PushRequest request = Json.toPojo(body, PushRequest.class);
		BoolQueryBuilder query = QueryBuilders.boolQuery();

		if (!Strings.isNullOrEmpty(request.appId))
			query.must(QueryBuilders.termQuery(APP_ID, request.appId));

		if (!Utils.isNullOrEmpty(request.credentialsIds))
			query.must(QueryBuilders.termsQuery(OWNER_FIELD, request.credentialsIds));

		if (!Utils.isNullOrEmpty(request.installationIds))
			query.must(QueryBuilders.idsQuery().ids(request.installationIds));

		if (request.protocol != null)
			query.must(QueryBuilders.termQuery(PROTOCOL, request.protocol));

		if (request.usersOnly)
			query.must(QueryBuilders.existsQuery(OWNER_FIELD))//
					.mustNot(QueryBuilders.termQuery(OWNER_FIELD, //
							Credentials.GUEST.id()));

		if (!Utils.isNullOrEmpty(request.tags))
			for (String tag : request.tags)
				query.must(QueryBuilders.termQuery(TAGS, tag));

		DataStore.get().refreshDataTypes(request.refresh, TYPE);

		// TODO use a scroll to push to all installations found
		SearchHits hits = elastic().prepareSearch(DataStore.toDataIndex(TYPE))//
				.setQuery(query)//
				.setFrom(0)//
				.setSize(1000)//
				.setVersion(false)//
				.setFetchSource(new String[] { OWNER_FIELD, ENDPOINT, PROTOCOL, BADGE }, null)//
				.get()//
				.getHits();

		if (hits.totalHits() > 1000)
			return JsonPayload.error(HttpStatus.NOT_IMPLEMENTED) //
					.withError("push to [%s] installations is a premium feature", //
							hits.totalHits())//
					.build();

		PushResponse response = new PushResponse();

		for (SearchHit hit : hits.getHits()) {
			DataObject<Installation> installation = new InstallationDataObject()//
					.source(Json.toPojo(hit.source(), Installation.class))//
					.id(hit.id());
			pushToInstallation(response, installation, toJsonMessage(request), //
					credentials, request.badgeStrategy);
			if (response.applicationDisabled)
				break;
		}

		return JsonPayload.ok().withObject(response).build();
	}

	//
	// Implementation
	//

	public void pushToInstallation(PushResponse response, DataObject<Installation> installation, //
			ObjectNode jsonMessage, Credentials credentials, BadgeStrategy badgeStrategy) {

		Notification notification = new Notification();
		notification.installationId = installation.id();
		response.notifications.add(notification);

		try {

			if (!Strings.isNullOrEmpty(installation.owner()))
				notification.owner = installation.owner();

			jsonMessage = badgeObjectMessage(installation, jsonMessage, //
					credentials, badgeStrategy);

			ObjectNode snsMessage = toSnsMessage(installation.source().protocol(), jsonMessage);

			if (!SpaceContext.isTest()) {
				PublishRequest pushRequest = new PublishRequest()//
						.withTargetArn(installation.source().endpoint())//
						.withMessageStructure("json")//
						.withMessage(snsMessage.toString());

				AwsSnsPusher.getSnsClient().publish(pushRequest);
			} else
				notification.message = snsMessage;

		} catch (Exception e) {
			response.failures = response.failures + 1;
			notification.error = JsonPayload.toJson(e, SpaceContext.isDebug());

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

	public static ObjectNode toJsonMessage(PushRequest request) {

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

	private ObjectNode badgeObjectMessage(DataObject<Installation> installation, //
			ObjectNode message, Credentials credentials, BadgeStrategy badgeStrategy) {

		if (badgeStrategy == null || //
				BadgeStrategy.manual.equals(badgeStrategy))
			return message;

		if (PushProtocol.APNS.equals(installation.source().protocol())//
				|| PushProtocol.APNS_SANDBOX.equals(installation.source().protocol())) {

			if (BadgeStrategy.auto.equals(badgeStrategy)) {
				installation.source().badge(installation.source().badge() + 1);

				// update installation badge in data store
				DataObject<ObjectNode> patch = DataObjects.copyIdentity(//
						installation, new JsonDataObject())//
						.source(Json.object(BADGE, installation.source().badge()));

				DataStore.get().patchObject(patch);
			}

			message.with(installation.source().protocol().toString())//
					.with("aps").put(BADGE, installation.source().badge());
		}
		return message;
	}

	private void removeEndpointQuietly(String id) {
		try {
			elastic().delete(DataStore.toDataIndex(TYPE), id, false, true);
		} catch (Exception e) {
			System.err.println(String.format(//
					"[Warning] failed to delete disabled installation [%s]", id));
			e.printStackTrace();
		}
	}

	public Payload upsertInstallation(Optional<String> id, String body, Context context) {

		DataObject<Installation> installation = new InstallationDataObject()//
				.type(TYPE).source(Json.toPojo(body, Installation.class));

		if (id.isPresent())
			installation.id(id.get());

		Installation source = installation.source();

		Check.notNullOrEmpty(source.token(), TOKEN);
		Check.notNullOrEmpty(source.appId(), APP_ID);
		Check.notNull(source.protocol(), PROTOCOL);

		source.endpoint(//
				SpaceContext.isTest() ? "FAKE_ENDPOINT_FOR_TESTING" //
						: AwsSnsPusher.createApplicationEndpoint(source.appId(), //
								source.protocol(), source.token()));

		return id.isPresent() //
				? DataService.get().doPut(installation, false, context) //
				: DataService.get().doPost(installation);
	}

	//
	// Singleton
	//

	private static PushService singleton = new PushService();

	public static PushService get() {
		return singleton;
	}

	private PushService() {
	}
}
