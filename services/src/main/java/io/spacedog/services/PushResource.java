package io.spacedog.services;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.joda.time.DateTime;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicResult;
import com.amazonaws.services.sns.model.ListTopicsResult;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.amazonaws.services.sns.model.SubscribeResult;
import com.amazonaws.services.sns.model.Subscription;
import com.amazonaws.services.sns.model.Topic;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mashape.unirest.http.exceptions.UnirestException;

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
	public Payload postDevice(String body, Context context)
			throws JsonParseException, JsonMappingException, IOException {

		Credentials credentials = SpaceContext.checkUserCredentials();

		ObjectNode node = Json.readObjectNode(body);
		String protocol = Json.checkStringNotNullOrEmpty(node, "protocol");
		String endpoint = Json.checkStringNotNullOrEmpty(node, "endpoint");

		String topicArn = createTopic("all", credentials.backendId());
		Optional<Subscription> subscription = getSubscribtion(topicArn, protocol, endpoint);

		if (!subscription.isPresent()) {

			SubscribeResult subscribe = getSnsClient().subscribe(topicArn, //
					protocol, endpoint);

			return PayloadHelper.saved(true, "/v1", "device", subscribe.getSubscriptionArn());
		}

		return PayloadHelper.saved(false, "/v1", "device", subscription.get().getSubscriptionArn());
	}

	@Post("/topic/:name/push")
	@Post("/topic/:name/push/")
	public Payload pushToTopic(String body, String name, Context context)
			throws JsonParseException, JsonMappingException, IOException, UnirestException, NotFoundException,
			InterruptedException, ExecutionException {

		Credentials credentials = SpaceContext.checkAdminCredentials();

		Check.notNullOrEmpty(name, "topic name");
		ObjectNode node = Json.readObjectNode(body);
		String message = Json.checkStringNotNullOrEmpty(node, "message");

		Optional<Topic> topic = getTopicArn(name, credentials.backendId());

		if (!topic.isPresent())
			return PayloadHelper.error(404, "topic with name [%s] not found", name);

		PublishResult publish = getSnsClient().publish(new PublishRequest()//
				.withTopicArn(topic.get().getTopicArn())//
				.withSubject(message)//
				.withMessage(message));

		return PayloadHelper.json(PayloadHelper.minimalBuilder(200)//
				.put("messageId", publish.getMessageId()).build(), 200);
	}

	@Post("/device/push")
	@Post("/device/push/")
	public Payload pushAll(Context context) throws JsonParseException, JsonMappingException, IOException,
			UnirestException, NotFoundException, InterruptedException, ExecutionException {

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

		return PayloadHelper.json(PayloadHelper.minimalBuilder(200)//
				.put("messageId", publish.getMessageId()).build(), 200);
	}

	//
	// implementation
	//

	AmazonSNSClient getSnsClient() {
		if (snsClient == null) {
			snsClient = new AmazonSNSClient();
			snsClient.setRegion(Region.getRegion(Regions.EU_WEST_1));
		}
		return snsClient;
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

	Optional<Subscription> getSubscribtion(String topicArn, String protocol, String endpoint) {

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
