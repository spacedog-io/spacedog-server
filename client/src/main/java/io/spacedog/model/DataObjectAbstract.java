package io.spacedog.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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

	public <T> DataObject<T> copyIdentity(DataObject<T> copy) {
		copy.type(type);
		copy.id(id);
		copy.version(version);
		return copy;
	}
}
