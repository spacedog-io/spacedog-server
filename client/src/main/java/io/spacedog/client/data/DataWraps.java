package io.spacedog.client.data;

public final class DataWraps {

	private DataWraps() {
	}

	public static <T, K> DataWrap<K> copyIdentity(//
			DataWrap<T> from, DataWrap<K> into) {

		into.type(from.type());
		into.id(from.id());
		into.version(from.version());
		return into;
	}
}