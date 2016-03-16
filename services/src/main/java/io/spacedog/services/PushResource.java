package io.spacedog.services;

import java.util.Arrays;
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
import com.amazonaws.services.sns.model.GetEndpointAttributesRequest;
import com.amazonaws.services.sns.model.GetEndpointAttributesResult;
import com.amazonaws.services.sns.model.InvalidParameterException;
import com.amazonaws.services.sns.model.ListPlatformApplicationsRequest;
import com.amazonaws.services.sns.model.ListPlatformApplicationsResult;
import com.amazonaws.services.sns.model.NotFoundException;
import com.amazonaws.services.sns.model.PlatformApplication;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.SetEndpointAttributesRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Iterators;

import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.SpaceParams;
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

	private AmazonSNSClient snsClient;

	public static final String TYPE = "installation";

	// installation field names
	public static final String APP_ID = "appId";
	public static final String USER_ID = "userId";
	public static final String DEVICE_TOKEN = "deviceToken";
	public static final String PROVIDER_ID = "providerId";
	public static final String TAGS = "tags";
	public static final String TAG_KEY = "key";
	public static final String TAG_VALUE = "value";

	// push field names
	private static final String MESSAGE = "message";

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
	// Routes
	//

	@Get("/installation")
	@Get("/installation/")
	public Payload getAll(Context context) {
		SpaceContext.checkAdminCredentials();
		return DataResource.get().getByType(TYPE, context);
	}

	@Post("/installation")
	@Post("/installation/")
	public Payload post(String body, Context context) {
		return upsertInstallation(Optional.empty(), body, context);
	}

	@Delete("/installation")
	@Delete("/installation/")
	public Payload deleteAll(Context context) {
		return DataResource.get().deleteByType(TYPE, context);
	}

	@Get("/installation/:id")
	@Get("/installation/:id/")
	public Payload get(String id, Context context) {
		return DataResource.get().getById(TYPE, id, context);
	}

	@Delete("/installation/:id")
	@Delete("/installation/:id/")
	public Payload delete(String id, Context context) {
		return DataResource.get().deleteById(TYPE, id, context);
	}

	@Put("/installation/:id")
	@Put("/installation/:id/")
	public Payload put(String id, String body, Context context) {
		return upsertInstallation(Optional.of(id), body, context);
	}

	@Get("/installation/:id/tags")
	@Get("/installation/:id/tags/")
	public Payload getTags(String id, Context context) {
		Credentials credentials = SpaceContext.checkCredentials();
		Optional<ObjectNode> object = DataStore.get().getObject(credentials.backendId(), TYPE, id);

		if (object.isPresent())
			return Payloads.json(//
					object.get().has(TAGS) //
							? object.get().get(TAGS) //
							: Json.newArrayNode());

		return Payloads.error(HttpStatus.NOT_FOUND);
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
	public Payload push(String body, Context context) {

		Credentials credentials = SpaceContext.checkUserCredentials();

		ObjectNode push = Json.readObjectNode(body);
		String appId = Json.checkStringNode(push, APP_ID, true).get().asText();
		JsonNode message = Json.checkJsonNode(push, MESSAGE, true).get();
		JsonNode tags = push.get(TAGS);

		BoolQueryBuilder query = QueryBuilders.boolQuery()//
				.filter(QueryBuilders.termQuery(APP_ID, appId));

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

		boolean refresh = context.query().getBoolean(SpaceParams.REFRESH, false);
		DataStore.get().refreshType(refresh, credentials.backendId(), TYPE);

		// TODO use a scroll to push to all installations found
		SearchHits hits = Start.get().getElasticClient()//
				.prepareSearch(credentials.backendId(), TYPE)//
				.setQuery(query)//
				.setFrom(0)//
				.setSize(1000)//
				.setVersion(false)//
				.setFetchSource(new String[] { USER_ID, DEVICE_TOKEN, PROVIDER_ID }, null)//
				.get()//
				.getHits();

		if (hits.totalHits() > 1000)
			return Payloads.error(HttpStatus.NOT_IMPLEMENTED, //
					"push to [%s] installations not yet implemented", hits.totalHits());

		JsonBuilder<ObjectNode> builder = Payloads.minimalBuilder(200)//
				.array("pushedTo");

		for (SearchHit hit : hits.getHits()) {
			ObjectNode installation = Json.readObjectNode(hit.sourceAsString());

			if (!isTest(context)) {
				PublishRequest request = new PublishRequest()//
						.withTargetArn(installation.get(PROVIDER_ID).asText());

				if (message.isObject()) {
					request.withMessageStructure("json")//
							.withMessage(message.toString());
				} else
					request.withMessage(message.asText());

				getSnsClient().publish(request);
			}

			builder.object()//
					.put(USER_ID, installation.get(USER_ID).asText())//
					.put(DEVICE_TOKEN, installation.get(DEVICE_TOKEN).asText())//
					.put(PROVIDER_ID, installation.get(PROVIDER_ID).asText())//
					.end();
		}

		return Payloads.json(builder);
	}

	//
	// Implementation
	//

	public Payload upsertInstallation(Optional<String> id, String body, Context context) {

		Credentials credentials = SpaceContext.checkCredentials();

		ObjectNode installation = Json.readObjectNode(body);
		String deviceToken = Json.checkStringNotNullOrEmpty(installation, DEVICE_TOKEN);
		String appId = Json.checkStringNotNullOrEmpty(installation, APP_ID);

		// remove all fields that should not be set by client code
		installation.remove(Arrays.asList(USER_ID, PROVIDER_ID));

		if (isTest(context)) {
			installation.set(PROVIDER_ID, TextNode.valueOf("XXXXXXXXXXXXXXXX"));
		} else {
			String providerId = createApplicationEndpoint(credentials.backendId(), appId, deviceToken);
			installation.set(PROVIDER_ID, TextNode.valueOf(providerId));
		}

		if (credentials.isAtLeastUser())
			installation.set(USER_ID, TextNode.valueOf(credentials.name()));

		if (id.isPresent()) {
			DataStore.get().patchObject(credentials.backendId(), TYPE, id.get(), installation, credentials.name());
			return Payloads.saved(false, credentials.backendId(), "/1", TYPE, id.get());
		} else
			return DataResource.get().post(TYPE, installation.toString(), context);

	}

	private Payload updateTags(String id, String body, boolean strict, boolean delete) {

		Credentials credentials = SpaceContext.checkCredentials();
		Optional<ObjectNode> installation = DataStore.get().getObject(credentials.backendId(), TYPE, id);

		if (!installation.isPresent())
			return Payloads.json(404);

		if (strict) {
			ArrayNode tags = Json.readArrayNode(body);
			installation.get().set(TAGS, tags);
		} else {
			ObjectNode newTag = Json.readObjectNode(body);
			String tagKey = Json.checkStringNotNullOrEmpty(newTag, TAG_KEY);
			String tagValue = Json.checkStringNotNullOrEmpty(newTag, TAG_VALUE);

			boolean updated = false;
			if (installation.get().has(TAGS)) {
				Iterator<JsonNode> tags = installation.get().get(TAGS).elements();
				while (tags.hasNext() && !updated) {
					ObjectNode tag = (ObjectNode) tags.next();
					if (tagKey.equals(tag.get(TAG_KEY).asText())//
							&& tagValue.equals(tag.get(TAG_VALUE).asText())) {
						if (delete) {
							tags.remove();
							break;
						}
						// tag already exists => nothing to save
						return Payloads.saved(false, credentials.backendId(), "/1", TYPE, id);
					}
				}
			}

			if (!delete)
				installation.get().withArray(TAGS).add(newTag);
		}

		DataStore.get().updateObject(credentials.backendId(), installation.get(), credentials.name());
		return Payloads.saved(false, credentials.backendId(), "/1", TYPE, id);
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

	Optional<PlatformApplication> getApplication(String backendId, String appId) {

		final String internalName = appId;
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

	String createApplicationEndpoint(String backendId, String appId, String deviceToken) {

		Optional<PlatformApplication> application = getApplication(backendId, appId);

		if (!application.isPresent())
			throw Exceptions.illegalArgument("invalid mobile	application id [%s]", appId);

		String applicationArn = application.get().getPlatformApplicationArn();

		String endpointArn = null;

		try {
			endpointArn = getSnsClient()
					.createPlatformEndpoint(//
							new CreatePlatformEndpointRequest()//
									.withPlatformApplicationArn(applicationArn)//
									.withToken(deviceToken))//
					.getEndpointArn();

		} catch (InvalidParameterException e) {
			String message = e.getErrorMessage();
			System.out.println("Exception message: " + message);
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

			updateNeeded = !endpointAttributes.getAttributes().get("Token").equals(deviceToken)
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
			attribs.put("Token", deviceToken);
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

	private static PushResource singleton = new PushResource();

	static PushResource get() {
		return singleton;
	}

	private PushResource() {
	}
}
