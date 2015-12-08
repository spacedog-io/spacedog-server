package io.spacedog.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import net.codestory.http.Context;
import net.codestory.http.Cookie;
import net.codestory.http.Cookies;
import net.codestory.http.Part;
import net.codestory.http.Query;
import net.codestory.http.Request;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;
import net.codestory.http.payload.StreamingOutput;

@Prefix("/v1/batch")
public class BatchResource extends AbstractResource {

	// query parameter names

	private static final String STOP_ON_ERROR_QUERY_PARAM = "stopOnError";

	@Post("")
	@Post("/")
	public Payload execute(String body, Context context) throws Exception {

		Debug.resetBatchDebug();

		// do not put any checkCredentials here since it must be called in the
		// following lambda function

		ArrayNode requests = Json.readArrayNode(body);

		if (requests.size() > 10)
			return PayloadHelper.error(HttpStatus.BAD_REQUEST, "batch are limited to 10 sub requests");

		boolean stopOnError = context.query().getBoolean(STOP_ON_ERROR_QUERY_PARAM, false);

		StreamingOutput streamingOutput = output -> {

			// Batch needs special care on SpaceContext and Credentials
			// since this code is executed by the fluent payload writer
			// outside of the filter chain.
			// I artificially reproduce the SpaceContext filter mechanism and
			// force init of the SpaceContext for the whole batch an avoid one
			// buildCredentials per sub request
			// Use extra care before to change any of this.
			try {
				SpaceContext.init(context);

				output.write("{\"success\":true,\"status\":200,\"responses\":[".getBytes(Utils.UTF8));

				for (int i = 0; i < requests.size(); i++) {

					if (i > 0)
						output.write(",".getBytes(Utils.UTF8));

					Payload payload = null;
					BatchJsonRequestWrapper requestWrapper = new BatchJsonRequestWrapper(//
							Json.checkObject(requests.get(i)), context);

					try {
						payload = Start.get().executeRequest(requestWrapper, null);
					} catch (Throwable t) {
						payload = PayloadHelper.error(t);
					}

					if (payload == null)
						payload = new Payload(HttpStatus.INTERNAL_SERVER_ERROR);

					if (PayloadHelper.isJson(payload)) {

						if (payload.isSuccess() && "GET".equalsIgnoreCase(requestWrapper.method())) {

							output.write(String.format("{\"success\":true,\"status\":%s,\"content\":", payload.code())
									.getBytes(Utils.UTF8));
							output.write(PayloadHelper.toBytes(payload.rawContent()));
							output.write("}".getBytes(Utils.UTF8));

						} else
							output.write(PayloadHelper.toBytes(payload.rawContent()));

					} else
						output.write(PayloadHelper.toBytes(PayloadHelper.json(payload.code())));

					if (stopOnError && payload.isError())
						break;
				}

				if (SpaceContext.get().debug()) {
					output.write(String.format("],\"debug\":%s}", Debug.buildDebugObjectNode().toString())//
							.getBytes(Utils.UTF8));
				} else
					output.write("]}".getBytes(Utils.UTF8));

			} finally {
				SpaceContext.reset();
			}

		};

		return new Payload(PayloadHelper.JSON_CONTENT_UTF8, streamingOutput, HttpStatus.OK);
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
			return type.isInstance(request) ? (T) request : type.isInstance(context) ? (T) context : null;
		}

		@Override
		public String uri() {
			return checkStringNotNullOrEmpty(request, "path");
		}

		@Override
		public String method() {
			return checkStringNotNullOrEmpty(request, "method");
		}

		@Override
		public String content() throws IOException {
			return checkJsonNode(request, "content", true).get().toString();
		}

		@Override
		public String contentType() {
			return context.request().contentType();
		}

		@Override
		public List<String> headerNames() {
			Set<String> headerNames = Sets.newHashSet();
			headerNames.addAll(context.request().headerNames());
			checkObjectNode(request, "headers", false)//
					.ifPresent(node -> Iterators.addAll(headerNames, node.fieldNames()));
			return Lists.newArrayList(headerNames.iterator());
		}

		@Override
		public List<String> headers(String name) {
			Optional<JsonNode> headers = checkObjectNode(request, "headers." + name, false);
			if (headers.isPresent())
				return Json.toList(headers.get());
			return context.request().headers(name);
		}

		@Override
		public String header(String name) {
			Optional<JsonNode> header = checkObjectNode(request, "headers." + name, false);
			if (header.isPresent())
				return header.get().asText();
			return context.request().header(name);
		}

		@Override
		public InputStream inputStream() throws IOException {
			throw new UnsupportedOperationException("batch wrapped request must not provide any input stream");
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
					Optional<JsonNode> opt = AbstractResource.checkObjectNode(request, "parameters", false);
					return opt.isPresent() ? Lists.newArrayList(opt.get().fieldNames()) : Collections.emptyList();
				}

				@Override
				public Iterable<String> all(String name) {
					JsonNode paramNode = Json.get(request, "parameters." + name);

					if (Json.isNull(paramNode))
						return Collections.emptyList();

					return Json.toList(paramNode);
				}
			};
		}

		@Override
		public List<Part> parts() {
			throw new UnsupportedOperationException("batch wrapped request must not provide parts");
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
