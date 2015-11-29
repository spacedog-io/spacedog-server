package io.spacedog.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.amazonaws.util.StringInputStream;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

import net.codestory.http.Context;
import net.codestory.http.Cookie;
import net.codestory.http.Cookies;
import net.codestory.http.Part;
import net.codestory.http.Query;
import net.codestory.http.Request;
import net.codestory.http.Response;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;
import net.codestory.http.types.ContentTypes;

@Prefix("/v1/batch")
public class BatchResource extends AbstractResource {

	@Post("")
	@Post("/")
	public Payload execute(String body, Context context) throws JsonParseException, JsonMappingException, IOException {
		Credentials credentials = AdminResource.checkCredentials(context);
		ArrayNode requests = Json.readArrayNode(body);
		JsonBuilder<ArrayNode> responses = Json.arrayBuilder();

		// TODO use this boolean
		boolean created = false;

		BatchResponse response = new BatchResponse(context);

		for (JsonNode request : requests) {
			// String uri = checkString(request, "uri", true, "batch request
			// objects");
			// String method = checkString(request, "method", true, "batch
			// request objects");
			// String[] uriTerms = Utils.splitBySlash(uri);
			// if ("data".equals(uriTerms[0])) {
			// if (Methods.GET.equals(method))
			// responses.addNode(getData(request, uriTerms, context,
			// credentials));
			// }

			// if (!request.isObject())
			// throw new IllegalArgumentException(
			// String.format("batch request not a json object but [%s]",
			// request.getNodeType()));
			//
			// JsonRequest wrapper = new JsonRequest((ObjectNode) request,
			// context);
			// SpaceDogServices.executeInternalRequest(wrapper, wrapper);
			// responses.addNode(wrapper.jsonContent());
		}

		return new Payload(JSON_CONTENT, responses.toString(), created ? HttpStatus.CREATED : HttpStatus.OK);
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

	//
	// JsonRequest and JsonResponse
	//

	public class JsonRequest implements Request {

		private ObjectNode request;
		private Context context;

		public JsonRequest(ObjectNode request, Context context) {
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
			return checkString(request, "uri", true, "batch request objects");
		}

		@Override
		public String method() {
			return checkString(request, "method", true, "batch request objects");
		}

		@Override
		public String content() throws IOException {
			return request.get("body").toString();
		}

		@Override
		public String contentType() {
			return ContentTypes.get(".json");
		}

		@Override
		public List<String> headerNames() {
			return context.request().headerNames();
		}

		@Override
		public List<String> headers(String name) {
			return context.request().headers(name);
		}

		@Override
		public String header(String name) {
			return context.request().header(name);
		}

		@Override
		public InputStream inputStream() throws IOException {
			return new StringInputStream(content());
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
					return JsonRequest.this.context.cookies().iterator();
				}

				@Override
				public <T> T unwrap(Class<T> type) {
					return JsonRequest.this.unwrap(type);
				}

				@Override
				public Cookie get(String name) {
					return JsonRequest.this.context.cookies().get(name);
				}
			};
		}

		@Override
		public Query query() {
			return new Query() {

				@Override
				public <T> T unwrap(Class<T> type) {
					return null;
				}

				@Override
				public Collection<String> keys() {
					return Lists.newArrayList(((ObjectNode) JsonRequest.this.request.get("query")).fieldNames());
				}

				@Override
				public Iterable<String> all(String name) {
					JsonNode jsonNode = ((ObjectNode) JsonRequest.this.request.get("query")).get(name);
					if (jsonNode.isArray())
						return () -> Iterators.transform(jsonNode.elements(), node -> node.asText());
					else
						return Collections.singleton(jsonNode.asText());
				}

			};
		}

		@Override
		public List<Part> parts() {
			// no multipart post request in batch
			return Collections.emptyList();
		}
	}

	public class BatchResponse implements Response {
		private Context context;
		private long batchLength = 0;

		public BatchResponse(Context context) {
			this.context = context;
		}

		@Override
		public <T> T unwrap(Class<T> type) {
			return context.response().unwrap(type);
		}

		@Override
		public void close() throws IOException {
		}

		public void batchClose() throws IOException {
			context.response().close();
		}

		@Override
		public OutputStream outputStream() throws IOException {
			return context.response().outputStream();
		}

		@Override
		public void setContentLength(long length) {
			context.response().setContentLength(this.batchLength + length);
		}

		@Override
		public void setHeader(String name, String value) {
			// TODO Auto-generated method stub

		}

		@Override
		public void setStatus(int statusCode) {
			// TODO Auto-generated method stub

		}

		@Override
		public void setCookie(Cookie cookie) {
			// TODO Auto-generated method stub

		}

	}

}
