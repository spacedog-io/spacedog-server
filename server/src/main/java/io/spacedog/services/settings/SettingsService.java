package io.spacedog.services.settings;

import java.util.Map;
import java.util.Optional;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;

import io.spacedog.client.admin.BackendSettings;
import io.spacedog.client.credentials.CredentialsSettings;
import io.spacedog.client.data.DataSettings;
import io.spacedog.client.email.EmailSettings;
import io.spacedog.client.file.WebSettings;
import io.spacedog.client.push.PushSettings;
import io.spacedog.client.schema.Schema;
import io.spacedog.client.schema.SchemaBuilder;
import io.spacedog.client.settings.Settings;
import io.spacedog.client.settings.SettingsAclSettings;
import io.spacedog.client.settings.SettingsBase;
import io.spacedog.client.sms.SmsSettings;
import io.spacedog.client.stripe.StripeSettings;
import io.spacedog.services.Server;
import io.spacedog.services.SpaceService;
import io.spacedog.services.db.elastic.ElasticClient;
import io.spacedog.services.db.elastic.ElasticIndex;
import io.spacedog.services.db.elastic.ElasticVersion;
import io.spacedog.services.file.InternalFileSettings;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;

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
		register(BackendSettings.class);
	}

	public <K extends Settings> void register(Class<K> settingsClass) {
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

		SearchSourceBuilder source = SearchSourceBuilder.searchSource()//
				.query(QueryBuilders.matchAllQuery())//
				.from(from)//
				.size(size);

		SearchResponse response = elastic().search(source, index());

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
		return get(id).orElseThrow(() -> Exceptions.objectNotFound("settings", id));
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

	public <K extends Settings> K getOrThrow(Class<K> settingsClass) {
		return get(settingsClass).orElseThrow(//
				() -> Exceptions.objectNotFound("settings", SettingsBase.id(settingsClass)));
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

	public <K extends Settings> String save(K settings) {
		return save(settings.id(), settings);
	}

	public <K> String save(String id, K settings) {
		String version = doSave(id, Json.toString(settings));
		Server.context().setSettings(id, settings);
		return version;
	}

	public String save(String id, ObjectNode settings) {
		checkSettingsAreValid(id, settings);
		String version = doSave(id, settings.toString());
		Server.context().setSettings(id, settings);
		return version;
	}

	private String doSave(String id, String settings) {
		makeSureIndexIsCreated();
		IndexResponse response = elastic().index(index(), id, settings);
		return ElasticVersion.toString(response.getSeqNo(), response.getPrimaryTerm());
	}

	// save field

	public String save(Class<? extends Settings> settingsClass, String field, Object value) {
		return save(SettingsBase.id(settingsClass), field, value);
	}

	public String save(String id, String field, Object value) {
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

	public <K extends Settings> String delete(Class<K> settingsClass, String field) {
		return delete(SettingsBase.id(settingsClass), field);
	}

	public String delete(String id, String field) {
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

	public ElasticIndex index() {
		return new ElasticIndex(SERVICE_NAME);
	}

	private void makeSureIndexIsCreated() {

		ElasticIndex index = index();
		ElasticClient elastic = elastic();

		if (!elastic.exists(index)) {
			Schema schema = SchemaBuilder.builder(SERVICE_NAME).enabled(false).build();
			elastic.createIndex(index, schema, false);
		}
	}

	private static class SettingsSource {
		private String string;
		private String version;
	}

	private Optional<SettingsSource> doGet(String id) {
		if (elastic().exists(index())) {
			GetResponse response = elastic().get(index(), id);

			if (response.isExists()) {
				SettingsSource source = new SettingsSource();
				source.string = response.getSourceAsString();
				source.version = ElasticVersion.toString(response.getSeqNo(), response.getPrimaryTerm());
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
