package io.spacedog.services;

import java.util.Optional;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.joda.time.DateTime;

import com.amazonaws.services.sns.model.EndpointDisabledException;
import com.amazonaws.services.sns.model.PlatformApplicationDisabledException;
import com.amazonaws.services.sns.model.PublishRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import io.spacedog.core.Json8;
import io.spacedog.model.BadgeStrategy;
import io.spacedog.model.Installation;
import io.spacedog.model.PushService;
import io.spacedog.model.Schema;
import io.spacedog.sdk.PushRequest;
import io.spacedog.services.DataStore.Meta;
import io.spacedog.utils.Check;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.JsonBuilder;
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
public class PushResource extends Resource {

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
		return Schema.builder("installation")//
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
		return DataResource.get().getByType(TYPE, context);
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
		return DataResource.get().deleteByType(TYPE, context);
	}

	@Get("/installation/:id")
	@Get("/installation/:id/")
	@Get("/data/installation/:id")
	@Get("/data/installation/:id/")
	public Payload get(String id, Context context) {
		return DataResource.get().getById(TYPE, id, context);
	}

	@Delete("/installation/:id")
	@Delete("/installation/:id/")
	@Delete("/data/installation/:id")
	@Delete("/data/installation/:id/")
	public Payload delete(String id, Context context) {
		return DataResource.get().deleteById(TYPE, id, context);
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
		PushRequest request = Json8.toPojo(body, PushRequest.class);
		Installation installation = load(id);

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
		Installation installation = load(id);
		String[] tags = Json8.toPojo(body, String[].class);
		installation.tags().addAll(Sets.newHashSet(tags));
		update(installation, SpaceContext.credentials());
		return JsonPayload.saved(false, "/1", TYPE, id);
	}

	@Put("/installation/:id/tags")
	@Put("/installation/:id/tags/")
	public Payload putTags(String id, String body, Context context) {
		return putField(id, TAGS, body, context);
	}

	@Delete("/installation/:id/tags")
	@Delete("/installation/:id/tags/")
	public Payload deleteTags(String id, String body, Context context) {
		Installation installation = load(id);
		String[] tags = Json8.toPojo(body, String[].class);
		installation.tags().removeAll(Sets.newHashSet(tags));
		update(installation, SpaceContext.credentials());
		return JsonPayload.saved(false, "/1", TYPE, id);
	}

	@Get("/installation/:id/:field")
	@Get("/installation/:id/:field/")
	public Payload getField(String id, String field, Context context) {
		return DataResource.get().getField(TYPE, id, field, context);
	}

	@Put("/installation/:id/:field")
	@Put("/installation/:id/:field/")
	public Payload putField(String id, String field, String body, Context context) {
		return DataResource.get().putField(TYPE, id, field, body, context);
	}

	@Delete("/installation/:id/:field")
	@Delete("/installation/:id/:field/")
	public Payload deleteField(String id, String field, Context context) {
		return DataResource.get().deleteField(TYPE, id, field, context);
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
		PushRequest request = Json8.toPojo(body, PushRequest.class);
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
		SearchHits hits = Start.get().getElasticClient()//
				.prepareSearch(DataStore.toDataIndex(TYPE))//
				.setQuery(query)//
				.setFrom(0)//
				.setSize(1000)//
				.setVersion(false)//
				.setFetchSource(new String[] { CREDENTIALS_ID, ENDPOINT, PUSH_SERVICE, BADGE }, null)//
				.get()//
				.getHits();

		if (hits.totalHits() > 1000)
			return JsonPayload.error(HttpStatus.NOT_IMPLEMENTED, //
					"push to [%s] installations is a premium feature", hits.totalHits());

		PushLog log = new PushLog();

		for (SearchHit hit : hits.getHits()) {
			Installation installation = Json8.toPojo(hit.source(), Installation.class)//
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
		public ArrayNode logItems = Json8.array();
		public boolean applicationDisabled;

		public Payload toPayload() {
			int httpStatus = logItems.size() == 0 ? HttpStatus.NOT_FOUND //
					: successes > 0 ? HttpStatus.OK : HttpStatus.BAD_REQUEST;

			JsonBuilder<ObjectNode> builder = JsonPayload.builder(httpStatus)//
					.put(FAILURES, failures)//
					.put("applicationDisabled", applicationDisabled)//
					.node(PUSHED_TO, logItems);

			return JsonPayload.json(builder, httpStatus);
		}

		public ObjectNode toNode() {
			return Json8.object(FAILURES, failures, //
					"applicationDisabled", applicationDisabled, //
					PUSHED_TO, logItems);
		}
	}

	public void pushToInstallation(PushLog log, Installation installation, //
			ObjectNode jsonMessage, Credentials credentials, BadgeStrategy badgeStrategy) {

		ObjectNode logItem = Json8.object("installationId", installation.id());
		log.logItems.add(logItem);

		try {

			if (!Strings.isNullOrEmpty(installation.credentialsId()))
				logItem.put(CREDENTIALS_ID, installation.credentialsId());

			jsonMessage = badgeObjectMessage(installation, jsonMessage, //
					credentials, logItem, badgeStrategy);

			ObjectNode snsMessage = toSnsMessage(installation.pushService(), jsonMessage);

			if (!SpaceContext.isTest()) {
				PublishRequest pushRequest = new PublishRequest()//
						.withTargetArn(installation.endpoint())//
						.withMessageStructure("json")//
						.withMessage(snsMessage.toString());

				AwsSnsPusher.getSnsClient().publish(pushRequest);
			} else
				logItem.set("message", snsMessage);

			log.successes = log.successes + 1;

		} catch (Exception e) {
			log.failures = true;
			logItem.set(FIELD_ERROR, JsonPayload.toJson(e, SpaceContext.isDebug()));

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
				? Json8.object("default", request.text, //
						"APNS", Json8.object("aps", Json8.object("alert", request.text)), //
						"APNS_SANDBOX", Json8.object("aps", Json8.object("alert", request.text)), //
						"GCM", Json8.object("data", Json8.object("message", request.text)))//
				: request.data;
	}

	static ObjectNode toSnsMessage(PushService service, ObjectNode message) {

		JsonNode node = message.get(service.toString());
		if (!Json8.isNull(node))
			return Json8.object(service.toString(), toSnsMessageStringValue(service, node));

		node = message.get("default");
		if (!Json8.isNull(node))
			return Json8.object("default", toSnsMessageStringValue(service, node));

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

	private ObjectNode badgeObjectMessage(Installation installation, //
			ObjectNode message, Credentials credentials, //
			ObjectNode logItem, BadgeStrategy badgeStrategy) {

		if (badgeStrategy == null || //
				BadgeStrategy.manual.equals(badgeStrategy))
			return message;

		if (PushService.APNS.equals(installation.pushService())//
				|| PushService.APNS_SANDBOX.equals(installation.pushService())) {

			if (BadgeStrategy.auto.equals(badgeStrategy)) {
				installation.badge(installation.badge() + 1);
				// update installation badge in data store
				DataStore.get().patchObject(TYPE, installation.id(), //
						Json8.object(BADGE, installation.badge()), credentials.name());
			}

			message.with(installation.pushService().toString())//
					.with("aps").put(BADGE, installation.badge());
		}
		return message;
	}

	private void removeEndpointQuietly(String id) {
		try {
			Start.get().getElasticClient().delete(DataStore.toDataIndex(TYPE), id, false, true);
		} catch (Exception e) {
			System.err.println(String.format(//
					"[Warning] failed to delete disabled installation [%s]", id));
			e.printStackTrace();
		}
	}

	public Payload upsertInstallation(Optional<String> id, String body, Context context) {

		Credentials credentials = SpaceContext.credentials();
		Installation installation = Json8.toPojo(body, Installation.class);

		Check.notNullOrEmpty(installation.token(), TOKEN);
		Check.notNullOrEmpty(installation.appId(), APP_ID);
		Check.notNull(installation.pushService(), PUSH_SERVICE);

		installation.endpoint(//
				SpaceContext.isTest() ? "FAKE_ENDPOINT_FOR_TESTING" //
						: AwsSnsPusher.createApplicationEndpoint(installation.appId(), //
								installation.pushService(), installation.token()));

		installation.credentialsId(credentials.isAtLeastUser() ? credentials.id() : null);

		ObjectNode object = Json8.checkObject(Json8.toNode(installation));
		return id.isPresent() //
				? DataResource.get().put(TYPE, id.get(), object, context) //
				: DataResource.get().post(TYPE, id, object);
	}

	private Installation load(String id) {
		return DataStore.get().getObject(TYPE, id, Installation.class);
	}

	private Installation update(Installation installation, Credentials credentials) {
		Meta meta = new Meta();
		meta.createdAt = installation.createdAt();
		meta.createdBy = installation.createdBy();
		meta.updatedAt = DateTime.now();
		meta.updatedBy = credentials.name();
		meta.version = installation.version();

		IndexResponse response = DataStore.get()//
				.updateObject(TYPE, installation.id(), //
						Json8.checkObject(Json8.toNode(installation)), //
						meta);

		installation.updatedBy(meta.updatedBy);
		installation.updatedAt(meta.updatedAt);
		installation.version(response.getVersion());
		return installation;
	}

	//
	// Singleton
	//

	private static PushResource singleton = new PushResource();

	public static PushResource get() {
		return singleton;
	}

	private PushResource() {
	}
}
