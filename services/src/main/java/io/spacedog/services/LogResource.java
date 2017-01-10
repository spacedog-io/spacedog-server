package io.spacedog.services;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.QuerySourceBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.io.Resources;

import io.spacedog.utils.Check;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Credentials.Level;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.SpaceHeaders;
import io.spacedog.utils.SpaceParams;
import io.spacedog.utils.Utils;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.filters.PayloadSupplier;
import net.codestory.http.payload.Payload;

@Prefix("/1/log")
public class LogResource extends Resource {

	public static final String TYPE = "log";

	//
	// init
	//

	void init() throws IOException {

		ElasticClient client = Start.get().getElasticClient();

		String mapping = Resources.toString(Resources.getResource(//
				"io/spacedog/services/log-mapping.json"), Utils.UTF8);

		if (client.existsIndex(SPACEDOG_BACKEND, TYPE))
			client.putMapping(SPACEDOG_BACKEND, TYPE, mapping);
		else
			client.createIndex(SPACEDOG_BACKEND, TYPE, mapping, false);
	}

	//
	// Routes
	//

	@Get("")
	@Get("/")
	public Payload getAll(Context context) {

		Credentials credentials = SpaceContext.checkSuperAdminCredentials();

		Optional<String> optBackendId = credentials.isSuperDog() //
				&& credentials.isTargetingRootApi() ? Optional.empty() //
						: Optional.of(credentials.target());

		int from = context.query().getInteger("from", 0);
		int size = context.query().getInteger("size", 10);
		Check.isTrue(from + size <= 1000, "from + size must be less than or equal to 1000");

		boolean refresh = context.query().getBoolean(SpaceParams.REFRESH, false);
		DataStore.get().refreshType(refresh, SPACEDOG_BACKEND, TYPE);

		SearchResponse response = doGetLogs(from, size, optBackendId);
		return extractLogs(response);
	}

	@Post("/search")
	@Post("/search/")
	public Payload search(String body, Context context) {

		Credentials credentials = SpaceContext.checkAdminCredentials();

		QueryBuilder query = QueryBuilders.wrapperQuery(body);

		if (!credentials.isTargetingRootApi())
			query = QueryBuilders.boolQuery()//
					.filter(query)//
					.filter(QueryBuilders.termQuery("credentials.backendId", //
							credentials.target()));

		boolean refresh = context.query().getBoolean(SpaceParams.REFRESH, false);
		DataStore.get().refreshType(refresh, SPACEDOG_BACKEND, TYPE);

		SearchResponse response = Start.get().getElasticClient()//
				.prepareSearch(SPACEDOG_BACKEND, TYPE)//
				.setTypes(TYPE)//
				.setQuery(query)//
				.setFrom(context.query().getInteger("from", 0)) //
				.setSize(context.query().getInteger("size", 10)) //
				.addSort("receivedAt", SortOrder.DESC)//
				.get();

		return extractLogs(response);
	}

	@Delete("")
	@Delete("/")
	public Payload purge(Context context) {

		Credentials credentials = SpaceContext.getCredentials();
		Optional<String> optBackendId = null;

		if (isPurgeAll(credentials))
			optBackendId = Optional.empty();

		else if (credentials.isAtLeastSuperAdmin())
			optBackendId = Optional.of(credentials.target());

		else
			throw Exceptions.insufficientCredentials(credentials);

		Optional<DeleteByQueryResponse> response = doPurgeBackend(//
				context.request().query().getInteger("from", 100000), optBackendId);

		// no delete response means no logs to delete means success

		return response.isPresent()//
				? JsonPayload.json(response.get()) : JsonPayload.success();
	}

	//
	// Filter
	//

	public static SpaceFilter filter() {

		return new SpaceFilter() {

			private static final long serialVersionUID = 5621427145724229373L;

			@Override
			public boolean matches(String uri, Context context) {

				// https://api.spacedog.io ping requests should not be logged
				if (uri.isEmpty() || uri.equals(SLASH))
					return !SpaceContext.getCredentials().isTargetingRootApi();

				return true;
			}

			@Override
			public Payload apply(String uri, Context context, PayloadSupplier nextFilter) throws Exception {
				Payload payload = null;
				DateTime receivedAt = DateTime.now();

				try {
					payload = nextFilter.get();
				} catch (Throwable t) {
					payload = JsonPayload.error(t);
				}

				if (payload == null)
					payload = JsonPayload.error(500, //
							"unexpected null payload for [%s] request to [%s]", context.method(), uri);

				try {
					get().log(uri, context, receivedAt, payload);
				} catch (Exception e) {
					// TODO: log platform unexpected error with a true logger
					e.printStackTrace();
				}

				return payload;
			}
		};
	}

	//
	// Implementation
	//

	private boolean isPurgeAll(Credentials credentials) {
		return credentials.isTargetingRootApi() //
				&& (credentials.isSuperDog() || credentials.roles().contains("purgeall"));
	}

	private Optional<DeleteByQueryResponse> doPurgeBackend(int from, //
			Optional<String> optBackendId) {

		SearchHit[] hits = doGetLogs(from, 1, optBackendId).getHits().getHits();

		if (hits == null || hits.length == 0)
			// no log to delete
			return Optional.empty();

		String receivedAt = Json.readObject(hits[0].sourceAsString())//
				.get("receivedAt").asText();

		BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery()//
				.filter(QueryBuilders.rangeQuery("receivedAt").lte(receivedAt));

		if (optBackendId.isPresent())
			boolQueryBuilder.filter(//
					QueryBuilders.termQuery("credentials.backendId", optBackendId.get()));

		String query = new QuerySourceBuilder().setQuery(boolQueryBuilder).toString();

		DeleteByQueryResponse delete = Start.get().getElasticClient()//
				.deleteByQuery(query, SPACEDOG_BACKEND, TYPE);

		return Optional.of(delete);
	}

	private SearchResponse doGetLogs(int from, int size, Optional<String> backendId) {

		BoolQueryBuilder query = QueryBuilders.boolQuery();

		if (backendId.isPresent())
			query.filter(QueryBuilders.termQuery("credentials.backendId", backendId.get()));

		return Start.get().getElasticClient()//
				.prepareSearch(SPACEDOG_BACKEND, TYPE)//
				.setTypes(TYPE)//
				.setQuery(query)//
				.addSort("receivedAt", SortOrder.DESC)//
				.setFrom(from)//
				.setSize(size)//
				.get();
	}

	private Payload extractLogs(SearchResponse response) {
		JsonBuilder<ObjectNode> builder = Json.objectBuilder()//
				.put("took", response.getTookInMillis())//
				.put("total", response.getHits().getTotalHits())//
				.array("results");

		for (SearchHit hit : response.getHits().getHits())
			builder.node(hit.sourceAsString());

		return JsonPayload.json(builder);
	}

	private String log(String uri, Context context, DateTime receivedAt, Payload payload) {

		ObjectNode log = Json.object(//
				"method", context.method(), //
				"path", uri, //
				"receivedAt", receivedAt.toString(), //
				"processedIn", DateTime.now().getMillis() - receivedAt.getMillis(), //
				"status", payload == null ? 500 : payload.code());

		addCredentials(log);
		addQuery(log, context);
		addHeaders(log, context.request().headers().entrySet());
		addRequestPayload(log, context);
		addResponsePayload(log, payload, context);

		return Start.get().getElasticClient()//
				.index(SPACEDOG_BACKEND, TYPE, log.toString())//
				.getId();
	}

	private void addResponsePayload(ObjectNode log, Payload payload, Context context) {
		if (payload != null) {
			if (payload.rawContent() instanceof ObjectNode) {
				ObjectNode node = (ObjectNode) payload.rawContent();
				ObjectNode response = log.putObject("response");
				// log the whole json payload but 'results'
				Iterator<Entry<String, JsonNode>> fields = node.fields();
				while (fields.hasNext()) {
					Entry<String, JsonNode> field = fields.next();
					if (!field.getKey().equals("results"))
						response.set(field.getKey(), field.getValue());
				}
			}
		}
	}

	private void addRequestPayload(ObjectNode log, Context context) {

		try {
			String content = context.request().content();

			if (!Strings.isNullOrEmpty(content)) {

				// TODO: fix the content type problem
				// String contentType = context.request().contentType();
				// log.put("contentType", contentType);
				// if (PayloadHelper.JSON_CONTENT.equals(contentType))
				// log.node("content", content);
				// else
				// log.put("content", content);

				if (Json.isObject(content)) {
					JsonNode securedContent = Json.fullReplaceTextualFields(//
							Json.readNode(content), "password", "******");

					log.set("jsonContent", securedContent);
				}
			}
		} catch (Exception e) {
			log.set("jsonContent", Json.object("error", JsonPayload.toJson(e, true)));
		}
	}

	private void addQuery(ObjectNode log, Context context) {
		if (context.query().keys().isEmpty())
			return;

		ObjectNode logQuery = log.putObject("query");
		for (String key : context.query().keys()) {
			String value = key.equals(PASSWORD) //
					? "******" : context.get(key);
			logQuery.put(key, value);
		}
	}

	private void addCredentials(ObjectNode log) {
		Credentials credentials = SpaceContext.getCredentials();
		ObjectNode logCredentials = log.putObject("credentials");
		logCredentials.put("backendId", credentials.target());
		logCredentials.put("type", credentials.level().toString());
		if (!credentials.level().equals(Level.KEY))
			logCredentials.put("name", credentials.name());
	}

	private void addHeaders(ObjectNode log, Set<Entry<String, List<String>>> headers) {

		for (Entry<String, List<String>> header : headers) {

			String key = header.getKey();
			List<String> values = header.getValue();

			if (key.equalsIgnoreCase(SpaceHeaders.AUTHORIZATION))
				continue;

			if (Utils.isNullOrEmpty(values))
				continue;

			if (key.equalsIgnoreCase(SpaceHeaders.USER_AGENT)) {
				log.with("headers").put(key, values.toString()//
						.substring(1, values.toString().length() - 1));
				return;
			}

			if (values.size() == 1)
				log.with("headers").put(key, values.get(0));
			else if (values.size() > 1) {
				ArrayNode array = log.with("headers").putArray(key);
				for (String string : values)
					array.add(string);
			}
		}
	}

	//
	// Singleton
	//

	private static LogResource singleton = new LogResource();

	static LogResource get() {
		return singleton;
	}

	private LogResource() {
	}
}
