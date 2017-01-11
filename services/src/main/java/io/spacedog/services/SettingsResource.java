package io.spacedog.services;

import java.io.IOException;
import java.util.Map;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;

import io.spacedog.utils.Check;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.NonDirectlyReadableSettings;
import io.spacedog.utils.NonDirectlyUpdatableSettings;
import io.spacedog.utils.NotFoundException;
import io.spacedog.utils.Settings;
import io.spacedog.utils.SettingsSettings;
import io.spacedog.utils.SettingsSettings.SettingsAcl;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

@Prefix("/1/settings")
public class SettingsResource extends Resource {

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

		String backendId = SpaceContext.checkSuperAdminCredentials().target();
		ElasticClient elastic = Start.get().getElasticClient();

		if (!elastic.existsIndex(backendId, TYPE))
			return JsonPayload.json(JsonPayload.builder()//
					.put("took", 0).put("total", 0).object("results"));

		boolean refresh = context.query().getBoolean(PARAM_REFRESH, false);
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
		String backendId = SpaceContext.checkSuperAdminCredentials().target();
		ElasticClient elastic = Start.get().getElasticClient();
		elastic.deleteIndex(backendId, TYPE);
		return JsonPayload.success();
	}

	@Get("/:id")
	@Get("/:id/")
	public Payload get(String id) {

		Credentials credentials = SpaceContext.getCredentials();
		if (!credentials.isAtLeastSuperAdmin())
			credentials.checkRoles(getSettingsAcl(id).read());

		String settingsAsString = null;

		if (registeredSettingsClasses.containsKey(id)) {
			try {
				Class<? extends Settings> settingsClass = registeredSettingsClasses.get(id);
				if (NonDirectlyReadableSettings.class.isAssignableFrom(settingsClass))
					throw Exceptions.illegalArgument(//
							"[%s] settings is not directly readable", id);

				Settings settings = load(settingsClass);
				settingsAsString = Json.mapper().writeValueAsString(settings);

			} catch (JsonProcessingException e) {
				throw Exceptions.runtime(e, "invalid [%s] settings");
			}
		} else
			settingsAsString = load(id);

		return JsonPayload.json(settingsAsString, HttpStatus.OK);
	}

	@Put("/:id")
	@Put("/:id/")
	public Payload put(String id, String body) {

		Credentials credentials = SpaceContext.getCredentials();
		if (!credentials.isAtLeastSuperAdmin())
			credentials.checkRoles(getSettingsAcl(id).update());

		IndexResponse response = null;

		if (registeredSettingsClasses.containsKey(id)) {
			try {
				Class<? extends Settings> settingsClass = registeredSettingsClasses.get(id);
				if (NonDirectlyUpdatableSettings.class.isAssignableFrom(settingsClass))
					throw Exceptions.illegalArgument(//
							"[%s] settings is not directly updatable", id);
				Settings settings = Json.mapper().readValue(body, settingsClass);
				response = save(settings);

			} catch (IOException e) {
				throw Exceptions.illegalArgument(e, "invalid [%s] settings", id);
			}
		} else
			response = save(id, body);

		return JsonPayload.saved(response.isCreated(), SpaceContext.target(), "/1", //
				response.getType(), response.getId(), response.getVersion());
	}

	@Delete("/:id")
	@Delete("/:id/")
	public Payload delete(String id) {

		Credentials credentials = SpaceContext.getCredentials();
		if (!credentials.isAtLeastSuperAdmin())
			credentials.checkRoles(getSettingsAcl(id).update());

		ElasticClient elastic = Start.get().getElasticClient();
		elastic.delete(credentials.backendId(), TYPE, id, false, true);
		return JsonPayload.success();
	}

	//
	// Internal services
	//

	public <K extends Settings> K load(Class<K> settingsClass) {
		K settings = SpaceContext.getSettings(settingsClass);
		if (settings == null) {
			String id = Settings.id(settingsClass);

			try {
				String json = load(id);
				settings = Json.mapper().readValue(json, settingsClass);
				SpaceContext.setSettings(settings);

			} catch (NotFoundException nfe) {
				// settings not set yet, return a default instance
				try {
					settings = settingsClass.newInstance();

				} catch (InstantiationException | IllegalAccessException e) {
					throw Exceptions.runtime(e, "error instanciating [%s] settings class", //
							settingsClass.getSimpleName());
				}
			} catch (IOException e) {
				throw Exceptions.runtime(e, "error mapping [%s] settings to [%s] class", //
						id, settingsClass.getSimpleName());
			}
		}
		return settings;
	}

	public IndexResponse save(Settings settings) {
		try {
			String settingsAsString = Json.mapper().writeValueAsString(settings);
			IndexResponse response = save(settings.id(), settingsAsString);
			SpaceContext.setSettings(settings);
			return response;
		} catch (JsonProcessingException e) {
			throw Exceptions.runtime(e);
		}
	}

	<K extends Settings> void registerSettingsClass(Class<K> settingsClass) {
		if (registeredSettingsClasses == null)
			registeredSettingsClasses = Maps.newHashMap();

		registeredSettingsClasses.put(Settings.id(settingsClass), settingsClass);
	}

	//
	// implementation
	//

	private SettingsAcl getSettingsAcl(String id) {
		try {
			return load(SettingsSettings.class).get(id);

		} catch (NotFoundException ignore) {
		}

		return SettingsAcl.defaultAcl();
	}

	private String load(String id) {
		String backendId = SpaceContext.target();
		ElasticClient elastic = Start.get().getElasticClient();

		if (elastic.existsIndex(backendId, TYPE)) {
			GetResponse response = elastic.get(backendId, TYPE, id);
			if (response.isExists())
				return response.getSourceAsString();
		}
		throw Exceptions.notFound(backendId, TYPE, id);
	}

	private IndexResponse save(String id, String body) {
		// Make sure index is created before to save anything
		makeSureIndexIsCreated();

		return Start.get().getElasticClient()//
				.prepareIndex(SpaceContext.target(), TYPE, id)//
				.setSource(body).get();
	}

	private void makeSureIndexIsCreated() {

		String backendId = SpaceContext.target();
		ElasticClient elastic = Start.get().getElasticClient();

		if (!elastic.existsIndex(backendId, TYPE)) {
			Context context = SpaceContext.get().context();
			int shards = context.query().getInteger(PARAM_SHARDS, PARAM_SHARDS_DEFAULT);
			int replicas = context.query().getInteger(PARAM_REPLICAS, PARAM_REPLICAS_DEFAULT);
			boolean async = context.query().getBoolean(PARAM_ASYNC, PARAM_ASYNC_DEFAULT);

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
