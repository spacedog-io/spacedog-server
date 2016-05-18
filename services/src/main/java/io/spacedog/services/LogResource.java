package io.spacedog.services;

import java.io.IOException;
import java.util.Optional;

import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.QuerySourceBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;

import io.spacedog.services.Credentials.Level;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.SpaceParams;
import io.spacedog.utils.Utils;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.payload.Payload;

@Prefix("/1")
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

	@Get("/log")
	@Get("/log/")
	public Payload getAll(Context context) {

		Credentials credentials = SpaceContext.checkAdminCredentials(false);

		Optional<String> backendId = credentials.isRootBackend() //
				? Optional.empty() : Optional.of(credentials.backendId());

		String logType = context.query().get(SpaceParams.LOG_TYPE);
		Optional<Level> type = Strings.isNullOrEmpty(logType) ? Optional.empty()//
				: Optional.of(Level.valueOf(logType));

		String minStatus = context.query().get(SpaceParams.MIN_STATUS);
		Optional<Integer> optMinStatus = Strings.isNullOrEmpty(minStatus) ? Optional.empty()//
				: Optional.of(Integer.parseInt(minStatus));

		SearchResponse response = doGetLogs(backendId, //
				context.query().getInteger("from", 0), //
				context.query().getInteger("size", 10), //
				optMinStatus, //
				type);

		return extractLogs(response);
	}

	@Delete("/log")
	@Delete("/log/")
	public Payload purgeBackend(Context context) {

		Credentials credentials = SpaceContext.checkSuperDogCredentials(true);

		Optional<DeleteByQueryResponse> response = doPurgeBackend(credentials.backendId(), //
				context.request().query().getInteger("from", 1000));

		// no delete response means no logs to delete means success

		return response.isPresent()//
				? JsonPayload.json(response.get()) : JsonPayload.success();
	}

	//
	// Filter
	//

	public static SpaceFilter filter() {

		return (uri, context, nextFilter) -> {
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
		};
	}

	//
	// Implementation
	//

	private Optional<DeleteByQueryResponse> doPurgeBackend(String backendId, int from) {

		SearchHit[] hits = doGetLogs(Optional.of(backendId), from, 1, Optional.empty(), Optional.empty())//
				.getHits().getHits();

		if (hits == null || hits.length == 0)
			// no log to delete
			return Optional.empty();

		String receivedAt = Json.readObjectNode(hits[0].sourceAsString())//
				.get("receivedAt").asText();

		String query = new QuerySourceBuilder().setQuery(//
				QueryBuilders.boolQuery()//
						.filter(QueryBuilders.termQuery("credentials.backendId", backendId))//
						.filter(QueryBuilders.rangeQuery("receivedAt").lte(receivedAt)))
				.toString();

		DeleteByQueryResponse delete = Start.get().getElasticClient()//
				.deleteByQuery(SPACEDOG_BACKEND, TYPE, query);

		return Optional.of(delete);
	}

	private SearchResponse doGetLogs(Optional<String> backendId, int from, int size, Optional<Integer> minStatus,
			Optional<Credentials.Level> type) {

		BoolQueryBuilder query = QueryBuilders.boolQuery();

		if (backendId.isPresent())
			query.filter(QueryBuilders.termQuery("credentials.backendId", backendId.get()));

		if (minStatus.isPresent())
			query.filter(QueryBuilders.rangeQuery("status").gte(minStatus.get()));

		if (type.isPresent())
			query.filter(QueryBuilders.termsQuery("credentials.type", //
					Lists.newArrayList(type.get().lowerOrEqual())));

		DataStore.get().refreshType(true, SPACEDOG_BACKEND, TYPE);

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

		JsonBuilder<ObjectNode> log = Json.objectBuilder()//
				.put("method", context.method())//
				.put("path", uri)//
				.put("receivedAt", receivedAt.toString())//
				.put("processedIn", DateTime.now().getMillis() - receivedAt.getMillis());

		Optional<Credentials> credentials = SpaceContext.getCredentials();
		if (credentials.isPresent())
			log.object("credentials")//
					.put("backendId", credentials.get().backendId())//
					.put("name", credentials.get().name())//
					.put("type", credentials.get().level().toString())//
					.end();

		if (!context.query().keys().isEmpty()) {
			log.object("query");
			for (String key : context.query().keys())
				log.put(key, context.get(key));
			log.end();
		}

		String content = null;

		try {
			content = context.request().content();
		} catch (IllegalArgumentException e) {
			// this exception is thrown in batch request for get sub requests
			// TODO: ignore this for now but refactor along batch service
			// refactoring
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		if (!Strings.isNullOrEmpty(content)) {

			// TODO: fix the content type problem
			// String contentType = context.request().contentType();
			// log.put("contentType", contentType);
			// if (PayloadHelper.JSON_CONTENT.equals(contentType))
			// log.node("content", content);
			// else
			// log.put("content", content);

			if (Json.isJsonObject(content))
				log.node("jsonContent", Json.readJsonNode(content));
		}

		if (payload != null) {
			log.put("status", payload.code());

			if (payload.rawContent() instanceof JsonNode)
				log.node("response", (JsonNode) payload.rawContent());
		}

		JsonNode securedLog = Json.fullReplace(log.build(), "password", "******");

		return Start.get().getElasticClient()//
				.prepareIndex(SPACEDOG_BACKEND, TYPE)//
				.setSource(securedLog.toString()).get().getId();
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
