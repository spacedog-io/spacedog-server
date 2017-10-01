package io.spacedog.model;

import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.utils.Json;

public class ObjectNodeMeta extends Meta {

	private final ObjectNode object;

	ObjectNodeMeta(ObjectNode objectNode) {
		object = objectNode;
	}

	public String createdBy() {
		return Json.toPojo(object, "meta.createdBy", String.class);
	}

	public Meta createdBy(String createdBy) {
		Json.with(object, "meta.createdBy", createdBy);
		return this;
	}

	public DateTime createdAt() {
		return Json.toPojo(object, "meta.createdAt", DateTime.class);
	}

	public Meta createdAt(DateTime createdAt) {
		Json.with(object, "meta.createdAt", createdAt);
		return this;
	}

	public String updatedBy() {
		return Json.toPojo(object, "meta.updatedBy", String.class);
	}

	public Meta updatedBy(String updatedBy) {
		Json.with(object, "meta.updatedBy", updatedBy);
		return this;
	}

	public DateTime updatedAt() {
		return Json.toPojo(object, "meta.updatedAt", DateTime.class);
	}

	public Meta updatedAt(DateTime updatedAt) {
		Json.with(object, "meta.updatedAt", updatedAt);
		return this;
	}

}