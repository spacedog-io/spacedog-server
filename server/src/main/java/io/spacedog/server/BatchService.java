package io.spacedog.server;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.batch.SpaceCall;
import io.spacedog.client.http.SpaceException;
import io.spacedog.client.http.SpaceMethod;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import net.codestory.http.Context;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

@Prefix("/1/batch")
public class BatchService extends SpaceService {

	// query parameter names

	private static final String STOP_ON_ERROR_QUERY_PARAM = "stopOnError";

	//
	// Routes
	//

	@Post("")
	@Post("/")
	public Payload post(List<SpaceCall> batch, Context context) {

		if (batch.size() > 10)
			throw new SpaceException("batch-limit-exceeded", HttpStatus.BAD_REQUEST, //
					"batch are limited to 10 sub requests");

		boolean stopOnError = context.query().getBoolean(STOP_ON_ERROR_QUERY_PARAM, false);
		ArrayNode responses = Json.array();

		for (int i = 0; i < batch.size(); i++) {

			Payload requestPayload = null;
			InternalRequest request = new InternalRequest(batch.get(i), context);

			try {
				checkRequestAuthorizedInBatch(request);
				requestPayload = Server.get().executeRequest(request, null);
			} catch (Throwable t) {
				requestPayload = JsonPayload.error(t).build();
			}

			if (requestPayload == null)
				requestPayload = new Payload(HttpStatus.INTERNAL_SERVER_ERROR);

			responses.add(toJson(requestPayload, request));

			if (stopOnError && requestPayload.isError())
				break;
		}
		return JsonPayload.ok()//
				.withFields("responses", responses)//
				.build();
	}

	private void checkRequestAuthorizedInBatch(InternalRequest request) {
		// backend service is forbidden in batch request to avoid create/delete backend
		// in batches. Without this restriction, it is possible in certain conditions to
		// create multiple credentials with the same username.
		if (request.uri().startsWith("/1/backend"))
			throw Exceptions.illegalArgument(//
					"/1/backend requests forbidden in batch");
	}

	@Get("")
	@Get("/")
	public Payload get(Context context) {

		ObjectNode response = Json.object();
		boolean stopOnError = context.query().getBoolean(STOP_ON_ERROR_QUERY_PARAM, false);

		for (String key : context.query().keys()) {

			if (!key.equals(STOP_ON_ERROR_QUERY_PARAM)) {
				Payload payload = null;
				InternalRequest request = new InternalRequest(//
						new SpaceCall(SpaceMethod.GET, "/1" + context.get(key)), //
						context);

				try {
					payload = Server.get().executeRequest(request, null);
				} catch (Throwable t) {
					payload = JsonPayload.error(t).build();
				}

				if (payload == null)
					payload = new Payload(HttpStatus.INTERNAL_SERVER_ERROR);

				response.set(key, toJson(payload, request));

				if (payload.isError() && stopOnError)
					break;
			}
		}

		return JsonPayload.ok().withContent(response).build();
	}

	private static JsonNode toJson(Payload payload, InternalRequest request) {

		Object rawContent = payload.rawContent();

		if (rawContent == null)
			rawContent = JsonPayload.status(payload.code())//
					.build().rawContent();

		if (rawContent instanceof JsonNode)
			return (JsonNode) rawContent;

		throw Exceptions.illegalArgument(//
				"batch sub requests [%s] returned a non JSON response", //
				request);
	}

	//
	// singleton
	//

	private static BatchService singleton = new BatchService();

	public static BatchService get() {
		return singleton;
	}

	private BatchService() {
	}
}
