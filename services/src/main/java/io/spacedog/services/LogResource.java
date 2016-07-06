package io.spacedog.services;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.mashape.unirest.http.HttpMethod;

import io.spacedog.services.Credentials.Level;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.SpaceHeaders;
import io.spacedog.utils.SpaceParams;
import io.spacedog.utils.Utils;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.filters.PayloadSupplier;
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

		return new SpaceFilter() {

			private static final long serialVersionUID = 5621427145724229373L;

			@Override
			public boolean matches(String uri, Context context) {

				// https://api.spacedog.io ping requests should not be logged
				if (uri.isEmpty() || uri.equals(SLASH))
					return !SpaceContext.getCredentials().isRootBackend();

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

	private Optional<DeleteByQueryResponse> doPurgeBackend(String backendId, int from) {

		SearchHit[] hits = doGetLogs(Optional.of(backendId), from, 1, Optional.empty(), Optional.empty())//
				.getHits().getHits();

		if (hits == null || hits.length == 0)
			// no log to delete
			return Optional.empty();

		String receivedAt = Json.readObject(hits[0].sourceAsString())//
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

		ObjectNode log = Json.object(//
				"method", context.method(), "path", uri, //
				"receivedAt", receivedAt.toString(), "processedIn",
				DateTime.now().getMillis() - receivedAt.getMillis());

		Credentials credentials = SpaceContext.getCredentials();
		ObjectNode logCredentials = log.putObject("credentials");
		logCredentials.put("backendId", credentials.backendId());
		logCredentials.put("type", credentials.level().toString());
		if (!credentials.level().equals(Level.KEY))
			logCredentials.put("name", credentials.name());

		if (!context.query().keys().isEmpty()) {
			ObjectNode logQuery = log.putObject("query");
			for (String key : context.query().keys()) {
				String value = key.equals(PASSWORD) //
						? "******" : context.get(key);
				logQuery.put(key, value);
			}
		}

		for (Entry<String, List<String>> entry : context.request().headers().entrySet()) {
			if (entry.getKey().equalsIgnoreCase(SpaceHeaders.AUTHORIZATION))
				continue;
			if (entry.getKey().equalsIgnoreCase(SpaceHeaders.USER_AGENT)) {
				String userAgent = entry.getValue().toString();
				log.with("headers").put(entry.getKey(), //
						userAgent.substring(1, userAgent.length() - 1));
				continue;
			}
			if (entry.getValue().size() == 1)
				log.with("headers").put(entry.getKey(), entry.getValue().get(0));
			else if (entry.getValue().size() > 1) {
				ArrayNode array = log.with("headers").putArray(entry.getKey());
				for (String string : entry.getValue())
					array.add(string);
			}
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

			if (Json.isObject(content))
				try {
					JsonNode securedContent = Json.fullReplaceTextualFields(//
							Json.readNode(content), "password", "******");

					log.set("jsonContent", securedContent);
				} catch (Exception e) {
					// I just do not log the content if I can not parse it
				}
		}

		if (payload != null) {
			log.put("status", payload.code());

			if (isNotGetLogRequest(context) //
					&& payload.rawContent() instanceof JsonNode)
				log.set("response", (JsonNode) payload.rawContent());
		}

		return Start.get().getElasticClient()//
				.prepareIndex(SPACEDOG_BACKEND, TYPE)//
				.setSource(log.toString()).get().getId();
	}

	private boolean isNotGetLogRequest(Context context) {
		return !(HttpMethod.GET.toString().equals(context.method()) //
				&& context.uri().equals("/1/log"));
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
