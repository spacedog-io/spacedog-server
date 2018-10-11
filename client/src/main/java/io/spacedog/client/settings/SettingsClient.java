package io.spacedog.client.settings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.http.SpaceResponse;

public class SettingsClient {

	private SpaceDog dog;

	public SettingsClient(SpaceDog session) {
		this.dog = session;
	}

	public <K extends Settings> boolean exists(Class<K> settingsClass) {
		return exists(SettingsBase.id(settingsClass));
	}

	public boolean exists(String id) {
		SpaceResponse response = dog.get("/2/settings/{id}")//
				.routeParam("id", id).go(200, 404);

		return response.status() == 404;
	}

	// get all

	public ObjectNode getAll() {
		return getAll(false);
	}

	public ObjectNode getAll(boolean refresh) {
		return dog.get("/2/settings")//
				.refresh(refresh).go(200).asJsonObject();
	}

	public ObjectNode getAll(int from, int size, boolean refresh) {
		return dog.get("/2/settings").from(from).size(size)//
				.refresh(refresh).go(200).asJsonObject();
	}

	// get

	public ObjectNode get(String id) {
		return this.doGet(id).asJsonObject();
	}

	public <K extends Settings> K get(Class<K> settingsClass) {
		return get(SettingsBase.id(settingsClass), settingsClass);
	}

	public <K> K get(String id, Class<K> settingsClass) {
		return doGet(id).asPojo(settingsClass);
	}

	private SpaceResponse doGet(String id) {
		return dog.get("/2/settings/{id}")//
				.routeParam("id", id).go(200);
	}

	// get field

	public JsonNode get(Class<? extends Settings> settingsClass, String field) {
		return get(SettingsBase.id(settingsClass), field);
	}

	public JsonNode get(String id, String field) {
		return this.doGet(id, field).asJson();
	}

	public <K> K get(Class<? extends Settings> settingsClass, String field, Class<K> fieldClass) {
		return get(SettingsBase.id(settingsClass), field, fieldClass);
	}

	public <K> K get(String id, String field, Class<K> fieldClass) {
		return doGet(id, field).asPojo(fieldClass);
	}

	private SpaceResponse doGet(String id, String field) {
		return dog.get("/2/settings/{id}/{field}")//
				.routeParam("id", id).routeParam("field", field)//
				.go(200);
	}

	// save settings

	public <K extends Settings> long save(K settings) {
		return save(settings.id(), settings);
	}

	public long save(String id, Object settings) {
		return dog.put("/2/settings/{id}").routeParam("id", id)//
				.bodyPojo(settings).go(200, 201)//
				.get("version").asLong();
	}

	public long save(String id, String settings) {
		return dog.put("/2/settings/{id}").routeParam("id", id)//
				.body(settings).go(200, 201)//
				.get("version").asLong();
	}

	// save field

	public long save(Class<? extends Settings> settingsClass, String field, Object value) {
		return save(SettingsBase.id(settingsClass), field, value);
	}

	public long save(String id, String field, Object value) {
		return dog.put("/2/settings/{id}/{field}")//
				.routeParam("id", id).routeParam("field", field)//
				.bodyPojo(value).go(200, 201)//
				.get("version").asLong();
	}

	//
	// Delete
	//

	public void deleteAll() {
		dog.delete("/2/settings").go(200);
	}

	public <K extends Settings> void delete(Class<K> settingsClass) {
		delete(SettingsBase.id(settingsClass));
	}

	public void delete(String id) {
		dog.delete("/2/settings/{id}").routeParam("id", id).go(200, 404);
	}

	public <K extends Settings> long delete(Class<K> settingsClass, String field) {
		return delete(SettingsBase.id(settingsClass), field);
	}

	public long delete(String id, String field) {
		return dog.delete("/2/settings/{id}/{field}")//
				.routeParam("id", id).routeParam("field", field)//
				.go(200).get("version").asLong();
	}

}
