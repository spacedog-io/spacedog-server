package io.spacedog.services;

import java.io.IOException;
import java.util.Map;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
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
	// SpaceDog constants and schema
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

		String backendId = SpaceContext.checkSuperAdminCredentials().backendId();
		ElasticClient elastic = Start.get().getElasticClient();

		if (!elastic.existsIndex(backendId, TYPE))
			return JsonPayload.json(JsonPayload.builder()//
					.put("took", 0).put("total", 0).object("results"));

		boolean refresh = context.query().getBoolean(PARAM_REFRESH, false);
		DataStore.get().refreshType(refresh, backendId, TYPE);

		int from = context.query().getInteger(PARAM_FROM, 0);
		int size = context.query().getInteger(PARAM_SIZE, 10);
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
		String backendId = SpaceContext.checkSuperAdminCredentials().backendId();
		ElasticClient elastic = Start.get().getElasticClient();
		elastic.deleteIndex(backendId, TYPE);
		return JsonPayload.success();
	}

	@Get("/:id")
	@Get("/:id/")
	public Payload get(String id) {

		checkIfAuthorizedToRead(id);
		String settingsAsString = null;

		Class<? extends Settings> settingsClass = registeredSettingsClasses.get(id);
		if (settingsClass == null)
			settingsAsString = load(id);
		else
			try {
				Settings settings = load(settingsClass);
				settingsAsString = Json.mapper().writeValueAsString(settings);

			} catch (JsonProcessingException e) {
				throw Exceptions.runtime(e, "invalid [%s] settings");
			}

		return JsonPayload.json(settingsAsString, HttpStatus.OK);
	}

	@Put("/:id")
	@Put("/:id/")
	public Payload put(String id, String body) {

		checkIfAuthorizedToUpdate(id);
		IndexResponse response = null;

		Class<? extends Settings> settingsClass = registeredSettingsClasses.get(id);
		if (settingsClass == null)
			response = save(id, body);
		else
			try {
				Settings settings = Json.mapper().readValue(body, settingsClass);
				response = save(settings);

			} catch (IOException e) {
				throw Exceptions.illegalArgument(e, "invalid [%s] settings", id);
			}

		return JsonPayload.saved(response.isCreated(), SpaceContext.backendId(), "/1", //
				response.getType(), response.getId(), response.getVersion());
	}

	@Delete("/:id")
	@Delete("/:id/")
	public Payload delete(String id) {

		Credentials credentials = checkIfAuthorizedToUpdate(id);
		ElasticClient elastic = Start.get().getElasticClient();
		elastic.delete(credentials.backendId(), TYPE, id, false, true);
		return JsonPayload.success();
	}

	@Get("/:id/:field")
	@Get("/:id/:field/")
	public Payload get(String id, String field) {

		checkIfAuthorizedToRead(id);

		Class<? extends Settings> settingsClass = registeredSettingsClasses.get(id);

		ObjectNode settings = settingsClass == null ? Json.readObject(load(id)) //
				: Json.mapper().valueToTree(load(settingsClass));

		JsonNode value = settings.get(field);
		if (value == null)
			value = NullNode.getInstance();

		return JsonPayload.json(value, HttpStatus.OK);
	}

	@Put("/:id/:field")
	@Put("/:id/:field/")
	public Payload put(String id, String field, String body) {

		checkIfAuthorizedToUpdate(id);
		boolean created = false;
		ObjectNode settings = null;

		try {
			Class<? extends Settings> settingsClass = registeredSettingsClasses.get(id);
			settings = settingsClass == null ? Json.readObject(load(id)) //
					: Json.mapper().valueToTree(load(settingsClass));

		} catch (NotFoundException e) {
			settings = Json.object();
			created = true;
		}

		settings.set(field, Json.readNode(body));
		IndexResponse response = save(id, settings.toString());

		return JsonPayload.saved(created, SpaceContext.backendId(), "/1", //
				response.getType(), response.getId(), response.getVersion());
	}

	@Delete("/:id/:field")
	@Delete("/:id/:field/")
	public Payload delete(String id, String field) {

		checkIfAuthorizedToUpdate(id);

		Class<? extends Settings> settingsClass = registeredSettingsClasses.get(id);

		ObjectNode settings = settingsClass == null ? Json.readObject(load(id)) //
				: Json.mapper().valueToTree(load(settingsClass));

		settings.remove(field);
		IndexResponse response = save(id, settings.toString());

		return JsonPayload.saved(false, SpaceContext.backendId(), "/1", //
				response.getType(), response.getId(), response.getVersion());
	}

	//
	// Internal services
	//

	public <K extends Settings> K load(Class<K> settingsClass) {
		String id = Settings.id(settingsClass);

		try {
			String json = load(id);
			return Json.mapper().readValue(json, settingsClass);

		} catch (NotFoundException nfe) {
			// settings not set yet, return a default instance
			try {
				return settingsClass.newInstance();

			} catch (InstantiationException | IllegalAccessException e) {
				throw Exceptions.runtime(e, "error instanciating [%s] settings class", //
						settingsClass.getSimpleName());
			}
		} catch (IOException e) {
			throw Exceptions.runtime(e, "error mapping [%s] settings to [%s] class", //
					id, settingsClass.getSimpleName());
		}
	}

	public IndexResponse save(Settings settings) {
		try {
			String settingsAsString = Json.mapper().writeValueAsString(settings);
			return save(settings.id(), settingsAsString);

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

	private Credentials checkIfAuthorizedToRead(String id) {
		Credentials credentials = SpaceContext.getCredentials();
		if (!credentials.isAtLeastSuperAdmin())
			credentials.checkRoles(getSettingsAcl(id).read());

		Class<? extends Settings> settingsClass = registeredSettingsClasses.get(id);
		if (settingsClass != null)
			if (NonDirectlyReadableSettings.class.isAssignableFrom(settingsClass))
				throw Exceptions.illegalArgument(//
						"[%s] settings is not directly readable", id);

		return credentials;
	}

	private Credentials checkIfAuthorizedToUpdate(String id) {
		Credentials credentials = SpaceContext.getCredentials();
		if (!credentials.isAtLeastSuperAdmin())
			credentials.checkRoles(getSettingsAcl(id).update());

		Class<? extends Settings> settingsClass = registeredSettingsClasses.get(id);
		if (settingsClass != null)
			if (NonDirectlyUpdatableSettings.class.isAssignableFrom(settingsClass))
				throw Exceptions.illegalArgument(//
						"[%s] settings is not directly updatable", id);

		return credentials;
	}

	private SettingsAcl getSettingsAcl(String id) {
		try {
			return load(SettingsSettings.class).get(id);

		} catch (NotFoundException ignore) {
		}

		return SettingsAcl.defaultAcl();
	}

	private String load(String id) {
		String settings = SpaceContext.getSettings(id);

		if (settings == null) {
			String backendId = SpaceContext.backendId();
			ElasticClient elastic = Start.get().getElasticClient();

			if (elastic.existsIndex(backendId, TYPE)) {
				GetResponse response = elastic.get(backendId, TYPE, id);

				if (response.isExists()) {
					settings = response.getSourceAsString();
					SpaceContext.setSettings(id, settings);
				}
			}

			if (settings == null)
				throw Exceptions.notFound(backendId, TYPE, id);
		}

		return settings;
	}

	private IndexResponse save(String id, String body) {
		// Make sure index is created before to save anything
		makeSureIndexIsCreated();

		IndexResponse response = Start.get().getElasticClient()//
				.prepareIndex(SpaceContext.backendId(), TYPE, id)//
				.setSource(body).get();

		SpaceContext.setSettings(id, body);
		return response;
	}

	private void makeSureIndexIsCreated() {

		String backendId = SpaceContext.backendId();
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
