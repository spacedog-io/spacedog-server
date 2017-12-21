package io.spacedog.server;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.QuerySourceBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import io.spacedog.http.SpaceBackend;
import io.spacedog.utils.ClassResources;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Json;
import io.spacedog.utils.SpaceHeaders;
import io.spacedog.utils.Utils;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.filters.PayloadSupplier;
import net.codestory.http.payload.Payload;

@Prefix("/1/log")
public class LogService extends SpaceService {

	private static final String PAYLOAD_FIELD = "payload";
	private static final String CREDENTIALS_FIELD = "credentials";
	private static final String PARAMETERS_FIELD = "parameters";
	private static final String HEADERS_FIELD = "headers";
	public static final String TYPE = "log";
	public static final String PURGE_ALL = "purgeall";

	//
	// init
	//

	public void initIndex(String backendId) {
		String mapping = ClassResources.loadAsString(this, "log-mapping.json");
		Index index = logIndex().backendId(backendId);

		if (!elastic().exists(index))
			elastic().createIndex(index, mapping, false);
	}

	//
	// Routes
	//

	@Get("")
	@Get("/")
	public Payload getAll(Context context) {
		SpaceContext.credentials().checkAtLeastSuperAdmin();

		int from = context.query().getInteger(FROM_PARAM, 0);
		int size = context.query().getInteger(SIZE_PARAM, 10);
		elastic().refreshType(logIndex(), isRefreshRequested(context));

		SearchResponse response = elastic().prepareSearch(logIndex())//
				.setTypes(TYPE)//
				.setQuery(QueryBuilders.matchAllQuery())//
				.addSort(RECEIVED_AT_FIELD, SortOrder.DESC)//
				.setFrom(from)//
				.setSize(size)//
				.get();

		return extractLogs(response);
	}

	@Post("/search")
	@Post("/search/")
	public Payload search(String body, Context context) {

		SpaceContext.credentials().checkAtLeastAdmin();
		elastic().refreshType(logIndex(), isRefreshRequested(context));
		SearchResponse response = elastic().prepareSearch(logIndex())//
				.setTypes(TYPE).setSource(body).get();
		return extractLogs(response);
	}

	@Delete("")
	@Delete("/")
	public Payload purge(Context context) {

		SpaceContext.credentials().checkAtLeastSuperAdmin();

		String param = context.request().query().get(BEFORE_PARAM);
		DateTime before = param == null ? DateTime.now().minusDays(7) //
				: DateTime.parse(param);

		DeleteByQueryResponse response = doPurge(before);
		return ElasticPayload.deleted(response).build();
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
					return !SpaceContext.backend().isDefault();

				return true;
			}

			@Override
			public Payload apply(String uri, Context context, PayloadSupplier nextFilter) throws Exception {
				Payload payload = null;
				DateTime receivedAt = DateTime.now();

				try {
					payload = nextFilter.get();
				} catch (Throwable t) {
					payload = JsonPayload.error(t).build();
				}

				if (payload == null)
					payload = JsonPayload.error(500, //
							"unexpected null payload for [%s] request to [%s]", context.method(), uri)//
							.build();

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

	private boolean hasPurgeAllRole(Credentials credentials) {
		return credentials.isSuperDog() || credentials.roles().contains(PURGE_ALL);
	}

	private DeleteByQueryResponse doPurge(DateTime before) {

		RangeQueryBuilder builder = QueryBuilders.rangeQuery(RECEIVED_AT_FIELD)//
				.lt(before.toString());

		String query = new QuerySourceBuilder().setQuery(builder).toString();
		DeleteByQueryResponse response = elastic().deleteByQuery(query, logIndex());
		return response;
	}

	private Payload extractLogs(SearchResponse response) {

		ArrayNode array = Json.array();
		for (SearchHit hit : response.getHits().getHits())
			array.add(Json.readNode(hit.sourceAsString()));

		return JsonPayload.ok()//
				.withFields("took", response.getTookInMillis())//
				.withFields("total", response.getHits().getTotalHits())//
				.withResults(array)//
				.build();
	}

	private String log(String uri, Context context, DateTime receivedAt, Payload payload) {

		ObjectNode log = Json.object(//
				"method", context.method(), //
				"path", uri, //
				RECEIVED_AT_FIELD, receivedAt.toString(), //
				"processedIn", DateTime.now().getMillis() - receivedAt.getMillis(), //
				"status", payload == null ? 500 : payload.code());

		addCredentials(log);
		addQuery(log, context);
		addHeaders(log, context.request().headers().entrySet());
		addRequestPayload(log, context);
		addResponsePayload(log, payload, context);

		// in case incoming request is targeting non existent backend
		// or backend without any log index, they should be logged to
		// default backend
		Index indexToLogTo = logIndex();

		if (!elastic().exists(indexToLogTo))
			indexToLogTo.backendId(SpaceBackend.defaultBackendId());

		return elastic().index(indexToLogTo, log.toString()).getId();
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
							Json.readNode(content), PASSWORD_FIELD, "********");

					log.set(PAYLOAD_FIELD, securedContent);
				}
			}
		} catch (Exception e) {
			log.set(PAYLOAD_FIELD, Json.object(ERROR_FIELD, JsonPayload.toJson(e, true)));
		}
	}

	private void addQuery(ObjectNode log, Context context) {
		ArrayNode parametersNode = Json.array();

		for (String key : context.query().keys()) {
			String value = key.equals(PASSWORD_FIELD) //
					? "********"
					: context.get(key);
			parametersNode.add(key + ": " + value);
		}

		if (parametersNode.size() > 0)
			log.set(PARAMETERS_FIELD, parametersNode);
	}

	private void addCredentials(ObjectNode log) {
		Credentials credentials = SpaceContext.credentials();
		ObjectNode logCredentials = log.putObject(CREDENTIALS_FIELD);
		logCredentials.put(ID_FIELD, credentials.id());
		logCredentials.put(USERNAME_FIELD, credentials.name());
		logCredentials.put(TYPE_FIELD, credentials.type().name());
	}

	private void addHeaders(ObjectNode log, Set<Entry<String, List<String>>> headers) {

		ArrayNode headersNode = Json.array();
		for (Entry<String, List<String>> header : headers) {

			String key = header.getKey();
			List<String> values = header.getValue();

			if (key.equalsIgnoreCase(SpaceHeaders.AUTHORIZATION))
				continue;

			if (Utils.isNullOrEmpty(values))
				continue;

			for (String value : values)
				headersNode.add(key + ": " + value);
		}
		if (headersNode.size() > 0)
			log.set(HEADERS_FIELD, headersNode);
	}

	public static Index logIndex() {
		return Index.toIndex(TYPE);
	}

	//
	// Singleton
	//

	private static LogService singleton = new LogService();

	static LogService get() {
		return singleton;
	}

	private LogService() {
	}
}
