package io.spacedog.server;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Sets;
import com.google.common.io.BaseEncoding;

import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.http.SpaceHeaders;
import io.spacedog.client.schema.Schema;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.KeyValue;
import io.spacedog.utils.Utils;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.filters.PayloadSupplier;
import net.codestory.http.payload.Payload;

/**
 * TODO
 * 
 * payload field should be enabled(false) - better handle filter errors
 * 
 * @author davattias
 *
 */
@Prefix("/1/callbacks")
public class CallbackService extends SpaceService {

	public static final String TYPE = "callback";

	//
	// Schema
	//

	public Schema requestSchema() {
		return Schema.builder(TYPE)//
				.keyword("status")//
				.keyword("method")//
				.keyword("path")//
				.stash("payload")//
				.stash("response")//

				.object("credentials")//
				.keyword("id")//
				.keyword("name")//
				.keyword("roles")//
				.closeObject()//

				.object("headers")//
				.keyword("name")//
				.keyword("values")//
				.closeObject()//

				.object("parameters")//
				.keyword("name")//
				.keyword("values")//
				.closeObject()//

				.build();
	}

	//
	// Routes
	//

	@Put("")
	@Put("/")
	public Payload putSafe() throws IOException {

		SpaceContext.credentials().checkAtLeastSuperAdmin();

		Index index = callbackIndex();
		ElasticClient elastic = elastic();
		String mapping = requestSchema().mapping().toString();

		if (!elastic.exists(index))
			elastic.createIndex(index, mapping, false);

		return JsonPayload.ok().build();
	}

	@Get("")
	@Get("/")
	public Payload getSafe(Context context) {
		return DataService.get().getAll(context);
	}

	@Post("/search")
	@Post("/search/")
	public Payload search(String body, Context context) {
		return SearchService.get().postSearchForType(TYPE, body, context);
	}

	@Get("/:id")
	@Get("/:id/")
	public Payload getDemand(String id, Context context) {
		return DataService.get().getById(TYPE, id, context);
	}

	@Put("/:id")
	@Put("/:id/")
	public Payload putDemand(String id, String body, Context context) {
		return DataService.get().put(TYPE, id, body, context);
	}

	@Delete("/:id")
	@Delete("/:id/")
	public Payload deleteDemand(String id, Context context) {
		return DataService.get().deleteById(TYPE, id, context);
	}

	//
	// Filter
	//

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Request {
		public String status;
		public String method;
		public String path;
		public Credentials credentials;
		public Set<KeyValue> headers;
		public Set<KeyValue> parameters;
		public Content payload;

		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class Credentials {
			public String id;
			public String name;
			public Set<String> roles;
		}

		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class Content {
			public String type;
			public String content;
		}
	}

	public SpaceFilter filter() {

		return new SpaceFilter() {

			private static final long serialVersionUID = 5621427145724229373L;

			@Override
			public boolean matches(String uri, Context context) {
				return uri.startsWith("/1/safe/stash");
			}

			@Override
			public Payload apply(String uri, Context context, PayloadSupplier nextFilter) throws Exception {
				DateTime receivedAt = DateTime.now();

				try {
					stash(uri, context, receivedAt);
				} catch (Exception e) {
					e.printStackTrace();
				}

				return Payload.ok();
			}
		};
	}

	//
	// Implementation
	//

	private String stash(String uri, Context context, DateTime receivedAt) {

		Credentials credentials = SpaceContext.credentials();

		Request demand = new Request();
		demand.method = context.method();
		demand.path = uri;
		demand.status = "created";

		demand.credentials = new Request.Credentials();
		demand.credentials.id = credentials.id();
		demand.credentials.name = credentials.username();
		demand.credentials.roles = credentials.roles();

		demand.parameters = extractParameters(context);
		demand.headers = extractHeaders(context);
		demand.payload = extractPayload(context);

		return elastic().index(callbackIndex(), demand)//
				.getId();
	}

	private Request.Content extractPayload(Context context) {
		try {
			Request.Content content = new Request.Content();
			content.type = context.request().contentType();
			content.content = isTextual(content.type) //
					? context.request().content()//
					: BaseEncoding.base64().encode(//
							context.request().contentAsBytes());

			return content;

		} catch (IOException e) {
			throw Exceptions.illegalArgument(e, "error extracting request [%s][%s] payload");
		}
	}

	private boolean isTextual(String contentType) {
		return contentType.startsWith("application/json");
	}

	private Set<KeyValue> extractParameters(Context context) {
		Set<KeyValue> parameters = null;
		Collection<String> keys = context.query().keys();

		if (!keys.isEmpty())
			parameters = Sets.newHashSet();

		for (String key : keys)
			if (!key.equals(ACCESS_TOKEN_PARAM))
				parameters.add(new KeyValue(key, context.get(key)));

		return parameters;
	}

	private Set<KeyValue> extractHeaders(Context context) {
		Set<KeyValue> headers = null;
		Set<Entry<String, List<String>>> entries = context.request().headers().entrySet();

		if (!entries.isEmpty())
			headers = Sets.newHashSet();

		for (Entry<String, List<String>> entry : entries) {
			String key = entry.getKey();
			List<String> values = entry.getValue();

			if (key.equalsIgnoreCase(SpaceHeaders.AUTHORIZATION))
				continue;

			if (Utils.isNullOrEmpty(values))
				continue;

			headers.add(new KeyValue(key, values));
		}

		return headers;
	}

	public static Index callbackIndex() {
		return Index.toIndex(TYPE);
	}

	//
	// Singleton
	//

	private static CallbackService singleton = new CallbackService();

	static CallbackService get() {
		return singleton;
	}

	private CallbackService() {
	}
}
