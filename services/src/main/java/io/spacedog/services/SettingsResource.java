package io.spacedog.services;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateResponse;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.SpaceParams;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;

@Prefix("/1/settings")
public class SettingsResource {

	//
	// User constants and schema
	//

	public static final String TYPE = "settings";

	//
	// Routes
	//

	@Put("")
	@Put("/")
	public Payload makeSureIndexIsThere() {

		String backendId = SpaceContext.checkAdminCredentials().backendId();
		ElasticClient elastic = Start.get().getElasticClient();

		if (!elastic.existsIndex(backendId, TYPE)) {
			Context context = SpaceContext.get().context();
			int shards = context.query().getInteger(SpaceParams.SHARDS, SpaceParams.SHARDS_DEFAULT);
			int replicas = context.query().getInteger(SpaceParams.REPLICAS, SpaceParams.REPLICAS_DEFAULT);
			boolean async = context.query().getBoolean(SpaceParams.ASYNC, SpaceParams.ASYNC_DEFAULT);

			ObjectNode mapping = Json.object(TYPE, Json.object("enabled", false));
			elastic.createIndex(backendId, TYPE, mapping.toString(), async, shards, replicas);
		}
		return JsonPayload.success();
	}

	@Delete("")
	@Delete("/")
	public Payload deleteIndex() {
		String backendId = SpaceContext.checkAdminCredentials().backendId();
		ElasticClient elastic = Start.get().getElasticClient();
		elastic.deleteIndex(backendId, TYPE);
		return JsonPayload.success();
	}

	@Get("/:id")
	@Get("/:id/")
	public Payload get(String id) {
		return new Payload(JsonPayload.JSON_CONTENT_UTF8, doGet(id));
	}

	@Put("/:id")
	@Put("/:id/")
	public Payload put(String id, String body) {
		IndexResponse response = doPut(id, body);
		return JsonPayload.saved(response.isCreated(), SpaceContext.backendId(), "/1", //
				response.getType(), response.getId(), response.getVersion());
	}

	@Delete("/:id")
	@Delete("/:id/")
	public Payload delete(String id) {
		delete(id);
		return JsonPayload.success();
	}

	//
	// Internal services
	//

	public String doGet(String id) {

		String backendId = SpaceContext.backendId();
		ElasticClient elastic = Start.get().getElasticClient();

		if (elastic.existsIndex(backendId, TYPE)) {
			GetResponse response = elastic.get(backendId, TYPE, id);
			if (response.isExists())
				return response.getSourceAsString();
		}
		throw Exceptions.notFound(backendId, TYPE, id);
	}

	public IndexResponse doPut(String id, String body) {

		String backendId = SpaceContext.checkAdminCredentials().backendId();
		ElasticClient elastic = Start.get().getElasticClient();

		// Make sure index is created before to save anything
		makeSureIndexIsThere();

		return elastic.prepareIndex(backendId, TYPE, id)//
				.setSource(body).get();
	}

	public UpdateResponse doPatch(String id, String body) {

		String backendId = SpaceContext.checkAdminCredentials().backendId();
		ElasticClient elastic = Start.get().getElasticClient();

		// Make sure index is created before to save anything
		makeSureIndexIsThere();

		return elastic.prepareUpdate(backendId, TYPE, id)//
				.setDoc(body).get();
	}

	public boolean doDelete(String id) {
		String backendId = SpaceContext.checkAdminCredentials().backendId();
		ElasticClient elastic = Start.get().getElasticClient();
		return elastic.delete(backendId, TYPE, id, true);
	}

	//
	// singleton
	//

	private static SettingsResource singleton = new SettingsResource();

	static SettingsResource get() {
		return singleton;
	}

	private SettingsResource() {
	}

}
