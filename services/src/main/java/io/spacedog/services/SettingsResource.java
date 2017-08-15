package io.spacedog.services;

import java.util.Map;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;

import io.spacedog.core.Json8;
import io.spacedog.model.NonDirectlyReadableSettings;
import io.spacedog.model.NonDirectlyUpdatableSettings;
import io.spacedog.model.Settings;
import io.spacedog.model.SettingsSettings;
import io.spacedog.model.SettingsSettings.SettingsAcl;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.NotFoundException;
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
		SpaceContext.credentials().checkAtLeastSuperAdmin();

		if (!elastic().exists(settingsIndex()))
			return JsonPayload.json(JsonPayload.builder()//
					.put("took", 0).put("total", 0).object("results"));

		elastic().refreshType(settingsIndex(), isRefreshRequested(context));

		int from = context.query().getInteger(FROM_PARAM, 0);
		int size = context.query().getInteger(SIZE_PARAM, 10);

		SearchResponse response = elastic().prepareSearch(settingsIndex())//
				.setTypes(TYPE)//
				.setFrom(from)//
				.setSize(size)//
				.setQuery(QueryBuilders.matchAllQuery())//
				.get();

		ObjectNode results = Json8.object();

		for (SearchHit hit : response.getHits().getHits())
			results.set(hit.getId(), Json8.readNode(hit.sourceAsString()));

		return JsonPayload.json(results);
	}

	@Delete("")
	@Delete("/")
	public Payload deleteIndex() {
		SpaceContext.credentials().checkAtLeastSuperAdmin();
		elastic().deleteIndex(settingsIndex());
		return JsonPayload.success();
	}

	@Get("/:id")
	@Get("/:id/")
	public Payload get(String id) {
		checkIfAuthorizedToRead(id);
		try {
			return JsonPayload.json(loadFromElastic(id));
		} catch (NotFoundException e) {
			if (registeredSettingsClasses.containsKey(id))
				return JsonPayload.pojo(instantiateDefaultAsNode(id));
			throw e;
		}
	}

	@Put("/:id")
	@Put("/:id/")
	public Payload put(String id, String body) {
		checkIfAuthorizedToUpdate(id);
		IndexResponse response = saveToElastic(id, body);
		return JsonPayload.toJson("/1", response);
	}

	@Delete("/:id")
	@Delete("/:id/")
	public Payload delete(String id) {
		checkIfAuthorizedToUpdate(id);
		elastic().delete(settingsIndex(), id, false, true);
		return JsonPayload.success();
	}

	@Get("/:id/:field")
	@Get("/:id/:field/")
	public Payload get(String id, String field) {
		checkIfAuthorizedToRead(id);
		ObjectNode object = getAsNode(id);
		if (object == null)
			throw Exceptions.notFound(TYPE, id);
		JsonNode value = Json8.get(object, field);
		value = value == null ? NullNode.getInstance() : value;
		return JsonPayload.json(value, HttpStatus.OK);
	}

	@Put("/:id/:field")
	@Put("/:id/:field/")
	public Payload put(String id, String field, String body) {
		checkIfAuthorizedToUpdate(id);
		ObjectNode object = getAsNode(id);
		object = object == null ? Json8.object() : object;
		JsonNode value = Json8.readNode(body);
		Json8.set(object, field, value);
		String source = object.toString();
		IndexResponse response = saveToElastic(id, source);
		return JsonPayload.toJson("/1", response);
	}

	@Delete("/:id/:field")
	@Delete("/:id/:field/")
	public Payload delete(String id, String field) {
		checkIfAuthorizedToUpdate(id);
		ObjectNode object = getAsNode(id);
		if (object == null)
			throw Exceptions.notFound(TYPE, id);

		Json8.remove(object, field);
		IndexResponse response = setAsNode(id, object);
		return JsonPayload.toJson("/1", response);
	}

	//
	// Internal services
	//

	public <K extends Settings> K getAsObject(Class<K> settingsClass) {
		String id = Settings.id(settingsClass);
		try {
			String source = loadFromElastic(id);
			return Json8.toPojo(source, settingsClass);

		} catch (NotFoundException nfe) {
			return instantiateDefaultAsObject(settingsClass);
		}
	}

	public ObjectNode getAsNode(String id) {
		try {
			String source = loadFromElastic(id);
			return Json8.readObject(source);

		} catch (NotFoundException nfe) {
			return instantiateDefaultAsNode(id);
		}
	}

	public <K extends Settings> void registerSettings(Class<K> settingsClass) {
		if (registeredSettingsClasses == null)
			registeredSettingsClasses = Maps.newHashMap();

		registeredSettingsClasses.put(Settings.id(settingsClass), settingsClass);
	}

	public <T extends Settings> IndexResponse setAsObject(T settings) {
		return saveToElastic(settings.id(), Json8.toString(settings));
	}

	public IndexResponse setAsNode(String id, ObjectNode settings) {
		return saveToElastic(id, settings.toString());
	}

	//
	// implementation
	//

	private static Index settingsIndex() {
		return Index.toIndex(TYPE);
	}

	private ObjectNode instantiateDefaultAsNode(String id) {
		Class<? extends Settings> settingsClass = registeredSettingsClasses.get(id);
		return settingsClass == null ? null //
				: Json8.mapper().valueToTree(instantiateDefaultAsObject(settingsClass));
	}

	private <K extends Settings> K instantiateDefaultAsObject(Class<K> settingsClass) {
		if (registeredSettingsClasses.containsKey(Settings.id(settingsClass))) {
			try {
				return settingsClass.newInstance();

			} catch (InstantiationException | IllegalAccessException e) {
				throw Exceptions.runtime(e, "error instantiating [%s] settings class", //
						settingsClass.getSimpleName());
			}
		}
		throw Exceptions.runtime("settings class [%s] not registered", //
				settingsClass.getSimpleName());
	}

	private Credentials checkIfAuthorizedToRead(String settingsId) {
		Credentials credentials = SpaceContext.credentials();
		if (!credentials.isAtLeastSuperAdmin())
			credentials.checkRoles(getSettingsAcl(settingsId).read());

		Class<? extends Settings> settingsClass = registeredSettingsClasses.get(settingsId);
		if (settingsClass != null)
			if (NonDirectlyReadableSettings.class.isAssignableFrom(settingsClass))
				throw Exceptions.illegalArgument(//
						"[%s] settings is not directly readable", settingsId);

		return credentials;
	}

	private Credentials checkIfAuthorizedToUpdate(String id) {
		Credentials credentials = SpaceContext.credentials();
		if (!credentials.isAtLeastSuperAdmin())
			credentials.checkRoles(getSettingsAcl(id).update());

		Class<? extends Settings> settingsClass = registeredSettingsClasses.get(id);
		if (settingsClass != null)
			if (NonDirectlyUpdatableSettings.class.isAssignableFrom(settingsClass))
				throw Exceptions.illegalArgument(//
						"[%s] settings is not directly updatable", id);

		return credentials;
	}

	private SettingsAcl getSettingsAcl(String settingsId) {
		try {
			return getAsObject(SettingsSettings.class).get(settingsId);
		} catch (NotFoundException ignore) {
		}
		return SettingsAcl.defaultAcl();
	}

	private String loadFromElastic(String id) {
		if (elastic().exists(settingsIndex())) {
			GetResponse response = elastic().get(settingsIndex(), id);
			if (response.isExists())
				return response.getSourceAsString();
		}
		throw Exceptions.notFound(TYPE, id);
	}

	private IndexResponse saveToElastic(String id, String source) {
		checkSettingsAreValid(id, source);
		makeSureIndexIsCreated();
		return elastic().prepareIndex(settingsIndex(), id)//
				.setSource(source).get();
	}

	private void checkSettingsAreValid(String id, String body) {
		if (registeredSettingsClasses.containsKey(id))
			Json8.toPojo(body, registeredSettingsClasses.get(id));
	}

	private void makeSureIndexIsCreated() {

		Index index = settingsIndex();
		ElasticClient elastic = elastic();

		if (!elastic.exists(index)) {
			Context context = SpaceContext.get().context();
			int shards = context.query().getInteger(SHARDS_PARAM, SHARDS_DEFAULT_PARAM);
			int replicas = context.query().getInteger(REPLICAS_PARAM, REPLICAS_DEFAULT_PARAM);
			boolean async = context.query().getBoolean(ASYNC_PARAM, ASYNC_DEFAULT_PARAM);

			ObjectNode mapping = Json8.object(TYPE, Json8.object("enabled", false));
			elastic.createIndex(index, mapping.toString(), async, shards, replicas);
		}
	}

	//
	// singleton
	//

	private static SettingsResource singleton = new SettingsResource();

	public static SettingsResource get() {
		return singleton;
	}

	private SettingsResource() {
		registerSettings(SettingsSettings.class);
	}
}
