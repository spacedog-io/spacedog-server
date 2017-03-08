package io.spacedog.sdk;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.model.Settings;

public class SpaceSettings {

	SpaceDog dog;

	SpaceSettings(SpaceDog session) {
		this.dog = session;
	}

	public <K extends Settings> K get(Class<K> settingsClass) {
		return get(Settings.id(settingsClass), settingsClass);
	}

	public ObjectNode get(String id) {
		return this.get(id, ObjectNode.class);
	}

	public <K> K get(String id, Class<K> settingsClass) {
		return dog.get("/1/settings/{id}")//
				.routeParam("id", id).go(200).toObject(settingsClass);
	}

	public <K extends Settings> void save(K settings) {
		save(settings.id(), settings);
	}

	public void save(String id, Object settings) {
		dog.put("/1/settings/{id}")//
				.routeParam("id", id).bodyPojo(settings).go(200, 201);
	}

	public void save(String id, ObjectNode settings) {
		dog.put("/1/settings/{id}")//
				.routeParam("id", id).body(settings).go(200, 201);
	}

	public void save(String id, String settings) {
		dog.put("/1/settings/{id}")//
				.routeParam("id", id).body(settings).go(200, 201);
	}

	public <K extends Settings> void delete(Class<K> settingsClass) {
		delete(Settings.id(settingsClass));
	}

	public void delete(String id) {
		dog.delete("/1/settings/{id}").routeParam("id", id).go(200);
	}
}
