package io.spacedog.client.data;

public final class DataObjects {

	private DataObjects() {
	}

	public static <T, K> DataObject<K> copyIdentity(//
			DataObject<T> from, DataObject<K> into) {

		into.type(from.type());
		into.id(from.id());
		into.version(from.version());
		return into;
	}
}