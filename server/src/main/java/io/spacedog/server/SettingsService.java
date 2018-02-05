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

import io.spacedog.model.Credentials;
import io.spacedog.model.Permission;
import io.spacedog.model.RolePermissions;
import io.spacedog.model.Settings;
import io.spacedog.model.SettingsAclSettings;
import io.spacedog.model.SettingsBase;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;

@Prefix("/1/settings")
public class SettingsService extends SpaceService {

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
			return JsonPayload.ok().build();

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
		checkAuthorizedTo(id, Permission.read);
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
		checkNotInternalSettings(id);
		checkAuthorizedTo(id, Permission.update);
		IndexResponse response = doSave(id, body);
		return ElasticPayload.saved("/1", response).build();
	}

	@Delete("/:id")
	@Delete("/:id/")
	public Payload delete(String id) {
		checkNotInternalSettings(id);
		checkAuthorizedTo(id, Permission.update);
		doDelete(id);
		return JsonPayload.ok().build();
	}

	@Get("/:id/:field")
	@Get("/:id/:field/")
	public Payload get(String id, String field) {
		checkAuthorizedTo(id, Permission.read);
		ObjectNode object = getAsNode(id)//
				.orElseThrow(() -> Exceptions.notFound(TYPE, id));
		JsonNode value = Json.get(object, field);
		value = value == null ? NullNode.getInstance() : value;
		return JsonPayload.ok().withObject(value).build();
	}

	@Put("/:id/:field")
	@Put("/:id/:field/")
	public Payload put(String id, String field, String body) {
		checkNotInternalSettings(id);
		checkAuthorizedTo(id, Permission.update);
		ObjectNode object = getAsNode(id).orElse(Json.object());
		JsonNode value = Json.readNode(body);
		Json.set(object, field, value);
		String source = object.toString();
		IndexResponse response = doSave(id, source);
		return ElasticPayload.saved("/1", response).build();
	}

	@Delete("/:id/:field")
	@Delete("/:id/:field/")
	public Payload delete(String id, String field) {
		checkNotInternalSettings(id);
		checkAuthorizedTo(id, Permission.update);
		ObjectNode object = getAsNode(id)//
				.orElseThrow(() -> Exceptions.notFound(TYPE, id));
		Json.remove(object, field);
		IndexResponse response = doSave(id, object.toString());
		return ElasticPayload.saved("/1", response).build();
	}

	//
	// Internal services
	//

	public <K extends Settings> K getAsObject(Class<K> settingsClass) {
		K settings = SpaceContext.getSettings(settingsClass);
		if (settings != null)
			return settings;

		String id = SettingsBase.id(settingsClass);

		if (elastic().exists(settingsIndex())) {
			GetResponse response = elastic().get(settingsIndex(), id);
			if (response.isExists()) {
				String source = response.getSourceAsString();
				settings = Json.toPojo(source, settingsClass);
				settings.version(response.getVersion());
			}
		}

		if (settings == null)
			settings = instantiateDefaultAsObject(settingsClass);

		SpaceContext.setSettings(settings);
		return settings;
	}

	public <K extends Settings> IndexResponse saveAsObject(K settings) {
		makeSureIndexIsCreated();
		return elastic().prepareIndex(settingsIndex(), settings.id())//
				.setSource(Json.toString(settings))//
				.setVersion(settings.version())//
				.get();
	}

	public Optional<ObjectNode> getAsNode(String id) {
		return doGet(id).map(source -> Json.readObject(source));
	}

	public Optional<String> doGet(String id) {
		if (elastic().exists(settingsIndex())) {
			GetResponse response = elastic().get(settingsIndex(), id);

			if (response.isExists())
				return Optional.of(response.getSourceAsString());
		}
		return Optional.empty();
	}

	public IndexResponse doSave(String id, String source) {
		checkSettingsAreValid(id, source);
		makeSureIndexIsCreated();
		return elastic().prepareIndex(settingsIndex(), id)//
				.setSource(source).get();
	}

	public void doDelete(String id) {
		elastic().delete(settingsIndex(), id, false, true);
	}

	public <K extends Settings> void registerSettings(Class<K> settingsClass) {
		if (registeredSettingsClasses == null)
			registeredSettingsClasses = Maps.newHashMap();

		registeredSettingsClasses.put(SettingsBase.id(settingsClass), settingsClass);
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
		if (registeredSettingsClasses.containsKey(SettingsBase.id(settingsClass)))
			return Utils.instantiate(settingsClass);

		throw Exceptions.runtime("settings class [%s] not registered", //
				settingsClass.getSimpleName());
	}

	private Credentials checkAuthorizedTo(String settingsId, Permission permission) {
		Credentials credentials = SpaceContext.credentials();
		getSettingsAcl(settingsId).check(credentials, permission);
		return credentials;
	}

	private RolePermissions getSettingsAcl(String settingsId) {
		return getAsObject(SettingsAclSettings.class).get(settingsId);
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

	private void checkNotInternalSettings(String settingsId) {
		if (settingsId.toLowerCase().startsWith("internal"))
			throw Exceptions.forbidden("direct update of internal settings is forbidden");
	}

	//
	// singleton
	//

	private static SettingsService singleton = new SettingsService();

	public static SettingsService get() {
		return singleton;
	}

	private SettingsService() {
		registerSettings(SettingsAclSettings.class);
	}
}
