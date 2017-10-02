package io.spacedog.model;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.utils.Json;
import io.spacedog.utils.SpaceFields;

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

	public String id() {
		return id;
	}

	public DataObjectAbstract<K> id(String id) {
		this.id = id;
		return this;
	}

	public String type() {
		return type == null ? type(this.getClass()) : type;
	}

	public DataObjectAbstract<K> type(String type) {
		this.type = type;
		return this;
	}

	public long version() {
		return version;
	}

	public DataObjectAbstract<K> version(long version) {
		this.version = version;
		return this;
	}

	public float score() {
		return score;
	}

	public Object[] sort() {
		return sort;
	}

	public boolean justCreated() {
		return this.justCreated;
	}

	public DataObjectAbstract<K> justCreated(boolean created) {
		this.justCreated = created;
		return this;
	}

	public static String type(Class<?> dataClass) {
		return dataClass.getSimpleName().toLowerCase();
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
	}

}
