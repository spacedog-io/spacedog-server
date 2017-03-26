package io.spacedog.services;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.EndpointDisabledException;
import com.amazonaws.services.sns.model.GetEndpointAttributesRequest;
import com.amazonaws.services.sns.model.GetEndpointAttributesResult;
import com.amazonaws.services.sns.model.InvalidParameterException;
import com.amazonaws.services.sns.model.ListPlatformApplicationsRequest;
import com.amazonaws.services.sns.model.ListPlatformApplicationsResult;
import com.amazonaws.services.sns.model.NotFoundException;
import com.amazonaws.services.sns.model.PlatformApplication;
import com.amazonaws.services.sns.model.PlatformApplicationDisabledException;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.SetEndpointAttributesRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Iterators;

import io.spacedog.core.Json8;
import io.spacedog.model.Schema;
import io.spacedog.services.ElasticClient.Index;
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
public class PushResource2 extends Resource {

	private AmazonSNSClient snsClient;

	public static final String TYPE = "installation";

	// installation field names
	public static final String APP_ID = "appId";
	public static final String PUSH_SERVICE = "pushService";
	@Deprecated
	public static final String USER_ID = "userId";
	public static final String CREDENTIALS_NAME = "credentialsName";
	public static final String CREDENTIALS_ID = "credentialsId";
	public static final String TOKEN = "token";
	public static final String ENDPOINT = "endpoint";
	public static final String BADGE = "badge";
	public static final String TAGS = "tags";
	public static final String TAG_KEY = "key";
	public static final String TAG_VALUE = "value";

	// push response field names
	public static final String PUSHED_TO = "pushedTo";
	public static final String FAILURES = "failures";

	// push request field names
	private static final String BADGE_STRATEGY = "badgeStrategy";
	private static final String MESSAGE = "message";
	private static final String USERS_ONLY = "usersOnly";

	public static enum PushServices {
		APNS, // Apple Push Notification Service
		APNS_SANDBOX, // Sandbox version of APNS
		ADM, // Amazon Device Messaging
		GCM, // Google Cloud Messaging
		BAIDU, // Baidu CloudMessaging Service
		WNS, // Windows Notification Service
		MPNS; // Microsoft Push Notification Service
	}

	//
	// Schema
	//

	public static Schema getDefaultInstallationSchema() {
		return Schema.builder("installation")//
				.string(APP_ID)//
				.string(PUSH_SERVICE)//
				.string(TOKEN)//
				.string(ENDPOINT)//
				.string(USER_ID)//
				.integer(BADGE)//
				.string(CREDENTIALS_ID)//
				.string(CREDENTIALS_NAME)//
				.object(TAGS).array()//
				.string(TAG_KEY)//
				.string(TAG_VALUE)//
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

		Credentials credentials = SpaceContext.checkUserCredentials();

		JsonNode node = Json8.readNode(body);
		BadgeStrategy badge = BadgeStrategy.manual;
		ObjectNode objectMessage = null;

		if (node.has(MESSAGE)) {
			objectMessage = toObjectMessage(node.get(MESSAGE));
			badge = getBadgeStrategy(node);
		} else {
			objectMessage = toObjectMessage(node);
		}

		ObjectNode installation = DataStore.get().getObject(credentials.backendId(), TYPE, id);

		PushLog log = new PushLog();
		pushToInstallation(log, id, installation, objectMessage, credentials, badge);
		return log.toPayload();
	}

	@Get("/installation/:id/tags")
	@Get("/installation/:id/tags/")
	public Payload getTags(String id, Context context) {
		Credentials credentials = SpaceContext.getCredentials();
		ObjectNode object = DataStore.get().getObject(credentials.backendId(), TYPE, id);

		return JsonPayload.json(//
				object.has(TAGS) //
						? object.get(TAGS) //
						: Json8.array());
	}

	@Post("/installation/:id/tags")
	@Post("/installation/:id/tags/")
	public Payload postTags(String id, String body, Context context) {
		return updateTags(id, body, false, false);
	}

	@Put("/installation/:id/tags")
	@Put("/installation/:id/tags/")
	public Payload putTags(String id, String body, Context context) {
		return updateTags(id, body, true, false);

	}

	@Delete("/installation/:id/tags")
	@Delete("/installation/:id/tags/")
	public Payload deleteTags(String id, String body, Context context) {
		return updateTags(id, body, false, true);
	}

	/**
	 * Check this page for specific json messages:
	 * http://docs.aws.amazon.com/sns/latest/dg/mobile-push-send-custommessage.
	 * html
	 */
	@Post("/push")
	@Post("/push/")
	@Post("/installation/push")
	@Post("/installation/push/")
	public Payload pushByTags(String body, Context context) {

		Credentials credentials = SpaceContext.checkUserCredentials();

		ObjectNode push = Json8.readObject(body);
		String appId = Json8.checkStringNotNullOrEmpty(push, APP_ID);
		JsonNode message = Json8.checkNode(push, MESSAGE, true).get();
		BadgeStrategy badge = getBadgeStrategy(push);
		ObjectNode objectMessage = toObjectMessage(message);

		BoolQueryBuilder query = QueryBuilders.boolQuery()//
				.filter(QueryBuilders.termQuery(APP_ID, appId));

		Optional<String> service = Json8.checkString(push, PUSH_SERVICE);
		if (service.isPresent())
			query.filter(QueryBuilders.termQuery(PUSH_SERVICE, service.get()));

		boolean usersOnly = Json8.checkBoolean(push, USERS_ONLY, false);
		if (usersOnly)
			query.filter(QueryBuilders.existsQuery(CREDENTIALS_NAME));

		Optional<String> credentialsId = Json8.checkString(push, CREDENTIALS_ID);
		if (credentialsId.isPresent())
			query.filter(QueryBuilders.termQuery(CREDENTIALS_ID, credentialsId.get()));

		Optional<String> credentialsName = Json8.checkString(push, CREDENTIALS_NAME);
		if (credentialsName.isPresent())
			query.filter(QueryBuilders.termQuery(CREDENTIALS_NAME, credentialsName.get()));

		JsonNode tags = push.get(TAGS);
		if (tags != null) {
			Iterator<JsonNode> tagsIterator = tags.isObject()//
					? Iterators.singletonIterator(tags) : tags.elements();

			while (tagsIterator.hasNext()) {
				JsonNode tag = tagsIterator.next();
				query.filter(//
						QueryBuilders.termQuery(//
								toFieldPath(TAGS, TAG_KEY), //
								tag.get(TAG_KEY).asText()))//
						.filter(QueryBuilders.termQuery(//
								toFieldPath(TAGS, TAG_VALUE), //
								tag.get(TAG_VALUE).asText()));
			}
		}

		boolean refresh = context.query().getBoolean(PARAM_REFRESH, false);
		DataStore.get().refreshType(refresh, credentials.backendId(), TYPE);

		// TODO use a scroll to push to all installations found
		SearchHits hits = Start.get().getElasticClient()//
				.prepareSearch(credentials.backendId(), TYPE)//
				.setQuery(query)//
				.setFrom(0)//
				.setSize(1000)//
				.setVersion(false)//
				.setFetchSource(new String[] { USER_ID, CREDENTIALS_NAME, ENDPOINT, PUSH_SERVICE, BADGE }, null)//
				.get()//
				.getHits();

		if (hits.totalHits() > 1000)
			return JsonPayload.error(HttpStatus.NOT_IMPLEMENTED, //
					"push to [%s] installations is a premium feature", hits.totalHits());

		PushLog log = new PushLog();

		for (SearchHit hit : hits.getHits()) {
			ObjectNode installation = Json8.readObject(hit.sourceAsString());
			pushToInstallation(log, hit.getId(), installation, objectMessage, credentials, badge);
			if (log.applicationDisabled)
				break;
		}

		return log.toPayload();
	}

	//
	// Implementation
	//

	public static enum BadgeStrategy {
		manual, semi, auto
	}

	private static class PushLog {
		public boolean successes;
		public boolean failures;
		public ArrayNode logItems = Json8.array();
		public boolean applicationDisabled;

		public Payload toPayload() {
			int httpStatus = logItems.size() == 0 ? HttpStatus.NOT_FOUND //
					: successes ? HttpStatus.OK : HttpStatus.BAD_REQUEST;

			JsonBuilder<ObjectNode> builder = JsonPayload.builder(httpStatus)//
					.put(FAILURES, failures)//
					.put("applicationDisabled", applicationDisabled)//
					.node(PUSHED_TO, logItems);

			return JsonPayload.json(builder, httpStatus);
		}
	}

	private void pushToInstallation(PushLog log, String installationId, //
			ObjectNode installation, ObjectNode message, Credentials credentials, BadgeStrategy badgeStrategy) {

		ObjectNode logItem = Json8.object("installationId", installationId);
		log.logItems.add(logItem);

		try {

			Json8.checkString(installation, USER_ID)//
					.ifPresent(userId -> logItem.put(USER_ID, userId));
			Json8.checkString(installation, CREDENTIALS_NAME)//
					.ifPresent(name -> logItem.put(CREDENTIALS_NAME, name));

			PushServices service = PushServices.valueOf(//
					Json8.get(installation, PUSH_SERVICE).asText());

			message = badgeObjectMessage(installationId, installation, service, message, //
					credentials, logItem, badgeStrategy);

			String endpoint = Json8.checkStringNotNullOrEmpty(installation, ENDPOINT);
			ObjectNode snsMessage = toSnsMessage(service, message);

			if (!SpaceContext.isTest()) {
				PublishRequest pushRequest = new PublishRequest()//
						.withTargetArn(endpoint)//
						.withMessageStructure("json")//
						.withMessage(snsMessage.toString());

				getSnsClient().publish(pushRequest);
			} else
				logItem.set("message", snsMessage);

			log.successes = true;

		} catch (Exception e) {
			log.failures = true;
			logItem.set(FIELD_ERROR, JsonPayload.toJson(e, SpaceContext.isDebug()));

			if (e instanceof EndpointDisabledException) {
				logItem.put("installationDisabled", true);
				removeEndpointQuietly(credentials.backendId(), installationId);
			}

			if (e instanceof PlatformApplicationDisabledException)
				log.applicationDisabled = true;
		}
	}

	void updateSchema() {
		ElasticClient elastic = Start.get().getElasticClient();
		Index[] indices = elastic.toIndicesForSchema(TYPE);
		for (Index index : indices) {
			String mapping = getDefaultInstallationSchema().validate().translate().toString();
			elastic.putMapping(index.backendId, index.schemaName, mapping);
			Utils.info("Mapping updated for index [%s][%s].", //
					index.backendId, index.schemaName);
		}
	}

	private BadgeStrategy getBadgeStrategy(JsonNode node) {
		Optional<String> optional = Json8.checkString(node, BADGE_STRATEGY);
		return optional.isPresent() //
				? BadgeStrategy.valueOf(optional.get()) //
				: BadgeStrategy.manual;
	}

	static ObjectNode toObjectMessage(JsonNode message) {

		if (message.isObject())
			return (ObjectNode) message;

		if (message.isTextual()) {
			String text = message.asText();
			return Json8.object("default", text, //
					"APNS", Json8.object("aps", Json8.object("alert", text)), //
					"APNS_SANDBOX", Json8.object("aps", Json8.object("alert", text)), //
					"GCM", Json8.object("data", Json8.object("message", text)));
		}

		throw Exceptions.illegalArgument("push message [%s][%s] is invalid", //
				message.getNodeType(), message);
	}

	static ObjectNode toSnsMessage(PushServices service, ObjectNode message) {

		JsonNode node = message.get(service.toString());
		if (!Json8.isNull(node))
			return Json8.object(service.toString(), toSnsMessageStringValue(service, node));

		node = message.get("default");
		if (!Json8.isNull(node))
			return Json8.object("default", toSnsMessageStringValue(service, node));

		throw Exceptions.illegalArgument(//
				"no push message for default nor [%s] service", service);
	}

	static String toSnsMessageStringValue(PushServices service, JsonNode message) {
		if (message.isObject())
			return message.toString();

		// be careful not to 'toString' simple text values
		// because it would double quote stringified objects
		if (message.isValueNode())
			return message.asText();

		throw Exceptions.illegalArgument("push message [%s][%s] invalid", service, message);
	}

	private ObjectNode badgeObjectMessage(String installationId, ObjectNode installation, //
			PushServices pushService, ObjectNode message, Credentials credentials, //
			ObjectNode logItem, BadgeStrategy badgeStrategy) {

		if (BadgeStrategy.manual.equals(badgeStrategy))
			return message;

		if (PushServices.APNS.equals(pushService)//
				|| PushServices.APNS_SANDBOX.equals(pushService)) {

			int badge = Json8.checkInteger(installation, BADGE).orElse(0);

			if (BadgeStrategy.auto.equals(badgeStrategy)) {
				badge = badge + 1;
				// update installation badge in data store
				DataStore.get().patchObject(credentials.backendId(), //
						TYPE, installationId, Json8.object(BADGE, badge), credentials.name());
			}

			message.with(pushService.toString()).with("aps").put(BADGE, badge);
		}
		return message;
	}

	private void removeEndpointQuietly(String backend, String id) {
		try {
			Start.get().getElasticClient().delete(backend, TYPE, id, false, true);
		} catch (Exception e) {
			System.err.println(String.format(//
					"[Warning] failed to delete disabled installation [%s][%s]", backend, id));
			e.printStackTrace();
		}
	}

	public Payload upsertInstallation(Optional<String> id, String body, Context context) {

		Credentials credentials = SpaceContext.getCredentials();
		ObjectNode installation = Json8.readObject(body);

		// check request is not trying to set private fields
		Json8.checkNull(installation, USER_ID);
		Json8.checkNull(installation, CREDENTIALS_ID);
		Json8.checkNull(installation, CREDENTIALS_NAME);
		Json8.checkNull(installation, ENDPOINT);

		Optional<String> token = Json8.checkString(installation, TOKEN);

		if (token.isPresent())
			return upsertInstallationWithToken(id, credentials, installation, token.get(), context);

		if (id.isPresent()) {
			// these fields must not be set directly
			// when no token is specified
			Json8.checkNull(installation, APP_ID);
			Json8.checkNull(installation, PUSH_SERVICE);

			return DataResource.get().put(TYPE, id.get(), body, context);
		}

		throw Exceptions.illegalArgument("no [token] field specified");
	}

	Payload upsertInstallationWithToken(Optional<String> id, Credentials credentials, //
			ObjectNode installation, String token, Context context) {

		String appId = Json8.checkStringNotNullOrEmpty(installation, APP_ID);
		PushServices service = PushServices.valueOf(//
				Json8.checkStringNotNullOrEmpty(installation, PUSH_SERVICE));

		if (SpaceContext.isTest()) {
			installation.set(ENDPOINT, TextNode.valueOf("FAKE_ENDPOINT_FOR_TESTING"));
		} else {
			String endpoint = createApplicationEndpoint(credentials.backendId(), appId, service, token);
			installation.put(ENDPOINT, endpoint);
		}

		if (credentials.isReal()) {
			installation.put(USER_ID, credentials.name());
			installation.put(CREDENTIALS_NAME, credentials.name());
			installation.put(CREDENTIALS_ID, credentials.id());
		}

		if (id.isPresent()) {
			DataStore.get().patchObject(credentials.backendId(), TYPE, id.get(), installation, credentials.name());
			return JsonPayload.saved(false, credentials.backendId(), "/1", TYPE, id.get());
		} else
			return DataResource.get().post(TYPE, installation.toString(), context);
	}

	private Payload updateTags(String id, String body, boolean strict, boolean delete) {

		Credentials credentials = SpaceContext.getCredentials();
		ObjectNode installation = DataStore.get().getObject(credentials.backendId(), TYPE, id);

		if (strict) {
			ArrayNode tags = Json8.readArray(body);
			installation.set(TAGS, tags);
		} else {
			ObjectNode newTag = Json8.readObject(body);
			String tagKey = Json8.checkStringNotNullOrEmpty(newTag, TAG_KEY);
			String tagValue = Json8.checkStringNotNullOrEmpty(newTag, TAG_VALUE);

			boolean updated = false;
			if (installation.has(TAGS)) {
				Iterator<JsonNode> tags = installation.get(TAGS).elements();
				while (tags.hasNext() && !updated) {
					ObjectNode tag = (ObjectNode) tags.next();
					if (tagKey.equals(tag.get(TAG_KEY).asText())//
							&& tagValue.equals(tag.get(TAG_VALUE).asText())) {
						if (delete) {
							tags.remove();
							break;
						}
						// tag already exists => nothing to save
						return JsonPayload.saved(false, credentials.backendId(), "/1", TYPE, id);
					}
				}
			}

			if (!delete)
				installation.withArray(TAGS).add(newTag);
		}

		DataStore.get().updateObject(credentials.backendId(), installation, credentials.name());
		return JsonPayload.saved(false, credentials.backendId(), "/1", TYPE, id);
	}

	private String toFieldPath(String... strings) {
		return String.join(".", strings);
	}

	AmazonSNSClient getSnsClient() {
		if (snsClient == null) {
			snsClient = new AmazonSNSClient();
			snsClient.setRegion(Region.getRegion(Regions.EU_WEST_1));
		}
		return snsClient;
	}

	Optional<PlatformApplication> getApplication(String backendId, String appId, PushServices service) {

		final String internalName = String.join("/", "app", service.toString(), appId);
		Optional<String> nextToken = Optional.empty();

		do {
			ListPlatformApplicationsRequest listAppRequest = new ListPlatformApplicationsRequest();

			if (nextToken.isPresent())
				listAppRequest.withNextToken(nextToken.get());

			ListPlatformApplicationsResult listAppResult = getSnsClient().listPlatformApplications(listAppRequest);

			nextToken = Optional.ofNullable(listAppResult.getNextToken());

			for (PlatformApplication application : listAppResult.getPlatformApplications())
				if (application.getPlatformApplicationArn().endsWith(internalName))
					return Optional.of(application);

		} while (nextToken.isPresent());

		return Optional.empty();
	}

	String createApplicationEndpoint(String backendId, String appId, PushServices service, String token) {

		PlatformApplication application = getApplication(backendId, appId, service)//
				.orElseThrow(//
						() -> Exceptions.illegalArgument(//
								"push service [%s] not registered for mobile	application [%s]", //
								appId, service));

		String endpointArn = null;
		String applicationArn = application.getPlatformApplicationArn();

		try {
			endpointArn = getSnsClient()
					.createPlatformEndpoint(//
							new CreatePlatformEndpointRequest()//
									.withPlatformApplicationArn(applicationArn)//
									.withToken(token))//
					.getEndpointArn();

		} catch (InvalidParameterException e) {
			String message = e.getErrorMessage();
			Utils.info("Exception message: %s", message);
			Pattern p = Pattern.compile(".*Endpoint (arn:aws:sns[^ ]+) already exists " + "with the same token.*");
			Matcher m = p.matcher(message);
			if (m.matches()) {
				// The platform endpoint already exists for this token, but with
				// additional custom data that
				// createEndpoint doesn't want to overwrite. Just use the
				// existing platform endpoint.
				endpointArn = m.group(1);
			} else {
				throw e;
			}
		}

		if (endpointArn == null)
			throw new RuntimeException("failed to create device notification endpoint: try again later");

		boolean updateNeeded = false;

		try {
			GetEndpointAttributesResult endpointAttributes = getSnsClient()
					.getEndpointAttributes(new GetEndpointAttributesRequest().withEndpointArn(endpointArn));

			updateNeeded = !endpointAttributes.getAttributes().get("Token").equals(token)
					|| !endpointAttributes.getAttributes().get("Enabled").equalsIgnoreCase("true");

		} catch (NotFoundException nfe) {
			// We had a stored ARN, but the platform endpoint associated with it
			// disappeared. Recreate it.
			endpointArn = null;
		}

		if (endpointArn == null)
			throw new RuntimeException("failed to create device notification endpoint: try again later");

		if (updateNeeded) {
			// The platform endpoint is out of sync with the current data;
			// update the token and enable it.
			Map<String, String> attribs = new HashMap<String, String>();
			attribs.put("Token", token);
			attribs.put("Enabled", "true");
			getSnsClient().setEndpointAttributes(//
					new SetEndpointAttributesRequest()//
							.withEndpointArn(endpointArn)//
							.withAttributes(attribs));
		}

		return endpointArn;
	}

	//
	// Singleton
	//

	private static PushResource2 singleton = new PushResource2();

	static PushResource2 get() {
		return singleton;
	}

	private PushResource2() {
	}
}
