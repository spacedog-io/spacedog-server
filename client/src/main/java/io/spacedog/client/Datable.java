package io.spacedog.client;

public interface Datable<K> {

	String type();

	String id();

	K id(String id);

	long version();

	K version(long version);
}
