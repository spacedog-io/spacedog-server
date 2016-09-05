package io.spacedog.services;

import java.io.IOException;
import java.util.Map;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import com.beust.jcommander.internal.Maps;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.utils.Check;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.NotFoundException;
import io.spacedog.utils.Settings;
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
	// Fields
	//

	private Map<String, Class<? extends Settings>> registeredSettingsClasses;

	//
	// Routes
	//

	@Get("")
	@Get("/")
	public Payload getAll(Context context) {

		String backendId = SpaceContext.backendId();
		ElasticClient elastic = Start.get().getElasticClient();

		if (!elastic.existsIndex(backendId, TYPE))
			return JsonPayload.json(JsonPayload.builder()//
					.put("took", 0).put("total", 0).object("results"));

		boolean refresh = context.query().getBoolean(SpaceParams.REFRESH, false);
		DataStore.get().refreshType(refresh, backendId, TYPE);

		int from = context.query().getInteger("from", 0);
		int size = context.query().getInteger("size", 10);
		Check.isTrue(from + size <= 1000, "from + size is greater than 1000");

		SearchResponse response = elastic.prepareSearch(backendId, TYPE)//
				.setTypes(TYPE)//
				.setFrom(from)//
				.setSize(size)//
				.setQuery(QueryBuilders.matchAllQuery())//
				.get();

		ObjectNode results = Json.object();

		for (SearchHit hit : response.getHits().getHits())
			results.set(hit.getId(), Json.readNode(hit.sourceAsString()));

		return JsonPayload.json(results);
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
		doDelete(id);
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

		// Check settings object is valid
		checkSettingsValid(id, body);

		// Make sure index is created before to save anything
		makeSureIndexIsCreated();

		return Start.get().getElasticClient().prepareIndex(backendId, TYPE, id)//
				.setSource(body).get();
	}

	public <K extends Settings> K load(Class<K> settingsClass) {
		K settings = SpaceContext.getCachedSettings(settingsClass);
		if (settings == null) {
			String id = Settings.id(settingsClass);

			try {
				String json = doGet(id);
				settings = Json.mapper().readValue(json, settingsClass);
			} catch (NotFoundException nfe) {
				// settings not set yet, return a default instance
				try {
					settings = settingsClass.newInstance();
				} catch (InstantiationException | IllegalAccessException e) {
					throw Exceptions.runtime(e, "invalid settings class [%s]", //
							settingsClass.getSimpleName());
				}
			} catch (IOException e) {
				throw Exceptions.runtime(e, "invalid [%s] settings", id);
			}
		}
		SpaceContext.setCachedSettings(settings);
		return settings;
	}

	public void save(Settings settings) {
		try {
			SettingsResource.get().doPut(settings.id(), //
					Json.mapper().writeValueAsString(settings));
		} catch (JsonProcessingException e) {
			throw Exceptions.runtime(e);
		}

		SpaceContext.setCachedSettings(settings);
	}

	private void checkSettingsValid(String id, String body) {
		try {

			if (registeredSettingsClasses.containsKey(id))
				Json.mapper().readValue(body, registeredSettingsClasses.get(id));

		} catch (IOException e) {
			throw Exceptions.illegalArgument(e, "invalid [%s] settings", id);
		}
	}

	public UpdateResponse doPatch(String id, String body) {

		String backendId = SpaceContext.checkAdminCredentials().backendId();
		ElasticClient elastic = Start.get().getElasticClient();

		// Make sure index is created before to save anything
		makeSureIndexIsCreated();

		return elastic.prepareUpdate(backendId, TYPE, id)//
				.setDoc(body).get();
	}

	public boolean doDelete(String id) {
		String backendId = SpaceContext.checkAdminCredentials().backendId();
		ElasticClient elastic = Start.get().getElasticClient();
		return elastic.delete(backendId, TYPE, id, true);
	}

	<K extends Settings> void registerSettingsClass(Class<K> settingsClass) {
		if (registeredSettingsClasses == null)
			registeredSettingsClasses = Maps.newHashMap();

		registeredSettingsClasses.put(Settings.id(settingsClass), settingsClass);
	}

	private void makeSureIndexIsCreated() {

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
