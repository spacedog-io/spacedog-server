package io.spacedog.services;

import java.io.IOException;
import java.util.Optional;

import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.QuerySourceBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
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
	// TODO in the near future
	// remove all admin/log routes
	@Get("/admin/log")
	@Get("/admin/log/")
	public Payload getAll(Context context) {

		Credentials credentials = SpaceContext.checkAdminCredentials();

		Optional<String> backendId = credentials.isSuperDogAuthenticated() ? Optional.empty()
				: Optional.of(credentials.backendId());

		SearchResponse response = doGetLogs(backendId, //
				context.request().query().getInteger("from", 0), //
				context.request().query().getInteger("size", 10));

		return extractLogs(response);
	}

	@Get("/log/:backendId")
	@Get("/log/:backendId/")
	@Get("/admin/log/:backendId")
	@Get("/admin/log/:backendId/")
	public Payload getForBackend(String backendId, Context context) {

		SpaceContext.checkSuperDogCredentials();

		SearchResponse response = doGetLogs(Optional.of(backendId), //
				context.request().query().getInteger("from", 0), //
				context.request().query().getInteger("size", 10));

		return extractLogs(response);
	}

	@Delete("/log/:backendId")
	@Delete("/log/:backendId/")
	@Delete("/admin/log/:backendId")
	@Delete("/admin/log/:backendId/")
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

		SearchHit[] hits = doGetLogs(Optional.of(backendId), from, 1)//
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

		DeleteByQueryResponse delete = ElasticHelper.get().delete(AccountResource.ADMIN_INDEX, query, TYPE);

		return Optional.of(delete);
	}

	private SearchResponse doGetLogs(Optional<String> backendId, int from, int size) {

		QueryBuilder query = backendId.isPresent()//
				? QueryBuilders.termQuery("credentials.backendId", backendId.get())//
				: QueryBuilders.matchAllQuery();

		ElasticHelper.get().refresh(true, AccountResource.ADMIN_INDEX);

		return Start.get().getElasticClient()//
				.prepareSearch(AccountResource.ADMIN_INDEX)//
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
				.prepareIndex(AccountResource.ADMIN_INDEX, TYPE)//
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
