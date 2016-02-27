package io.spacedog.services;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.joda.time.DateTime;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.GetEndpointAttributesRequest;
import com.amazonaws.services.sns.model.GetEndpointAttributesResult;
import com.amazonaws.services.sns.model.InvalidParameterException;
import com.amazonaws.services.sns.model.ListPlatformApplicationsRequest;
import com.amazonaws.services.sns.model.ListPlatformApplicationsResult;
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicResult;
import com.amazonaws.services.sns.model.ListTopicsResult;
import com.amazonaws.services.sns.model.NotFoundException;
import com.amazonaws.services.sns.model.PlatformApplication;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.amazonaws.services.sns.model.SetEndpointAttributesRequest;
import com.amazonaws.services.sns.model.Subscription;
import com.amazonaws.services.sns.model.Topic;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;

import io.spacedog.utils.Check;
import io.spacedog.utils.Json;
import net.codestory.http.Context;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.payload.Payload;

@Prefix("/v1")
public class PushResource {

	public static final String TYPE = "device";
	private AmazonSNSClient snsClient;

	@Post("/device")
	@Post("/device/")
	public Payload postDevice(String body, Context context) {

		Credentials credentials = SpaceContext.checkUserCredentials();

		ObjectNode node = Json.readObjectNode(body);
		String protocol = Json.checkStringNotNullOrEmpty(node, "protocol");
		String endpoint = Json.checkStringNotNullOrEmpty(node, "endpoint");

		if (!protocol.equals("application"))
			throw new IllegalArgumentException(String.format("invalid protocol [%s]", protocol));

		String appName = Json.checkStringNotNullOrEmpty(node, "appName");
		Optional<PlatformApplication> application = getApplication(credentials.backendId(), appName);
		if (!application.isPresent())
			throw new IllegalArgumentException(String.format("invalid mobile application name [%s]", appName));

		String endpointArn = createApplicationEndpoint(application.get().getPlatformApplicationArn(), endpoint);

		// TODO handle more than one app
		// TODO handle more than one endpoint per app
		DataStore.get().patchObject(credentials.backendId(), //
				UserResource.USER_TYPE, //
				credentials.name(), //
				Json.objectBuilder().put(UserResource.ENDPOINT_ARN, endpointArn).build(), //
				credentials.name());

		String topicArn = createTopic("all", credentials.backendId());
		Optional<Subscription> subscription = getSubscription(topicArn, protocol, endpoint);

		if (!subscription.isPresent()) {
			getSnsClient().subscribe(topicArn, //
					protocol, endpoint);
		}

		try {
			return Payloads.saved(false, "/v1", TYPE, URLEncoder.encode(endpointArn, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	@Post("/device/:id/topics")
	@Post("/device/:id/topics/")
	public Payload postDeviceToTopic(String id, String body, Context context) {

		Credentials credentials = SpaceContext.checkUserCredentials();

		ObjectNode node = Json.readObjectNode(body);
		JsonNode topics = Json.checkArrayNode(node, "topics", true).get();

		String endpointArn;
		try {
			endpointArn = URLEncoder.encode(id, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}

		String endpoint = getSnsClient().getEndpointAttributes(//
				new GetEndpointAttributesRequest()//
						.withEndpointArn(endpointArn))
				.getAttributes().get("Token");

		topics.elements().forEachRemaining(element -> {
			String topicName = Json.checkString(element);
			String topicArn = createTopic(topicName, credentials.backendId());
			Optional<Subscription> subscription = getSubscription(topicArn, "application", endpoint);

			if (!subscription.isPresent())
				getSnsClient().subscribe(topicArn, "application", endpoint);
		});

		return Payloads.saved(false, "/v1", "device", id);
	}

	@Post("/topic/:name/push")
	@Post("/topic/:name/push/")
	public Payload pushToTopic(String body, String name, Context context) {

		Credentials credentials = SpaceContext.checkAdminCredentials();

		Check.notNullOrEmpty(name, "topic name");
		ObjectNode node = Json.readObjectNode(body);
		String message = Json.checkStringNotNullOrEmpty(node, "message");

		Optional<Topic> topic = getTopicArn(name, credentials.backendId());

		if (!topic.isPresent())
			return Payloads.error(404, "topic with name [%s] not found", name);

		PublishResult publish = getSnsClient().publish(new PublishRequest()//
				.withTopicArn(topic.get().getTopicArn())//
				.withSubject(message)//
				.withMessage(message));

		return Payloads.json(Payloads.minimalBuilder(200)//
				.put("messageId", publish.getMessageId()).build(), 200);
	}

	@Post("/device/push")
	@Post("/device/push/")
	public Payload postPushAll(Context context) {

		Credentials credentials = SpaceContext.checkAdminCredentials();
		Optional<Topic> topic = getTopicArn("all", credentials.backendId());

		if (!topic.isPresent())
			throw new IllegalStateException(String.format(//
					"topic [all] not found for backend [%s]", credentials.backendId()));

		String msg = "PUSH " + credentials.backendId() + ' ' + DateTime.now();
		PublishResult publish = getSnsClient().publish(new PublishRequest()//
				.withTopicArn(topic.get().getTopicArn())//
				.withSubject(msg)//
				.withMessage(msg));

		return Payloads.json(Payloads.minimalBuilder(200)//
				.put("messageId", publish.getMessageId()).build(), 200);
	}

	@Post("/push")
	@Post("/push/")
	public Payload postPushBySearch(String body, Context context) {

		Credentials credentials = SpaceContext.checkAdminCredentials();
		ObjectNode node = Json.readObjectNode(body);
		String type = Json.checkStringNode(node, "type", true).get().asText();
		JsonNode query = Json.checkObject(node, "query", true).get();
		JsonNode properties = Json.checkArrayNode(node, "properties", true).get();
		String message = Json.checkStringNode(node, "message", true).get().asText();

		Set<String> usernames = getUsernamesFromObjects(credentials.backendId(), type, query, properties);
		Set<String> endpointArns = getEndpointArnsFromUsers(credentials, usernames);

		for (String endpointArn : endpointArns) {
			getSnsClient().publish(new PublishRequest()//
					.withTargetArn(endpointArn)//
					.withMessage(message));
		}

		return Payloads.success();
	}

	//
	// implementation
	//

	private Set<String> getEndpointArnsFromUsers(Credentials credentials, Set<String> usernames) {

		// TODO how many users can I get in one request ?
		MultiGetResponse multiGet = Start.get().getElasticClient().multiGet(//
				credentials.backendId(), UserResource.USER_TYPE, usernames);

		Set<String> endpointArns = Sets.newHashSet();
		multiGet.forEach(response -> endpointArns
				.add(response.getResponse().getSourceAsMap().get(UserResource.ENDPOINT_ARN).toString()));

		return endpointArns;
	}

	private Set<String> getUsernamesFromObjects(String backendId, String type, JsonNode query, JsonNode properties) {

		SearchResponse response = Start.get().getElasticClient()//
				.prepareSearch(backendId, type)//
				.setFrom(0)//
				// TODO get objects 100 by 100
				.setSize(1000)//
				.setFetchSource(true)//
				.setSource(query.toString())//
				.get();

		Set<String> usernames = Sets.newHashSet();
		for (SearchHit hit : response.getHits().getHits()) {
			ObjectNode object = Json.readObjectNode(hit.sourceAsString());
			for (Iterator<JsonNode> iterator = properties.elements(); iterator.hasNext();)
				usernames.add(Json.get(object, iterator.next().asText()).asText());
		}

		return usernames;
	}

	AmazonSNSClient getSnsClient() {
		if (snsClient == null) {
			snsClient = new AmazonSNSClient();
			snsClient.setRegion(Region.getRegion(Regions.EU_WEST_1));
		}
		return snsClient;
	}

	Optional<PlatformApplication> getApplication(String backendId, String appName) {

		final String internalName = backendId + '-' + appName;
		Optional<String> nextToken = Optional.empty();

		do {

			ListPlatformApplicationsResult listApplications = nextToken.isPresent()//
					? getSnsClient().listPlatformApplications(
							new ListPlatformApplicationsRequest().withNextToken(nextToken.get()))//
					: getSnsClient().listPlatformApplications();

			nextToken = Optional.ofNullable(listApplications.getNextToken());

			Optional<PlatformApplication> myTopic = listApplications.getPlatformApplications().stream()//
					.filter(application -> application.getAttributes().get("name").equals(internalName))//
					.findAny();

			if (myTopic.isPresent())
				return myTopic;

		} while (nextToken.isPresent());

		return Optional.empty();
	}

	String createApplicationEndpoint(String applicationArn, String endpoint) {

		String endpointArn = null;

		try {
			endpointArn = getSnsClient()
					.createPlatformEndpoint(//
							new CreatePlatformEndpointRequest()//
									.withPlatformApplicationArn(applicationArn)//
									.withToken(endpoint))//
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

			updateNeeded = !endpointAttributes.getAttributes().get("Token").equals(endpoint)
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
			attribs.put("Token", endpoint);
			attribs.put("Enabled", "true");
			getSnsClient().setEndpointAttributes(//
					new SetEndpointAttributesRequest()//
							.withEndpointArn(endpointArn)//
							.withAttributes(attribs));
		}

		return endpointArn;
	}

	String createTopic(String name, String backendId) {
		final String internalName = backendId + '-' + name;
		return getSnsClient().createTopic(internalName).getTopicArn();
	}

	Optional<Topic> getTopicArn(String name, String backendId) {

		final String internalName = backendId + '-' + name;
		Optional<String> nextToken = Optional.empty();

		do {

			ListTopicsResult listTopics = nextToken.isPresent()//
					? getSnsClient().listTopics(nextToken.get())//
					: getSnsClient().listTopics();

			nextToken = Optional.ofNullable(listTopics.getNextToken());

			Optional<Topic> myTopic = listTopics.getTopics().stream()//
					.filter(topic -> topic.getTopicArn().endsWith(internalName))//
					.findAny();

			if (myTopic.isPresent())
				return myTopic;

		} while (nextToken.isPresent());

		return Optional.empty();
	}

	Optional<Subscription> getSubscription(String topicArn, String protocol, String endpoint) {

		Optional<String> nextToken = Optional.empty();

		do {

			ListSubscriptionsByTopicResult listSubscriptions = nextToken.isPresent()//
					? getSnsClient().listSubscriptionsByTopic(topicArn, nextToken.get())//
					: getSnsClient().listSubscriptionsByTopic(topicArn);

			nextToken = Optional.ofNullable(listSubscriptions.getNextToken());

			Optional<Subscription> mySubscription = listSubscriptions.getSubscriptions().stream()//
					.filter(subscription -> subscription.getProtocol().equals(protocol)//
							&& subscription.getEndpoint().equals(endpoint))//
					.findAny();

			if (mySubscription.isPresent())
				return mySubscription;

		} while (nextToken.isPresent());

		return Optional.empty();
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
