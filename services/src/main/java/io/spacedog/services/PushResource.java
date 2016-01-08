package io.spacedog.services;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.index.IndexResponse;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
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

	public static final String TYPE = "device";

	@Post("/device")
	@Post("/device/")
	public Payload postDevice(Context context) throws JsonParseException, JsonMappingException, IOException {

		Credentials credentials = SpaceContext.checkUserCredentials();

		if (!credentials.email().isPresent())
			throw new IllegalArgumentException("credentials without any email address");

		JsonBuilder<ObjectNode> device = Json.objectBuilder()//
				.put("name", credentials.name())//
				.put("email", credentials.email().get());

		IndexResponse response = ElasticHelper.get().createObject(credentials.backendId(), TYPE, device.build(),
				credentials.name());

		return PayloadHelper.saved(true, "/v1/device", response.getType(), response.getId(), response.getVersion());
	}

	@Post("/device/push")
	@Post("/device/push/")
	public Payload pushAll(Context context) throws JsonParseException, JsonMappingException, IOException,
			UnirestException, NotFoundException, InterruptedException, ExecutionException {

		Credentials admin = SpaceContext.checkAdminCredentials();
		ElasticHelper.get().refresh(true, admin.backendId());
		String from = admin.backendId().toUpperCase() + "-PUSH <no-reply@api.spacedog.io>";

		ObjectNode result = SearchResource.get()//
				.searchInternal(admin, TYPE, null, context);

		JsonBuilder<ObjectNode> response = PayloadHelper.minimalBuilder(200).array("mailgun");

		JsonNode results = result.get("results");
		for (int i = 0; i < results.size(); i++) {
			String msg = "Hello " + results.get(i).get("name").asText();
			ObjectNode mail = MailResource.get().send(from, results.get(i).get("email").asText(), null, null, msg, msg,
					null);
			response.node(mail);
		}

		return PayloadHelper.json(response.build(), 200);
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
