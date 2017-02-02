package io.spacedog.sdk;

import java.io.IOException;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;

//ignore unknown fields
@JsonIgnoreProperties(ignoreUnknown = true)
// only map to fields
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
getterVisibility = Visibility.NONE, //
isGetterVisibility = Visibility.NONE, //
setterVisibility = Visibility.NONE)
public class DataObject {

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonAutoDetect(fieldVisibility = Visibility.ANY)
	public static class Meta {
		String id;
		String type;
		long version;
		String createdBy;
		DateTime createdAt;
		String updatedBy;
		DateTime updatedAt;
	}

	Meta meta;
	@JsonIgnore
	SpaceDog dog;
	@JsonIgnore
	ObjectNode node;

	protected DataObject() {
		this.meta = new Meta();
	}

	DataObject(SpaceDog session, String type) {
		this();
		this.dog = session;
		this.meta.type = type;
	}

	DataObject(SpaceDog session, String type, String id) {
		this(session, type);
		this.meta.id = id;
	}

	public String id() {
		return meta.id;
	}

	public DataObject id(String id) {
		this.meta.id = id;
		return this;
	}

	public String type() {
		return meta.type == null ? DataObject.type(this.getClass()) : meta.type;
	}

	public DataObject type(String type) {
		this.meta.type = type;
		return this;
	}

	public long version() {
		return meta.version;
	}

	public DataObject version(long version) {
		this.meta.version = version;
		return this;
	}

	public String createdBy() {
		return meta.createdBy;
	}

	public DataObject createdBy(String createdBy) {
		this.meta.createdBy = createdBy;
		return this;
	}

	public DateTime createdAt() {
		return meta.createdAt;
	}

	public DataObject createdAt(DateTime createdAt) {
		this.meta.createdAt = createdAt;
		return this;
	}

	public String updatedBy() {
		return meta.updatedBy;
	}

	public DataObject updatedBy(String updatedBy) {
		this.meta.updatedBy = updatedBy;
		return this;
	}

	public DateTime updatesdAt() {
		return meta.updatedAt;
	}

	public DataObject updatedAt(DateTime updatedAt) {
		this.meta.updatedAt = updatedAt;
		return this;
	}

	public ObjectNode node() {
		return node;
	}

	public DataObject node(ObjectNode node) {
		this.node = node;
		return this;
	}

	public DataObject node(Object... elements) {
		this.node = Json.object(elements);
		return this;
	}

	public static <K extends DataObject> String type(Class<K> dataClass) {
		return dataClass.getSimpleName().toLowerCase();
	}

	//
	// CRUD methods
	//

	public void fetch() {
		node = SpaceRequest.get("/1/data/{type}/{id}")//
				.auth(dog)//
				.routeParam("type", type())//
				.routeParam("id", id())//
				.go(200).objectNode();

		if (isSubClass()) {
			try {
				Json.mapper().readerForUpdating(this).readValue(node);
			} catch (IOException e) {
				throw Exceptions.runtime(e);
			}
		}
	}

	boolean isSubClass() {
		return !getClass().equals(DataObject.class);
	}

	public DataObject save() {
		SpaceRequest request = id() == null //
				? SpaceRequest.post("/1/data/{type}")//
				: SpaceRequest.put("/1/data/{type}/" + id());

		request.auth(dog).routeParam("type", type());

		if (isSubClass())
			request.bodyPojo(this);
		else
			request.body(node);

		ObjectNode result = request.go(200, 201).objectNode();
		this.id(result.get("id").asText());
		this.version(result.get("version").asLong());
		return this;
	}

	public void delete() {
		SpaceRequest.delete("/1/data/{type}/{id}")//
				.auth(dog)//
				.routeParam("type", meta.type)//
				.routeParam("id", meta.id)//
				.go(200);
	}

}
