package io.spacedog.client.data;

public interface DataWrap<K> extends DataObject {

	String id();

	DataWrap<K> id(String id);

	String type();

	DataWrap<K> type(String type);

	long version();

	DataWrap<K> version(long version);

	Class<K> sourceClass();

	K source();

	DataWrap<K> source(K source);

	float score();

	Object[] sort();

	boolean justCreated();

	DataWrap<K> justCreated(boolean created);
}
