package io.spacedog.services;

import java.util.Map;
import java.util.Optional;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;

import io.spacedog.client.credentials.CredentialsSettings;
import io.spacedog.client.data.DataSettings;
import io.spacedog.client.email.EmailSettings;
import io.spacedog.client.file.InternalFileSettings;
import io.spacedog.client.file.WebSettings;
import io.spacedog.client.push.PushSettings;
import io.spacedog.client.settings.Settings;
import io.spacedog.client.settings.SettingsAclSettings;
import io.spacedog.client.settings.SettingsBase;
import io.spacedog.client.sms.SmsSettings;
import io.spacedog.client.stripe.StripeSettings;
import io.spacedog.server.ElasticClient;
import io.spacedog.server.Index;
import io.spacedog.server.Server;
import io.spacedog.server.SpaceService;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;
import net.codestory.http.Request;

public class SettingsService extends SpaceService {

	public SettingsService() {
		registeredSettingsClasses = Maps.newHashMap();

		register(SettingsAclSettings.class);
		register(StripeSettings.class);
		register(CredentialsSettings.class);
		register(PushSettings.class);
		register(WebSettings.class);
		register(InternalFileSettings.class);
		register(DataSettings.class);
		register(EmailSettings.class);
		register(SmsSettings.class);
	}

	private <K extends Settings> void register(Class<K> settingsClass) {
		registeredSettingsClasses.put(SettingsBase.id(settingsClass), settingsClass);
	}

	public <K extends Settings> boolean exists(Class<K> settingsClass) {
		return exists(SettingsBase.id(settingsClass));
	}

	public boolean exists(String id) {
		if (elastic().exists(index())) {
			GetResponse response = elastic().get(index(), id);

			if (response.isExists())
				return true;
		}
		return false;
	}

	// get all

	public ObjectNode getAll() {
		return getAll(false);
	}

	public ObjectNode getAll(boolean refresh) {
		return getAll(0, 10, refresh);
	}

	public ObjectNode getAll(int from, int size, boolean refresh) {

		ObjectNode results = Json.object();
		if (!elastic().exists(index()))
			return results;

		elastic().refreshIndex(refresh, index());

		SearchResponse response = elastic().prepareSearch(index())//
				.setFrom(from)//
				.setSize(size)//
				.setQuery(QueryBuilders.matchAllQuery())//
				.get();

		for (SearchHit hit : response.getHits().getHits())
			results.set(hit.getId(), Json.readNode(hit.getSourceAsString()));

		return results;
	}

	// get

	public Optional<ObjectNode> get(String id) {
		ObjectNode settings = Server.context().getSettings(id).orElse(null);

		if (settings == null) {
			settings = doGet(id)//
					.map(source -> Json.readObject(source.string))//
					.orElse(instantiateDefaultAsNode(id).orElse(null));

			if (settings != null)
				Server.context().setSettings(id, settings);
		}

		return Optional.ofNullable(settings);
	}

	public ObjectNode getOrThrow(String id) {
		return get(id).orElseThrow(() -> Exceptions.notFound("settings", id));
	}

	public <K extends Settings> Optional<K> get(Class<K> settingsClass) {
		K settings = Server.context().getSettings(settingsClass).orElse(null);

		if (settings == null) {
			String id = SettingsBase.id(settingsClass);
			settings = doGet(id)//
					.map(source -> {
						K settings2 = Json.toPojo(source.string, settingsClass);
						settings2.version(source.version);
						return settings2;
					})//
					.orElse(instantiateDefaultAsObject(settingsClass).orElse(null));

			if (settings != null)
				Server.context().setSettings(id, settings);
		}

		return Optional.ofNullable(settings);
	}

	public <K> Optional<K> get(String id, Class<K> settingsClass) {
		return get(id).map(settings -> Json.toPojo(settings, settingsClass));
	}

	// get field

	public Optional<JsonNode> get(Class<? extends Settings> settingsClass, String field) {
		return get(SettingsBase.id(settingsClass), field);
	}

	public Optional<JsonNode> get(String id, String field) {
		return Optional.ofNullable(Json.get(getOrThrow(id), field));
	}

	public <K> Optional<K> get(Class<? extends Settings> settingsClass, String field, Class<K> fieldClass) {
		return get(SettingsBase.id(settingsClass), field, fieldClass);
	}

	public <K> Optional<K> get(String id, String field, Class<K> fieldClass) {
		return get(id, field).map(value -> Json.toPojo(value, fieldClass));
	}

	// save settings

	public <K extends Settings> long save(K settings) {
		return save(settings.id(), settings);
	}

	public <K> long save(String id, K settings) {
		long version = doSave(id, Json.toString(settings));
		Server.context().setSettings(id, settings);
		return version;
	}

	public long save(String id, ObjectNode settings) {
		checkSettingsAreValid(id, settings);
		long version = doSave(id, settings.toString());
		Server.context().setSettings(id, settings);
		return version;
	}

	private long doSave(String id, String settings) {
		makeSureIndexIsCreated();
		return elastic().prepareIndex(index(), id)//
				.setSource(settings, XContentType.JSON)//
				.get()//
				.getVersion();
	}

	// save field

	public long save(Class<? extends Settings> settingsClass, String field, Object value) {
		return save(SettingsBase.id(settingsClass), field, value);
	}

	public long save(String id, String field, Object value) {
		ObjectNode object = get(id).orElse(Json.object());
		Json.set(object, field, value);
		return save(id, object);
	}

	// delete settings

	public <K extends Settings> void delete(Class<K> settingsClass) {
		delete(SettingsBase.id(settingsClass));
	}

	public void delete(String id) {
		elastic().delete(index(), id, false, true);
		Server.context().setSettings(id, null);
	}

	// delete field

	public <K extends Settings> long delete(Class<K> settingsClass, String field) {
		return delete(SettingsBase.id(settingsClass), field);
	}

	public long delete(String id, String field) {
		ObjectNode object = getOrThrow(id);
		Json.remove(object, field);
		return save(id, object);
	}

	//
	//
	//

	public static final String SERVICE_NAME = "settings";

	//
	// Fields
	//

	private static Map<String, Class<? extends Settings>> registeredSettingsClasses;

	//
	//
	//

	public Index index() {
		return new Index(SERVICE_NAME);
	}

	private void makeSureIndexIsCreated() {

		Index index = index();
		ElasticClient elastic = elastic();

		if (!elastic.exists(index)) {
			Request request = Server.context().request();
			ObjectNode mapping = Json.object(SERVICE_NAME, Json.object("enabled", false));

			int shards = request.query().getInteger(SHARDS_PARAM, SHARDS_DEFAULT_PARAM);
			int replicas = request.query().getInteger(REPLICAS_PARAM, REPLICAS_DEFAULT_PARAM);
			boolean async = request.query().getBoolean(ASYNC_PARAM, ASYNC_DEFAULT_PARAM);

			org.elasticsearch.common.settings.Settings settings = //
					org.elasticsearch.common.settings.Settings.builder()//
							.put("number_of_shards", shards)//
							.put("number_of_replicas", replicas)//
							.build();

			elastic.createIndex(index, mapping.toString(), settings, async);
		}
	}

	private static class SettingsSource {
		private String string;
		private long version;
	}

	private Optional<SettingsSource> doGet(String id) {
		if (elastic().exists(index())) {
			GetResponse response = elastic().get(index(), id);

			if (response.isExists()) {
				SettingsSource source = new SettingsSource();
				source.string = response.getSourceAsString();
				source.version = response.getVersion();
				return Optional.of(source);
			}

		}
		return Optional.empty();
	}

	private void checkSettingsAreValid(String id, ObjectNode settings) {
		if (registeredSettingsClasses.containsKey(id))
			Json.toPojo(settings, registeredSettingsClasses.get(id));
	}

	private Optional<ObjectNode> instantiateDefaultAsNode(String id) {
		Class<? extends Settings> settingsClass = registeredSettingsClasses.get(id);
		return settingsClass == null ? Optional.empty() //
				: instantiateDefaultAsObject(settingsClass)//
						.map(object -> Json.toObjectNode(object));
	}

	private <K extends Settings> Optional<K> instantiateDefaultAsObject(Class<K> settingsClass) {
		if (registeredSettingsClasses.containsKey(SettingsBase.id(settingsClass)))
			return Optional.of(Utils.instantiate(settingsClass));
		return Optional.empty();
	}

}
