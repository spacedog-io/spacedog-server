package io.spacedog.client.data;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.http.SpaceFields;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public abstract class DataWrapAbstract<K> implements DataWrap<K>, SpaceFields {

	private String id;
	private String type;
	private long version;
	private Object[] sort;
	private float score;
	private boolean justCreated;

	public DataWrapAbstract() {
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public DataWrapAbstract<K> id(String id) {
		this.id = id;
		return this;
	}

	@Override
	public String type() {
		return type == null ? sourceClass().getSimpleName().toLowerCase() : type;
	}

	@Override
	public DataWrapAbstract<K> type(String type) {
		this.type = type;
		return this;
	}

	@Override
	public long version() {
		return version;
	}

	@Override
	public DataWrapAbstract<K> version(long version) {
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
	public DataWrapAbstract<K> justCreated(boolean created) {
		this.justCreated = created;
		return this;
	}

	@Override
	public String owner() {
		K source = source();
		if (source instanceof DataObject)
			return ((DataObject) source).owner();
		if (source instanceof ObjectNode) {
			JsonNode node = ((ObjectNode) source).get(OWNER_FIELD);
			return Json.isNull(node) ? null : node.asText();
		}
		return null;
	}

	@Override
	public void owner(String owner) {
		K source = source();
		if (source instanceof DataObject)
			((DataObject) source).owner(owner);
		else if (source instanceof ObjectNode)
			((ObjectNode) source).put(OWNER_FIELD, owner);
		else
			throw Exceptions.unsupportedOperation("class [%s] doesn't implement this operation", //
					source.getClass().getSimpleName());
	}

	@Override
	public String group() {
		K source = source();
		if (source instanceof DataObject)
			return ((DataObject) source).group();
		if (source instanceof ObjectNode) {
			JsonNode node = ((ObjectNode) source).get(GROUP_FIELD);
			return Json.isNull(node) ? null : node.asText();
		}
		return null;
	}

	@Override
	public void group(String group) {
		K source = source();
		if (source instanceof DataObject)
			((DataObject) source).group(group);
		else if (source instanceof ObjectNode)
			((ObjectNode) source).put(GROUP_FIELD, group);
		else
			throw Exceptions.unsupportedOperation("class [%s] doesn't implement this operation", //
					source.getClass().getSimpleName());
	}

	@Override
	public DateTime createdAt() {
		K source = source();
		if (source instanceof DataObject)
			return ((DataObject) source).createdAt();
		if (source instanceof ObjectNode)
			return Json.toPojo(((ObjectNode) source).get(CREATED_AT_FIELD), DateTime.class);
		return null;
	}

	@Override
	public void createdAt(DateTime createdAt) {
		K source = source();
		if (source instanceof DataObject)
			((DataObject) source).createdAt(createdAt);
		else if (source instanceof ObjectNode)
			((ObjectNode) source).put(CREATED_AT_FIELD, createdAt.toString());
		else
			throw Exceptions.unsupportedOperation("class [%s] doesn't implement this operation", //
					source.getClass().getSimpleName());
	}

	@Override
	public DateTime updatedAt() {
		K source = source();
		if (source instanceof DataObject)
			return ((DataObject) source).updatedAt();
		if (source instanceof ObjectNode)
			return Json.toPojo(((ObjectNode) source).get(UPDATED_AT_FIELD), DateTime.class);
		return null;
	}

	@Override
	public void updatedAt(DateTime updatedAt) {
		K source = source();
		if (source instanceof DataObject)
			((DataObject) source).updatedAt(updatedAt);
		else if (source instanceof ObjectNode)
			((ObjectNode) source).put(UPDATED_AT_FIELD, updatedAt.toString());
		else
			throw Exceptions.unsupportedOperation("class [%s] doesn't implement this operation", //
					source.getClass().getSimpleName());
	}

}
