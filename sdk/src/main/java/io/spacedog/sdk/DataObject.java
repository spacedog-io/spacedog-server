package io.spacedog.sdk;

import java.io.IOException;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import io.spacedog.rest.SpaceRequest;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json7;

//ignore unknown fields
@JsonIgnoreProperties(ignoreUnknown = true)
// only map to fields
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class DataObject<K extends DataObject<K>> implements Datable<K> {

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
		Object[] sort;
		float score;
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

	public SpaceDog session() {
		return dog;
	}

	@SuppressWarnings("unchecked")
	public K session(SpaceDog session) {
		this.dog = session;
		return (K) this;
	}

	@Override
	public String id() {
		return meta.id;
	}

	@Override
	@SuppressWarnings("unchecked")
	public K id(String id) {
		this.meta.id = id;
		return (K) this;
	}

	@Override
	public String type() {
		return meta.type == null ? type(this.getClass()) : meta.type;
	}

	@SuppressWarnings("unchecked")
	public K type(String type) {
		this.meta.type = type;
		return (K) this;
	}

	@Override
	public long version() {
		return meta.version;
	}

	@Override
	@SuppressWarnings("unchecked")
	public K version(long version) {
		this.meta.version = version;
		return (K) this;
	}

	public String createdBy() {
		return meta.createdBy;
	}

	@SuppressWarnings("unchecked")
	public K createdBy(String createdBy) {
		this.meta.createdBy = createdBy;
		return (K) this;
	}

	public DateTime createdAt() {
		return meta.createdAt;
	}

	@SuppressWarnings("unchecked")
	public K createdAt(DateTime createdAt) {
		this.meta.createdAt = createdAt;
		return (K) this;
	}

	public String updatedBy() {
		return meta.updatedBy;
	}

	@SuppressWarnings("unchecked")
	public K updatedBy(String updatedBy) {
		this.meta.updatedBy = updatedBy;
		return (K) this;
	}

	public DateTime updatedAt() {
		return meta.updatedAt;
	}

	@SuppressWarnings("unchecked")
	public K updatedAt(DateTime updatedAt) {
		this.meta.updatedAt = updatedAt;
		return (K) this;
	}

	public float score() {
		return meta.score;
	}

	public Object[] sort() {
		return meta.sort;
	}

	public ObjectNode node() {
		return node;
	}

	@SuppressWarnings("unchecked")
	public K node(ObjectNode node) {
		this.node = node;
		return (K) this;
	}

	@SuppressWarnings("unchecked")
	public K node(Object... elements) {
		this.node = Json7.object(elements);
		return (K) this;
	}

	public static String type(Class<?> dataClass) {
		return dataClass.getSimpleName().toLowerCase();
	}

	//
	// CRUD methods
	//

	@SuppressWarnings("unchecked")
	public K fetch() {
		node = dog.get("/1/data/{type}/{id}")//
				.routeParam("type", type())//
				.routeParam("id", id())//
				.go(200).asJsonObject();

		if (isSubClass()) {
			try {
				Json7.mapper().readerForUpdating(this).readValue(node);
			} catch (IOException e) {
				throw Exceptions.runtime(e);
			}
		}

		return (K) this;
	}

	boolean isSubClass() {
		return !getClass().equals(DataObject.class);
	}

	@SuppressWarnings("unchecked")
	public K create() {
		SpaceRequest request = dog.post("/1/data/{type}")//
				.routeParam("type", type());

		if (!Strings.isNullOrEmpty(id()))
			request.queryParam("id", id());

		if (isSubClass())
			request.bodyPojo(this);
		else
			request.bodyJson(node);

		ObjectNode result = request.go(201).asJsonObject();
		this.id(result.get("id").asText());
		this.version(result.get("version").asLong());
		return (K) this;
	}

	@SuppressWarnings("unchecked")
	public K save() {
		SpaceRequest request = id() == null //
				? dog.post("/1/data/{type}")//
				: dog.put("/1/data/{type}/" + id());

		request.routeParam("type", type());

		if (isSubClass())
			request.bodyPojo(this);
		else
			request.bodyJson(node);

		ObjectNode result = request.go(200, 201).asJsonObject();
		this.id(result.get("id").asText());
		this.version(result.get("version").asLong());
		return (K) this;
	}

	public void delete() {
		dog.delete("/1/data/{type}/{id}")//
				.routeParam("type", meta.type)//
				.routeParam("id", meta.id)//
				.go(200);
	}

}
