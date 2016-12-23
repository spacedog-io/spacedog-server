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
	}

	DataObject(SpaceDog session, String type) {
		this.dog = session;
		this.meta = new Meta();
		this.meta.type = type;
	}

	public DataObject(SpaceDog session, String type, String id) {
		this(session, type);
		this.meta.id = id;
	}

	public String id() {
		return meta.id;
	}

	public void id(String id) {
		this.meta.id = id;
	}

	public String type() {
		return meta.type == null ? DataObject.type(this.getClass()) : meta.type;
	}

	public void type(String type) {
		this.meta.type = type;
	}

	public long version() {
		return meta.version;
	}

	public void version(long version) {
		this.meta.version = version;
	}

	public String createdBy() {
		return meta.createdBy;
	}

	public void createdBy(String createdBy) {
		this.meta.createdBy = createdBy;
	}

	public DateTime createdAt() {
		return meta.createdAt;
	}

	public void createdAt(DateTime createdAt) {
		this.meta.createdAt = createdAt;
	}

	public String updatedBy() {
		return meta.updatedBy;
	}

	public void updatedBy(String updatedBy) {
		this.meta.updatedBy = updatedBy;
	}

	public DateTime updatesdAt() {
		return meta.updatedAt;
	}

	public void updatedAt(DateTime updatedAt) {
		this.meta.updatedAt = updatedAt;
	}

	public static <K extends DataObject> String type(Class<K> dataClass) {
		return dataClass.getSimpleName().toLowerCase();
	}

	//
	// CRUD methods
	//

	public void fetch() {
		node = SpaceRequest.get("/1/data/{type}/{id}")//
				.bearerAuth(dog.backendId, dog.accessToken)//
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

	public void save() {
		SpaceRequest request = id() == null //
				? SpaceRequest.post("/1/data/{type}")//
				: SpaceRequest.put("/1/data/{type}/" + id());

		request.bearerAuth(dog.backendId, dog.accessToken)//
				.routeParam("type", type());

		if (isSubClass())
			request.bodyPojo(this);
		else
			request.body(node);

		ObjectNode result = request.go(200, 201).objectNode();
		this.id(result.get("id").asText());
		this.version(result.get("version").asLong());
	}

	public void delete() {
		SpaceRequest.delete("/1/data/{type}/{id}")//
				.bearerAuth(dog.backendId, dog.accessToken)//
				.routeParam("type", meta.type)//
				.routeParam("id", meta.id)//
				.go(200);
	}

}
