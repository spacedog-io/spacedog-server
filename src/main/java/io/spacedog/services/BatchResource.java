package io.spacedog.services;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

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
import net.codestory.http.constants.Headers;
import net.codestory.http.payload.Payload;

@Prefix("/v1/batch")
public class BatchResource extends AbstractResource {

	@Post("")
	@Post("/")
	public Payload execute(String body, Context context) throws Exception {

		// TODO attach credentials to thread for sub request to bypass
		// credentials gathering and don't forget to clean the thread before
		// release
		AdminResource.checkCredentials(context);

		ArrayNode requests = Json.readArrayNode(body);
		BatchResponse responseWrapper = new BatchResponse(requests.size(), context);
		BatchStreaming streaming = new BatchStreaming();
		streaming.write("[");
		boolean comma = false;

		for (JsonNode subRequest : requests) {

			if (comma)
				streaming.write(",");
			else
				comma = true;

			BatchSubRequest subRequestWrapper = new BatchSubRequest(checkObjectNode(subRequest), context);
			Payload subRequestPayload = Start.executeInternalRequest(subRequestWrapper, responseWrapper);
			streaming.write((byte[]) subRequestPayload.rawContent());
		}

		streaming.write("]");
		return PayloadHelper.json(streaming.getBytes());
	}

	public class BatchStreaming {

		private ByteArrayOutputStream bytes;
		private BufferedOutputStream buffer;
		private PrintWriter writer;

		public BatchStreaming() {
			bytes = new ByteArrayOutputStream();
			buffer = new BufferedOutputStream(bytes);
			writer = new PrintWriter(buffer);
		}

		public byte[] getBytes() throws IOException {
			buffer.close();
			return bytes.toByteArray();
		}

		public BatchStreaming write(String string) {
			writer.write(string);
			writer.flush();
			return this;
		}

		public BatchStreaming write(byte[] bytes) throws IOException {
			buffer.write(bytes);
			return this;
		}

	}
	//
	// JsonRequest and JsonResponse
	//

	public class BatchSubRequest implements Request {

		private ObjectNode request;
		private Context context;

		public BatchSubRequest(ObjectNode request, Context context) {
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
			return checkStringNotNullOrEmpty(request, "uri");
		}

		@Override
		public String method() {
			return checkStringNotNullOrEmpty(request, "method");
		}

		@Override
		public String content() throws IOException {
			return request.get("body").toString();
		}

		@Override
		public String contentType() {
			return context.request().contentType();
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
			return Headers.ACCEPT_ENCODING.equals(name) ? "" : context.request().header(name);
		}

		@Override
		public InputStream inputStream() throws IOException {
			return new ByteArrayInputStream(content().getBytes(Utils.UTF8));
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
					return BatchSubRequest.this.unwrap(type);
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
					return BatchSubRequest.this.unwrap(type);
				}

				@Override
				public Collection<String> keys() {
					Optional<JsonNode> opt = AbstractResource.checkObjectNode(request, "query", false);
					return opt.isPresent() ? Lists.newArrayList(opt.get().fieldNames()) : Collections.emptyList();
				}

				@Override
				public Iterable<String> all(String name) {
					Optional<JsonNode> opt = AbstractResource.checkObjectNode(request, "query", false);
					if (opt.isPresent()) {
						JsonNode paramNode = opt.get().get(name);
						return paramNode.isArray()//
								? () -> Iterators.transform(paramNode.elements(), node -> node.asText())//
								: Collections.singleton(paramNode.asText());
					} else
						return null;
				}
			};
		}

		@Override
		public List<Part> parts() {
			// no multi part post request in batch
			return Collections.emptyList();
		}
	}

	public class BatchResponse implements Response {
		private int closeCounter;
		private Context context;
		private PrintWriter writer;
		private ByteArrayOutputStream buffer;

		public BatchResponse(int size, Context context) throws IOException {
			this.closeCounter = size;
			this.context = context;
			this.buffer = new ByteArrayOutputStream();
			this.writer = new PrintWriter(buffer);
			this.writer.write('[');
		}

		public byte[] rawContent() {
			return buffer.toByteArray();
		}

		@Override
		public <T> T unwrap(Class<T> type) {
			return context.response().unwrap(type);
		}

		@Override
		public void close() throws IOException {
			closeCounter = closeCounter - 1;
			if (closeCounter == 0) {
				writer.write(']');
				// context.response().outputStream().write(buffer.toByteArray());
				// context.response().close();
			} else {
				writer.write(',');
				writer.flush();
				System.out.println(buffer.toString());
			}
		}

		@Override
		public OutputStream outputStream() throws IOException {
			return this.buffer;
		}

		@Override
		public void setContentLength(long length) {
			// do not set the content length since it should be set before
			// writing
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
