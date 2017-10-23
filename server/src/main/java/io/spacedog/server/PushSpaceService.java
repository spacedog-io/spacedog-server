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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import io.spacedog.client.PushRequest;
import io.spacedog.model.BadgeStrategy;
import io.spacedog.model.DataObject;
import io.spacedog.model.DataObjects;
import io.spacedog.model.Installation;
import io.spacedog.model.InstallationDataObject;
import io.spacedog.model.JsonDataObject;
import io.spacedog.model.Permission;
import io.spacedog.model.PushService;
import io.spacedog.model.Schema;
import io.spacedog.utils.Check;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

@Prefix("/1")
public class PushSpaceService extends SpaceService {

	public static final String TYPE = "installation";

	// installation field names
	public static final String APP_ID = "appId";
	public static final String PUSH_SERVICE = "pushService";
	public static final String CREDENTIALS_ID = "credentialsId";
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
				.acl(Credentials.Type.user.name(), Permission.create, Permission.read, //
						Permission.update, Permission.delete) //

				.string(APP_ID)//
				.string(PUSH_SERVICE)//
				.string(TOKEN)//
				.string(ENDPOINT)//
				.string(CREDENTIALS_ID)//
				.integer(BADGE)//
				.string(TAGS).array()//
				.close()//
				.build();
	}

	//
	// Routes
	//

	@Get("/installation")
	@Get("/installation/")
	@Get("/data/installation")
	@Get("/data/installation/")
	public Payload getAll(Context context) {
		return DataService.get().getByType(TYPE, context);
	}

	@Post("/installation")
	@Post("/installation/")
	@Post("/data/installation")
	@Post("/data/installation/")
	public Payload post(String body, Context context) {
		return upsertInstallation(Optional.empty(), body, context);
	}

	@Delete("/installation")
	@Delete("/installation/")
	@Delete("/data/installation")
	@Delete("/data/installation/")
	public Payload deleteAll(Context context) {
		return DataService.get().deleteByType(TYPE, context);
	}

	@Get("/installation/:id")
	@Get("/installation/:id/")
	@Get("/data/installation/:id")
	@Get("/data/installation/:id/")
	public Payload get(String id, Context context) {
		return DataService.get().getById(TYPE, id, context);
	}

	@Delete("/installation/:id")
	@Delete("/installation/:id/")
	@Delete("/data/installation/:id")
	@Delete("/data/installation/:id/")
	public Payload delete(String id, Context context) {
		return DataService.get().deleteById(TYPE, id, context);
	}

	@Put("/installation/:id")
	@Put("/installation/:id/")
	@Put("/data/installation/:id")
	@Put("/data/installation/:id/")
	public Payload put(String id, String body, Context context) {
		return upsertInstallation(Optional.of(id), body, context);
	}

	@Post("/installation/:id/push")
	@Post("/installation/:id/push/")
	public Payload pushById(String id, String body, Context context) {

		Credentials credentials = SpaceContext.credentials().checkAtLeastUser();
		PushRequest request = Json.toPojo(body, PushRequest.class);
		DataObject<Installation> installation = load(id);

		PushLog log = new PushLog();
		pushToInstallation(log, installation, toJsonMessage(request), //
				credentials, request.badgeStrategy);
		return log.toPayload();
	}

	@Get("/installation/:id/tags")
	@Get("/installation/:id/tags/")
	public Payload getTags(String id, Context context) {
		return getField(id, TAGS, context);
	}

	@Post("/installation/:id/tags")
	@Post("/installation/:id/tags/")
	public Payload postTags(String id, String body, Context context) {
		DataObject<Installation> installation = load(id);
		String[] tags = Json.toPojo(body, String[].class);
		installation.source().tags().addAll(Sets.newHashSet(tags));
		DataStore.get().updateObject(installation);
		return JsonPayload.saved(false, "/1", TYPE, id).build();
	}

	@Put("/installation/:id/tags")
	@Put("/installation/:id/tags/")
	public Payload putTags(String id, String body, Context context) {
		return putField(id, TAGS, body, context);
	}

	@Delete("/installation/:id/tags")
	@Delete("/installation/:id/tags/")
	public Payload deleteTags(String id, String body, Context context) {
		DataObject<Installation> installation = load(id);
		String[] tags = Json.toPojo(body, String[].class);
		installation.source().tags().removeAll(Sets.newHashSet(tags));
		DataStore.get().updateObject(installation);
		return JsonPayload.saved(false, "/1", TYPE, id).build();
	}

	@Get("/installation/:id/:field")
	@Get("/installation/:id/:field/")
	public Payload getField(String id, String field, Context context) {
		return DataService.get().getField(TYPE, id, field, context);
	}

	@Put("/installation/:id/:field")
	@Put("/installation/:id/:field/")
	public Payload putField(String id, String field, String body, Context context) {
		return DataService.get().putField(TYPE, id, field, body, context);
	}

	@Delete("/installation/:id/:field")
	@Delete("/installation/:id/:field/")
	public Payload deleteField(String id, String field, Context context) {
		return DataService.get().deleteField(TYPE, id, field, context);
	}

	/**
	 * Check this page for specific json messages:
	 * http://docs.aws.amazon.com/sns/latest/dg/mobile-push-send-custommessage. html
	 */
	@Post("/push")
	@Post("/push/")
	@Post("/installation/push")
	@Post("/installation/push/")
	public Payload pushByTags(String body, Context context) {

		Credentials credentials = SpaceContext.credentials().checkAtLeastUser();
		PushRequest request = Json.toPojo(body, PushRequest.class);
		BoolQueryBuilder query = QueryBuilders.boolQuery();

		if (!Strings.isNullOrEmpty(request.appId))
			query.must(QueryBuilders.termQuery(APP_ID, request.appId));

		if (!Strings.isNullOrEmpty(request.credentialsId))
			query.must(QueryBuilders.termQuery(CREDENTIALS_ID, request.credentialsId));

		if (request.pushService != null)
			query.must(QueryBuilders.termQuery(PUSH_SERVICE, request.pushService));

		if (request.usersOnly)
			query.must(QueryBuilders.existsQuery(CREDENTIALS_ID));

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
				.setFetchSource(new String[] { CREDENTIALS_ID, ENDPOINT, PUSH_SERVICE, BADGE }, null)//
				.get()//
				.getHits();

		if (hits.totalHits() > 1000)
			return JsonPayload.error(HttpStatus.NOT_IMPLEMENTED) //
					.withError("push to [%s] installations is a premium feature", //
							hits.totalHits())//
					.build();

		PushLog log = new PushLog();

		for (SearchHit hit : hits.getHits()) {
			DataObject<Installation> installation = new InstallationDataObject()//
					.source(Json.toPojo(hit.source(), Installation.class))//
					.id(hit.id());
			pushToInstallation(log, installation, toJsonMessage(request), //
					credentials, request.badgeStrategy);
			if (log.applicationDisabled)
				break;
		}

		return log.toPayload();
	}

	//
	// Implementation
	//

	public static class PushLog {
		public int successes = 0;
		public boolean failures;
		public ArrayNode logItems = Json.array();
		public boolean applicationDisabled;

		public Payload toPayload() {
			int httpStatus = logItems.size() == 0 ? HttpStatus.NOT_FOUND //
					: successes > 0 ? HttpStatus.OK : HttpStatus.BAD_REQUEST;

			return JsonPayload.status(httpStatus)//
					.withFields(FAILURES, failures, //
							"applicationDisabled", applicationDisabled, //
							PUSHED_TO, logItems)//
					.build();
		}

		public ObjectNode toNode() {
			return Json.object(FAILURES, failures, //
					"applicationDisabled", applicationDisabled, //
					PUSHED_TO, logItems);
		}
	}

	public void pushToInstallation(PushLog log, DataObject<Installation> installation, //
			ObjectNode jsonMessage, Credentials credentials, BadgeStrategy badgeStrategy) {

		ObjectNode logItem = Json.object("installationId", installation.id());
		log.logItems.add(logItem);

		try {

			if (!Strings.isNullOrEmpty(installation.source().credentialsId()))
				logItem.put(CREDENTIALS_ID, installation.source().credentialsId());

			jsonMessage = badgeObjectMessage(installation, jsonMessage, //
					credentials, logItem, badgeStrategy);

			ObjectNode snsMessage = toSnsMessage(installation.source().pushService(), jsonMessage);

			if (!SpaceContext.isTest()) {
				PublishRequest pushRequest = new PublishRequest()//
						.withTargetArn(installation.source().endpoint())//
						.withMessageStructure("json")//
						.withMessage(snsMessage.toString());

				AwsSnsPusher.getSnsClient().publish(pushRequest);
			} else
				logItem.set("message", snsMessage);

			log.successes = log.successes + 1;

		} catch (Exception e) {
			log.failures = true;
			logItem.set(ERROR_FIELD, JsonPayload.toJson(e, SpaceContext.isDebug()));

			if (e instanceof EndpointDisabledException //
					|| e.getMessage().contains(//
							"No endpoint found for the target arn specified")) {

				logItem.put("installationDisabled", true);
				removeEndpointQuietly(installation.id());
			}

			if (e instanceof PlatformApplicationDisabledException)
				log.applicationDisabled = true;
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

	static ObjectNode toSnsMessage(PushService service, ObjectNode message) {

		JsonNode node = message.get(service.toString());
		if (!Json.isNull(node))
			return Json.object(service.toString(), toSnsMessageStringValue(service, node));

		node = message.get("default");
		if (!Json.isNull(node))
			return Json.object("default", toSnsMessageStringValue(service, node));

		throw Exceptions.illegalArgument(//
				"no push message for default nor [%s] service", service);
	}

	static String toSnsMessageStringValue(PushService service, JsonNode message) {
		if (message.isObject())
			return message.toString();

		// be careful not to 'toString' simple text values
		// because it would double quote stringified objects
		if (message.isValueNode())
			return message.asText();

		throw Exceptions.illegalArgument("push message [%s][%s] invalid", service, message);
	}

	private ObjectNode badgeObjectMessage(DataObject<Installation> installation, //
			ObjectNode message, Credentials credentials, //
			ObjectNode logItem, BadgeStrategy badgeStrategy) {

		if (badgeStrategy == null || //
				BadgeStrategy.manual.equals(badgeStrategy))
			return message;

		if (PushService.APNS.equals(installation.source().pushService())//
				|| PushService.APNS_SANDBOX.equals(installation.source().pushService())) {

			if (BadgeStrategy.auto.equals(badgeStrategy)) {
				installation.source().badge(installation.source().badge() + 1);

				// update installation badge in data store
				DataObject<ObjectNode> patch = DataObjects.copyIdentity(//
						installation, new JsonDataObject())//
						.source(Json.object(BADGE, installation.source().badge()));

				DataStore.get().patchObject(patch);
			}

			message.with(installation.source().pushService().toString())//
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

		Credentials credentials = SpaceContext.credentials();
		DataObject<Installation> installation = new InstallationDataObject()//
				.type(TYPE).source(Json.toPojo(body, Installation.class));

		if (id.isPresent())
			installation.id(id.get());

		Installation source = installation.source();

		Check.notNullOrEmpty(source.token(), TOKEN);
		Check.notNullOrEmpty(source.appId(), APP_ID);
		Check.notNull(source.pushService(), PUSH_SERVICE);

		source.endpoint(//
				SpaceContext.isTest() ? "FAKE_ENDPOINT_FOR_TESTING" //
						: AwsSnsPusher.createApplicationEndpoint(source.appId(), //
								source.pushService(), source.token()));

		source.credentialsId(credentials.isAtLeastUser() ? credentials.id() : null);

		return id.isPresent() //
				? DataService.get().doPut(installation, false, context) //
				: DataService.get().doPost(installation);
	}

	private DataObject<Installation> load(String id) {
		return DataStore.get().getObject(new InstallationDataObject().id(id));
	}

	//
	// Singleton
	//

	private static PushSpaceService singleton = new PushSpaceService();

	public static PushSpaceService get() {
		return singleton;
	}

	private PushSpaceService() {
	}
}
