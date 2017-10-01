package io.spacedog.server;

import java.util.Map;
import java.util.Optional;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;

import io.spacedog.model.Settings;
import io.spacedog.model.SettingsSettings;
import io.spacedog.model.SettingsSettings.SettingsAcl;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.NotFoundException;
import io.spacedog.utils.Utils;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
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
			return JsonPayload.ok()//
					.withFields("took", 0, "total", 0)//
					.withResults(Json.array())//
					.build();

		elastic().refreshType(settingsIndex(), isRefreshRequested(context));

		int from = context.query().getInteger(FROM_PARAM, 0);
		int size = context.query().getInteger(SIZE_PARAM, 10);

		SearchResponse response = elastic().prepareSearch(settingsIndex())//
				.setTypes(TYPE)//
				.setFrom(from)//
				.setSize(size)//
				.setQuery(QueryBuilders.matchAllQuery())//
				.get();

		ObjectNode results = Json.object();

		for (SearchHit hit : response.getHits().getHits())
			results.set(hit.getId(), Json.readNode(hit.sourceAsString()));

		return JsonPayload.ok().withObject(results).build();
	}

	@Delete("")
	@Delete("/")
	public Payload deleteIndex() {
		SpaceContext.credentials().checkAtLeastSuperAdmin();
		elastic().deleteIndex(settingsIndex());
		return JsonPayload.ok().build();
	}

	@Get("/:id")
	@Get("/:id/")
	public Payload get(String id) {
		checkIfAuthorizedToRead(id);
		Optional<ObjectNode> object = getAsNode(id);

		if (object.isPresent())
			return JsonPayload.ok()//
					.withObject(object.get()).build();

		if (registeredSettingsClasses.containsKey(id))
			return JsonPayload.ok()//
					.withObject(instantiateDefaultAsNode(id)).build();

		throw Exceptions.notFound(TYPE, id);
	}

	@Put("/:id")
	@Put("/:id/")
	public Payload put(String id, String body) {
		checkIfNotInternalSettings(id);
		checkIfAuthorizedToUpdate(id);
		IndexResponse response = saveToElastic(id, body);
		return ElasticPayload.saved("/1", response).build();
	}

	@Delete("/:id")
	@Delete("/:id/")
	public Payload delete(String id) {
		checkIfAuthorizedToUpdate(id);
		elastic().delete(settingsIndex(), id, false, true);
		return JsonPayload.ok().build();
	}

	@Get("/:id/:field")
	@Get("/:id/:field/")
	public Payload get(String id, String field) {
		checkIfAuthorizedToRead(id);
		ObjectNode object = getAsNode(id)//
				.orElseThrow(() -> Exceptions.notFound(TYPE, id));
		JsonNode value = Json.get(object, field);
		value = value == null ? NullNode.getInstance() : value;
		return JsonPayload.ok().withObject(value).build();
	}

	@Put("/:id/:field")
	@Put("/:id/:field/")
	public Payload put(String id, String field, String body) {
		checkIfNotInternalSettings(id);
		checkIfAuthorizedToUpdate(id);
		ObjectNode object = getAsNode(id).orElse(Json.object());
		JsonNode value = Json.readNode(body);
		Json.set(object, field, value);
		String source = object.toString();
		IndexResponse response = saveToElastic(id, source);
		return ElasticPayload.saved("/1", response).build();
	}

	@Delete("/:id/:field")
	@Delete("/:id/:field/")
	public Payload delete(String id, String field) {
		checkIfNotInternalSettings(id);
		checkIfAuthorizedToUpdate(id);
		ObjectNode object = getAsNode(id)//
				.orElseThrow(() -> Exceptions.notFound(TYPE, id));
		Json.remove(object, field);
		IndexResponse response = setAsNode(id, object);
		return ElasticPayload.saved("/1", response).build();
	}

	//
	// Internal services
	//

	public <K extends Settings> K getAsObject(Class<K> settingsClass) {
		String id = Settings.id(settingsClass);
		@SuppressWarnings("unchecked")
		K settings = (K) SpaceContext.getSettings(id);
		if (settings != null)
			return settings;

		try {
			String source = loadFromElastic(id);
			settings = Json.toPojo(source, settingsClass);

		} catch (NotFoundException nfe) {
			settings = instantiateDefaultAsObject(settingsClass);
		}

		if (settings != null)
			SpaceContext.setSettings(id, settings);

		return settings;
	}

	public Optional<ObjectNode> getAsNode(String id) {
		try {
			String source = loadFromElastic(id);
			return Optional.of(Json.readObject(source));

		} catch (NotFoundException nfe) {
			return Optional.empty();
		}
	}

	public <K extends Settings> void registerSettings(Class<K> settingsClass) {
		if (registeredSettingsClasses == null)
			registeredSettingsClasses = Maps.newHashMap();

		registeredSettingsClasses.put(Settings.id(settingsClass), settingsClass);
	}

	public <T extends Settings> IndexResponse setAsObject(T settings) {
		return saveToElastic(settings.id(), Json.toString(settings));
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
				: Json.mapper().valueToTree(instantiateDefaultAsObject(settingsClass));
	}

	private <K extends Settings> K instantiateDefaultAsObject(Class<K> settingsClass) {
		if (registeredSettingsClasses.containsKey(Settings.id(settingsClass)))
			return Utils.instantiate(settingsClass);

		throw Exceptions.runtime("settings class [%s] not registered", //
				settingsClass.getSimpleName());
	}

	private Credentials checkIfAuthorizedToRead(String settingsId) {
		Credentials credentials = SpaceContext.credentials();
		if (!credentials.isAtLeastSuperAdmin())
			credentials.checkRoles(getSettingsAcl(settingsId).read());

		return credentials;
	}

	private Credentials checkIfAuthorizedToUpdate(String id) {
		Credentials credentials = SpaceContext.credentials();
		if (!credentials.isAtLeastSuperAdmin())
			credentials.checkRoles(getSettingsAcl(id).update());

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
			Json.toPojo(body, registeredSettingsClasses.get(id));
	}

	private void makeSureIndexIsCreated() {

		Index index = settingsIndex();
		ElasticClient elastic = elastic();

		if (!elastic.exists(index)) {
			Context context = SpaceContext.get().context();
			int shards = context.query().getInteger(SHARDS_PARAM, SHARDS_DEFAULT_PARAM);
			int replicas = context.query().getInteger(REPLICAS_PARAM, REPLICAS_DEFAULT_PARAM);
			boolean async = context.query().getBoolean(ASYNC_PARAM, ASYNC_DEFAULT_PARAM);

			ObjectNode mapping = Json.object(TYPE, Json.object("enabled", false));
			elastic.createIndex(index, mapping.toString(), async, shards, replicas);
		}
	}

	private void checkIfNotInternalSettings(String settingsId) {
		if (settingsId.toLowerCase().startsWith("internal"))
			throw Exceptions.forbidden("internal settings [%s] not updatable", settingsId);
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
