package io.spacedog.services;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.codestory.http.Context;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.constants.Methods;
import net.codestory.http.payload.Payload;

@Prefix("/v1/batch")
public class BatchResource extends AbstractResource {

	@Post("")
	@Post("/")
	public Payload execute(String body, Context context) {
		try {
			Credentials credentials = AdminResource.checkCredentials(context);
			ArrayNode requests = Json.readArrayNode(body);
			JsonBuilder<ArrayNode> responses = Json.startArray();

			// TODO use this boolean
			boolean created = false;

			for (JsonNode request : requests) {
				String uri = checkString(request, "uri", true, "batch request objects");
				String method = checkString(request, "method", true, "batch request objects");
				String[] uriTerms = Utils.splitBySlash(uri);

				if ("data".equals(uriTerms[0])) {
					if (Methods.GET.equals(method))
						responses.addNode(getData(request, uriTerms, context, credentials));
				}
			}

			return new Payload(JSON_CONTENT, responses.toString(), created ? HttpStatus.CREATED : HttpStatus.OK);

		} catch (Throwable throwable) {
			return error(throwable);
		}

	}

	private ObjectNode getData(JsonNode request, String[] uriTerms, Context context, Credentials credentials)
			throws NotFoundException, JsonProcessingException, InterruptedException, ExecutionException, IOException {

		if (uriTerms.length == 1)
			return DataResource.get().internalSearch(credentials, null, null, context);
		if (uriTerms.length == 2)
			return DataResource.get().internalSearch(credentials, uriTerms[1], null, context);
		if (uriTerms.length == 3)
			return DataResource.get().internalGet(uriTerms[1], uriTerms[2], credentials);

		throw new IllegalArgumentException(String.format("invalid GET request data uri [%s]", Utils.toUri(uriTerms)));
	}

	//
	// singleton
	//

	private static BatchResource singleton = new BatchResource();

	static BatchResource get() {
		return singleton;
	}

	private BatchResource() {
	}

}
