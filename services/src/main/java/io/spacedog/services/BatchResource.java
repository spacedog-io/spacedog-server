package io.spacedog.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.spacedog.core.Json8;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.Optional7;
import io.spacedog.utils.SpaceException;
import net.codestory.http.Context;
import net.codestory.http.Cookie;
import net.codestory.http.Cookies;
import net.codestory.http.Part;
import net.codestory.http.Query;
import net.codestory.http.Request;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

@Prefix("/1/batch")
public class BatchResource extends Resource {

	// query parameter names

	private static final String STOP_ON_ERROR_QUERY_PARAM = "stopOnError";

	//
	// Routes
	//

	@Post("")
	@Post("/")
	public Payload post(String body, Context context) {

		ArrayNode requests = Json.readArray(body);

		if (requests.size() > 10)
			throw new SpaceException("batch-limit-exceeded", HttpStatus.BAD_REQUEST, //
					"batch are limited to 10 sub requests");

		boolean stopOnError = context.query().getBoolean(STOP_ON_ERROR_QUERY_PARAM, false);
		ArrayNode responses = Json.array();

		for (int i = 0; i < requests.size(); i++) {

			Payload requestPayload = null;
			BatchJsonRequestWrapper request = new BatchJsonRequestWrapper(//
					Json.checkObject(requests.get(i)), context);

			try {
				checkRequestAuthorizedInBatch(request);
				requestPayload = Start.get().executeRequest(request, null);
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
				.with("responses", responses)//
				.build();
	}

	private void checkRequestAuthorizedInBatch(BatchJsonRequestWrapper request) {
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
				BatchJsonRequestWrapper request = new BatchJsonRequestWrapper(//
						Json.object("method", "GET", "path", "/1" + context.get(key)), //
						context);

				try {
					payload = Start.get().executeRequest(request, null);
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

		return JsonPayload.ok().with(response).build();
	}

	private static JsonNode toJson(Payload payload, BatchJsonRequestWrapper request) {

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
	// BatchRequestWrapper
	//

	public class BatchJsonRequestWrapper implements Request {

		private ObjectNode request;
		private Context context;

		public BatchJsonRequestWrapper(ObjectNode request, Context context) {
			this.request = request;
			this.context = context;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> T unwrap(Class<T> type) {
			return type.isInstance(request) //
					? (T) request
					: type.isInstance(context) ? (T) context : null;
		}

		@Override
		public String uri() {
			return Json.checkStringNotNullOrEmpty(request, "path");
		}

		@Override
		public String method() {
			return Json.checkStringNotNullOrEmpty(request, "method");
		}

		@Override
		public String toString() {
			return method() + uri();
		}

		@Override
		public String content() throws IOException {
			JsonNode content = request.get("content");
			return content == null ? null : content.toString();
		}

		@Override
		public String contentType() {
			return context.request().contentType();
		}

		@Override
		public List<String> headerNames() {
			Set<String> headerNames = Sets.newHashSet();
			headerNames.addAll(context.request().headerNames());
			Optional7<JsonNode> nodeOpt = Json.checkObject(request, "headers", false);
			if (nodeOpt.isPresent())
				Iterators.addAll(headerNames, nodeOpt.get().fieldNames());
			return Lists.newArrayList(headerNames.iterator());
		}

		@Override
		public List<String> headers(String name) {
			Optional7<JsonNode> headers = Json.checkObject(request, "headers." + name, false);
			if (headers.isPresent())
				return Json8.toStrings(headers.get());
			return context.request().headers(name);
		}

		@Override
		public String header(String name) {
			Optional7<JsonNode> header = Json.checkObject(request, "headers." + name, false);
			if (header.isPresent())
				return header.get().asText();
			return context.request().header(name);
		}

		@Override
		public InputStream inputStream() throws IOException {
			throw new UnsupportedOperationException(//
					"batch wrapped request must not provide any input stream");
		}

		@Override
		public InetSocketAddress clientAddress() {
			return context.request().clientAddress();
		}

		@Override
		public boolean isSecure() {
			return context.request().isSecure();
		}

		@Override
		public Cookies cookies() {
			return new Cookies() {

				@Override
				public Iterator<Cookie> iterator() {
					return context.cookies().iterator();
				}

				@Override
				public <T> T unwrap(Class<T> type) {
					return BatchJsonRequestWrapper.this.unwrap(type);
				}

				@Override
				public Cookie get(String name) {
					return context.cookies().get(name);
				}
			};
		}

		@Override
		public Query query() {
			return new Query() {

				@Override
				public <T> T unwrap(Class<T> type) {
					return BatchJsonRequestWrapper.this.unwrap(type);
				}

				@Override
				public Collection<String> keys() {
					Optional7<JsonNode> opt = Json.checkObject(request, "parameters", false);
					return opt.isPresent() ? Lists.newArrayList(opt.get().fieldNames()) : Collections.emptyList();
				}

				@Override
				public Iterable<String> all(String name) {
					JsonNode paramNode = Json.get(request, "parameters." + name);

					if (Json.isNull(paramNode))
						return Collections.emptyList();

					return Json8.toStrings(paramNode);
				}
			};
		}

		@Override
		public List<Part> parts() {
			throw new UnsupportedOperationException(//
					"batch wrapped request must not provide parts");
		}
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
