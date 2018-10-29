package io.spacedog.client.data;

import java.util.Objects;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.http.SpaceFields;
import io.spacedog.utils.Json;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.NONE, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class DataWrap<K> implements DataObject, SpaceFields {

	public static final long MATCH_ANY_VERSIONS = -3L;

	private K source;
	@JsonIgnore
	private Class<K> sourceClass;
	private String id;
	private String type;
	private long version = MATCH_ANY_VERSIONS;
	private Object[] sort;
	private float score;

	public DataWrap() {
	}

	public static <K> DataWrap<K> wrap(K source) {
		return new DataWrap<K>().source(source);
	}

	public static <K> DataWrap<K> wrap(Class<K> sourceClass) {
		return new DataWrap<K>().sourceClass(sourceClass);
	}

	@SuppressWarnings("unchecked")
	public Class<K> sourceClass() {
		if (sourceClass == null)
			if (source != null)
				return (Class<K>) source.getClass();

		return sourceClass;
	}

	public DataWrap<K> sourceClass(Class<K> sourceClass) {
		this.sourceClass = sourceClass;
		return this;
	}

	@JsonProperty
	public K source() {
		return source;
	}

	public DataWrap<K> source(K source) {
		this.source = source;
		return this;
	}

	@JsonProperty
	public String id() {
		return id;
	}

	public DataWrap<K> id(String id) {
		this.id = id;
		return this;
	}

	@JsonProperty
	public String type() {
		if (type == null) {
			Class<K> sourceClass = sourceClass();
			return sourceClass() == null ? null //
					: sourceClass.getSimpleName().toLowerCase();
		}
		return type;
	}

	public DataWrap<K> type(String type) {
		this.type = type;
		return this;
	}

	@JsonProperty
	public long version() {
		return version;
	}

	public DataWrap<K> version(long version) {
		this.version = version;
		return this;
	}

	@JsonProperty
	public float score() {
		return score;
	}

	public DataWrap<K> score(float score) {
		this.score = score;
		return this;
	}

	@JsonProperty
	public Object[] sort() {
		return sort;
	}

	public DataWrap<K> sort(Object[] sort) {
		this.sort = sort;
		return this;
	}

	public boolean isCreated() {
		DateTime createdAt = createdAt();
		DateTime updatedAt = updatedAt();
		return createdAt != null //
				&& updatedAt != null //
				&& createdAt.equals(updatedAt);
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
			throw unsupportedOpperation();
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
			throw unsupportedOpperation();
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
			throw unsupportedOpperation();
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
			throw unsupportedOpperation();
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean equals(Object obj) {
		if (obj instanceof DataWrap == false)
			return false;
		DataWrap<K> wrap = (DataWrap<K>) obj;
		return Objects.equals(id(), wrap.id()) //
				&& Objects.equals(source(), wrap.source()) //
				&& Objects.equals(sourceClass(), wrap.sourceClass()) //
				&& Objects.equals(type(), wrap.type()) //
				&& version() == wrap.version();
	}

	@Override
	public String toString() {
		return String.format("DataWrap[%s][%s][%s][%s]", //
				type(), id(), version(), source());
	}

	private UnsupportedOperationException unsupportedOpperation() {
		return new UnsupportedOperationException(//
				String.format("class [%s] doesn't implement this operation", //
						sourceClass().getSimpleName()));
	}
}
