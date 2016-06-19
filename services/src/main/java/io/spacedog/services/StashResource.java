package io.spacedog.services;

import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.utils.Check;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.SpaceParams;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

@Prefix("/1/stash")
public class StashResource {

	//
	// User constants and schema
	//

	public static final String TYPE = "stash";

	//
	// Routes
	//

	@Get("")
	@Get("/")
	public Payload getAll(Context context) {
		Credentials credentials = SpaceContext.checkAdminCredentials();
		boolean refresh = context.query().getBoolean(SpaceParams.REFRESH, false);
		DataStore.get().refreshType(refresh, credentials.backendId(), TYPE);
		ElasticClient elastic = Start.get().getElasticClient();

		int from = context.query().getInteger("from", 0);
		int size = context.query().getInteger("size", 10);
		Check.isTrue(from + size <= 10000, "from + size is greater than 10.000");

		SearchResponse response = elastic.prepareSearch(credentials.backendId(), TYPE)//
				.setTypes(TYPE)//
				.setFrom(from)//
				.setSize(size)//
				.setFetchSource(context.query().getBoolean("fetch-contents", true))//
				.setQuery(QueryBuilders.matchAllQuery())//
				.get();

		JsonBuilder<ObjectNode> builder = JsonPayload.minimalBuilder(HttpStatus.OK)//
				.put("took", response.getTookInMillis())//
				.put("total", response.getHits().getTotalHits())//
				.array("results");

		for (SearchHit hit : response.getHits().getHits())
			builder.node(hit.sourceAsString());

		return JsonPayload.json(builder);
	}

	@Put("")
	@Put("/")
	public Payload createIndex(Context context) {

		Credentials credentials = SpaceContext.checkAdminCredentials();

		String backendId = credentials.backendId();
		ElasticClient elastic = Start.get().getElasticClient();
		boolean indexExists = elastic.existsIndex(backendId, TYPE);
		String mapping = Json.object(TYPE, //
				Json.object("enabled", false)).toString();

		if (indexExists)
			elastic.putMapping(backendId, TYPE, mapping);
		else {
			int shards = context.query().getInteger(SpaceParams.SHARDS, SpaceParams.SHARDS_DEFAULT);
			int replicas = context.query().getInteger(SpaceParams.REPLICAS, SpaceParams.REPLICAS_DEFAULT);
			boolean async = context.query().getBoolean(SpaceParams.ASYNC, SpaceParams.ASYNC_DEFAULT);
			elastic.createIndex(backendId, TYPE, mapping, async, shards, replicas);
		}

		return JsonPayload.success();
	}

	@Get("/:id")
	@Get("/:id/")
	public Payload getById(String id, Context context) {
		Credentials credentials = SpaceContext.checkCredentials();

		GetResponse response = Start.get().getElasticClient()//
				.get(credentials.backendId(), TYPE, id);

		return response.isExists() //
				? new Payload(JsonPayload.JSON_CONTENT_UTF8, response.getSourceAsString()) //
				: JsonPayload.error(HttpStatus.NOT_FOUND);
	}

	@Put("/:id")
	@Put("/:id/")
	public Payload put(String id, String body, Context context) {
		Credentials credentials = SpaceContext.checkAdminCredentials();
		String backendId = credentials.backendId();

		IndexResponse response = Start.get().getElasticClient()//
				.prepareIndex(backendId, TYPE, id).setSource(body).get();

		return JsonPayload.saved(response.isCreated(), backendId, "/1", //
				response.getType(), response.getId(), response.getVersion());
	}

	@Delete("/:id")
	@Delete("/:id/")
	public Payload delete(String id, Context context) {
		Credentials credentials = SpaceContext.checkAdminCredentials();

		DeleteResponse delete = Start.get().getElasticClient()//
				.delete(credentials.backendId(), TYPE, id);

		return delete.isFound() ? JsonPayload.success() //
				: JsonPayload.error(HttpStatus.NOT_FOUND);
	}

	//
	// singleton
	//

	private static StashResource singleton = new StashResource();

	static StashResource get() {
		return singleton;
	}

	private StashResource() {
	}

}
