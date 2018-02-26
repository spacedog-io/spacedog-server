package io.spacedog.client.data;

public interface DataObject<K> extends Metadata {

	String id();

	DataObject<K> id(String id);

	String type();

	DataObject<K> type(String type);

	long version();

	DataObject<K> version(long version);

	Class<K> sourceClass();

	K source();

	DataObject<K> source(K source);

	float score();

	Object[] sort();

	boolean justCreated();

	DataObject<K> justCreated(boolean created);
}
