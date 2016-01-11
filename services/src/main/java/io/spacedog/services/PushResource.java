package io.spacedog.services;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.index.IndexResponse;
import org.joda.time.DateTime;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mashape.unirest.http.exceptions.UnirestException;

import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import net.codestory.http.Context;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.payload.Payload;

@Prefix("/v1")
public class PushResource {

	private static final String ALL_TOPIC_ARN = "arn:aws:sns:eu-west-1:309725721660:test-all";
	public static final String TYPE = "device";
	private AmazonSNSClient snsClient;

	@Post("/device")
	@Post("/device/")
	public Payload postDevice(Context context) throws JsonParseException, JsonMappingException, IOException {

		Credentials credentials = SpaceContext.checkUserCredentials();

		if (!credentials.email().isPresent())
			throw new IllegalArgumentException("credentials without any email address");

		if (!getSnsClient().listSubscriptionsByTopic(ALL_TOPIC_ARN)//
				.getSubscriptions().stream().anyMatch(//
						subscription -> subscription.getEndpoint().equals(credentials.email().get()))) {

			JsonBuilder<ObjectNode> device = Json.objectBuilder()//
					.put("name", credentials.name())//
					.put("email", credentials.email().get());

			IndexResponse response = ElasticHelper.get().createObject(credentials.backendId(), TYPE, device.build(),
					credentials.name());

			getSnsClient().subscribe(ALL_TOPIC_ARN, //
					"email", credentials.email().get());

			return PayloadHelper.saved(true, "/v1/device", response.getType(), response.getId(), response.getVersion());
		}

		return PayloadHelper.success();
	}

	@Post("/device/push")
	@Post("/device/push/")
	public Payload pushAll(Context context) throws JsonParseException, JsonMappingException, IOException,
			UnirestException, NotFoundException, InterruptedException, ExecutionException {

		SpaceContext.checkAdminCredentials();

		String msg = "PUSH " + DateTime.now();
		AmazonSNSClient snsClient = getSnsClient();
		PublishResult result = snsClient.publish(new PublishRequest()//
				.withTopicArn(ALL_TOPIC_ARN)//
				.withSubject(msg)//
				.withMessage(msg));

		return PayloadHelper.json(PayloadHelper.minimalBuilder(200)//
				.put("messageId", result.getMessageId()).build(), 200);
	}

	AmazonSNSClient getSnsClient() {
		if (snsClient == null) {
			snsClient = new AmazonSNSClient();
			snsClient.setRegion(Region.getRegion(Regions.EU_WEST_1));
		}
		return snsClient;
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
