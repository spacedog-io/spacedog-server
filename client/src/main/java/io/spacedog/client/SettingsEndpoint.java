package io.spacedog.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.http.SpaceResponse;
import io.spacedog.model.Settings;
import io.spacedog.model.SettingsBase;
import io.spacedog.utils.Optional7;

public class SettingsEndpoint {

	SpaceDog dog;

	SettingsEndpoint(SpaceDog session) {
		this.dog = session;
	}

	public <K extends Settings> Optional7<K> exists(Class<K> settingsClass) {
		return exists(SettingsBase.id(settingsClass), settingsClass);
	}

	public <K> Optional7<K> exists(String id, Class<K> settingsClass) {
		SpaceResponse response = dog.get("/1/settings/{id}")//
				.routeParam("id", id).go(200, 404);

		if (response.status() == 404)
			return Optional7.empty();

		return Optional7.of(response.asPojo(settingsClass));
	}

	// get settings

	public ObjectNode get(String id) {
		return this.doGet(id).asJsonObject();
	}

	public <K extends Settings> K get(Class<K> settingsClass) {
		return get(SettingsBase.id(settingsClass), settingsClass);
	}

	public <K> K get(String id, Class<K> settingsClass) {
		return doGet(id).asPojo(settingsClass);
	}

	public SpaceResponse doGet(String id) {
		return dog.get("/1/settings/{id}")//
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
		return dog.get("/1/settings/{id}/{field}")//
				.routeParam("id", id).routeParam("field", field)//
				.go(200);
	}

	// save settings

	public <K extends Settings> void save(K settings) {
		save(settings.id(), settings);
	}

	public void save(String id, Object settings) {
		dog.put("/1/settings/{id}")//
				.routeParam("id", id).bodyPojo(settings).go(200, 201);
	}

	public void save(String id, ObjectNode settings) {
		dog.put("/1/settings/{id}")//
				.routeParam("id", id).bodyJson(settings).go(200, 201);
	}

	public void save(String id, String settings) {
		dog.put("/1/settings/{id}")//
				.routeParam("id", id).bodyString(settings).go(200, 201);
	}

	// save field

	public void save(Class<? extends Settings> settingsClass, String field, JsonNode value) {
		save(SettingsBase.id(settingsClass), field, value);
	}

	public void save(String id, String field, JsonNode value) {
		dog.put("/1/settings/{id}/{field}")//
				.routeParam("id", id).routeParam("field", field)//
				.bodyJson(value).go(200, 201);
	}

	public void save(Class<? extends Settings> settingsClass, String field, Object value) {
		save(SettingsBase.id(settingsClass), field, value);
	}

	public void save(String id, String field, Object value) {
		dog.put("/1/settings/{id}/{field}")//
				.routeParam("id", id).routeParam("field", field)//
				.bodyPojo(value).go(200, 201);
	}

	// delete settings

	public <K extends Settings> void delete(Class<K> settingsClass) {
		delete(SettingsBase.id(settingsClass));
	}

	public void delete(String id) {
		dog.delete("/1/settings/{id}").routeParam("id", id).go(200, 404);
	}

	// delete field

	public <K extends Settings> void delete(Class<K> settingsClass, String field) {
		delete(SettingsBase.id(settingsClass), field);
	}

	public void delete(String id, String field) {
		dog.delete("/1/settings/{id}/{field}")//
				.routeParam("id", id).routeParam("field", field).go(200);
	}

}
