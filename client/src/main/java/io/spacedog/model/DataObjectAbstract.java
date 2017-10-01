package io.spacedog.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.node.ObjectNode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public abstract class DataObjectAbstract<K> implements DataObject<K> {

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

	public Meta meta() {
		K source = source();
		if (source instanceof Metadata)
			return ((Metadata) source).meta();
		if (source instanceof ObjectNode)
			return new ObjectNodeMeta((ObjectNode) source);
		return new Meta();
	}

	public void meta(Meta meta) {
		K source = source();
		if (source instanceof Metadata)
			((Metadata) source).meta(meta);
		if (source instanceof ObjectNode) {
			ObjectNodeMeta objectNodeMeta = new ObjectNodeMeta((ObjectNode) source);
			objectNodeMeta.createdAt(meta.createdAt());
			objectNodeMeta.createdBy(meta.createdBy());
			objectNodeMeta.updatedAt(meta.updatedAt());
			objectNodeMeta.createdBy(meta.createdBy());
		}
	}

}
