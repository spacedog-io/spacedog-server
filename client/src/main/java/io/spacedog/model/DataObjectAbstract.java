package io.spacedog.model;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.http.SpaceFields;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public abstract class DataObjectAbstract<K> implements DataObject<K>, SpaceFields {

	private String id;
	private String type;
	private long version;
	private Object[] sort;
	private float score;
	private boolean justCreated;

	public DataObjectAbstract() {
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public DataObjectAbstract<K> id(String id) {
		this.id = id;
		return this;
	}

	@Override
	public String type() {
		return type == null ? sourceClass().getSimpleName().toLowerCase() : type;
	}

	@Override
	public DataObjectAbstract<K> type(String type) {
		this.type = type;
		return this;
	}

	@Override
	public long version() {
		return version;
	}

	@Override
	public DataObjectAbstract<K> version(long version) {
		this.version = version;
		return this;
	}

	@Override
	public float score() {
		return score;
	}

	@Override
	public Object[] sort() {
		return sort;
	}

	@Override
	public boolean justCreated() {
		return this.justCreated;
	}

	@Override
	public DataObjectAbstract<K> justCreated(boolean created) {
		this.justCreated = created;
		return this;
	}

	@Override
	public String owner() {
		K source = source();
		if (source instanceof Metadata)
			return ((Metadata) source).owner();
		if (source instanceof ObjectNode) {
			JsonNode node = ((ObjectNode) source).get(OWNER_FIELD);
			return Json.isNull(node) ? null : node.asText();
		}
		return null;
	}

	@Override
	public void owner(String owner) {
		K source = source();
		if (source instanceof Metadata)
			((Metadata) source).owner(owner);
		else if (source instanceof ObjectNode)
			((ObjectNode) source).put(OWNER_FIELD, owner);
		else
			throw Exceptions.unsupportedOperation("class [%s] doesn't implement this operation", //
					source.getClass().getSimpleName());
	}

	@Override
	public String group() {
		K source = source();
		if (source instanceof Metadata)
			return ((Metadata) source).group();
		if (source instanceof ObjectNode) {
			JsonNode node = ((ObjectNode) source).get(GROUP_FIELD);
			return Json.isNull(node) ? null : node.asText();
		}
		return null;
	}

	@Override
	public void group(String group) {
		K source = source();
		if (source instanceof Metadata)
			((Metadata) source).group(group);
		else if (source instanceof ObjectNode)
			((ObjectNode) source).put(GROUP_FIELD, group);
		else
			throw Exceptions.unsupportedOperation("class [%s] doesn't implement this operation", //
					source.getClass().getSimpleName());
	}

	@Override
	public DateTime createdAt() {
		K source = source();
		if (source instanceof Metadata)
			return ((Metadata) source).createdAt();
		if (source instanceof ObjectNode)
			return Json.toPojo(((ObjectNode) source).get(CREATED_AT_FIELD), DateTime.class);
		return null;
	}

	@Override
	public void createdAt(DateTime createdAt) {
		K source = source();
		if (source instanceof Metadata)
			((Metadata) source).createdAt(createdAt);
		else if (source instanceof ObjectNode)
			((ObjectNode) source).put(CREATED_AT_FIELD, createdAt.toString());
		else
			throw Exceptions.unsupportedOperation("class [%s] doesn't implement this operation", //
					source.getClass().getSimpleName());
	}

	@Override
	public DateTime updatedAt() {
		K source = source();
		if (source instanceof Metadata)
			return ((Metadata) source).updatedAt();
		if (source instanceof ObjectNode)
			return Json.toPojo(((ObjectNode) source).get(UPDATED_AT_FIELD), DateTime.class);
		return null;
	}

	@Override
	public void updatedAt(DateTime updatedAt) {
		K source = source();
		if (source instanceof Metadata)
			((Metadata) source).updatedAt(updatedAt);
		else if (source instanceof ObjectNode)
			((ObjectNode) source).put(UPDATED_AT_FIELD, updatedAt.toString());
		else
			throw Exceptions.unsupportedOperation("class [%s] doesn't implement this operation", //
					source.getClass().getSimpleName());
	}

}
