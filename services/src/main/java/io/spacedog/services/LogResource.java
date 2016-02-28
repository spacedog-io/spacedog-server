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

import io.spacedog.services.Credentials.Type;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.SpaceParams;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.payload.Payload;

@Prefix("/v1")
public class LogResource {

	public static final String TYPE = "log";

	//
	// Routes
	//

	@Get("/log")
	@Get("/log/")
	public Payload getAll(Context context) {

		Credentials credentials = SpaceContext.checkAdminCredentials();

		Optional<String> backendId = credentials.isSuperDogAuthenticated() ? Optional.empty()
				: Optional.of(credentials.backendId());

		String logType = context.query().get(SpaceParams.LOG_TYPE);
		Optional<Type> type = Strings.isNullOrEmpty(logType) ? Optional.empty()//
				: Optional.of(Type.valueOf(logType));

		SearchResponse response = doGetLogs(backendId, //
				context.query().getInteger("from", 0), //
				context.query().getInteger("size", 10), //
				type);

		return extractLogs(response);
	}

	@Get("/log/:backendId")
	@Get("/log/:backendId/")
	public Payload getForBackend(String backendId, Context context) {

		SpaceContext.checkSuperDogCredentials();

		String logType = context.query().get(SpaceParams.LOG_TYPE);
		Optional<Type> type = Strings.isNullOrEmpty(logType) ? Optional.empty()//
				: Optional.of(Type.valueOf(logType));

		SearchResponse response = doGetLogs(Optional.of(backendId), //
				context.request().query().getInteger("from", 0), //
				context.request().query().getInteger("size", 10), //
				type);

		return extractLogs(response);
	}

	@Delete("/log/:backendId")
	@Delete("/log/:backendId/")
	public Payload purgeBackend(String backendId, Context context) {

		SpaceContext.checkSuperDogCredentialsFor(backendId);

		Optional<DeleteByQueryResponse> response = doPurgeBackend(backendId, //
				context.request().query().getInteger("from", 1000));

		// no delete response means no logs to delete means success

		return response.isPresent()//
				? Payloads.json(response.get()) : Payloads.success();
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
				payload = Payloads.error(t);
			}

			if (payload == null)
				payload = Payloads.error(500, //
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

		SearchHit[] hits = doGetLogs(Optional.of(backendId), from, 1, Optional.empty())//
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
				.deleteByQuery(AccountResource.ADMIN_BACKEND, TYPE, query);

		return Optional.of(delete);
	}

	private SearchResponse doGetLogs(Optional<String> backendId, int from, int size, Optional<Credentials.Type> type) {

		BoolQueryBuilder query = QueryBuilders.boolQuery();

		if (backendId.isPresent())
			query.filter(QueryBuilders.termQuery("credentials.backendId", backendId.get()));

		if (type.isPresent())
			query.filter(QueryBuilders.termsQuery("credentials.type", //
					Lists.newArrayList(type.get().lowerOrEqual())));

		DataStore.get().refreshType(true, AccountResource.ADMIN_BACKEND, TYPE);

		return Start.get().getElasticClient()//
				.prepareSearch(AccountResource.ADMIN_BACKEND, TYPE)//
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

		return Payloads.json(builder);
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
					.put("type", credentials.get().type().toString())//
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

		if (payload.rawContent() instanceof JsonNode)
			log.node("response", (JsonNode) payload.rawContent());

		JsonNode securedLog = Json.fullReplace(log.build(), "password", "******");

		return Start.get().getElasticClient()//
				.prepareIndex(AccountResource.ADMIN_BACKEND, TYPE)//
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
