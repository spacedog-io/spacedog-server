package io.spacedog.sdk;

import com.fasterxml.jackson.databind.JsonNode;

import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Json;
import io.spacedog.utils.Settings;

public class SpaceSettings {

	SpaceDog dog;

	SpaceSettings(SpaceDog session) {
		this.dog = session;
	}

	public <K extends Settings> K get(Class<K> settingsClass) {
		return get(Settings.id(settingsClass), settingsClass);
	}

	public <K> K get(String id, Class<K> settingsClass) {
		return SpaceRequest.get("/1/settings/{id}")//
				.routeParam("id", id).auth(dog).go(200).toObject(settingsClass);
	}

	public <K extends Settings> void save(K settings) {
		save(settings.id(), settings);
	}

	public void save(String id, Object settings) {
		JsonNode node = Json.mapper().valueToTree(settings);
		SpaceRequest.put("/1/settings/{id}")//
				.routeParam("id", id).auth(dog).bodyPojo(node).go(200, 201);
	}

	public <K extends Settings> void delete(Class<K> settingsClass) {
		delete(Settings.id(settingsClass));
	}

	public void delete(String id) {
		SpaceRequest.delete("/1/settings/{id}").routeParam("id", id).auth(dog).go(200);
	}
}
