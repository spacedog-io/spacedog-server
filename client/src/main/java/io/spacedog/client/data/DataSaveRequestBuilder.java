package io.spacedog.client.data;

public abstract class DataSaveRequestBuilder<K> {

	private DataWrap<K> wrap;

	public DataSaveRequestBuilder(K source) {
		this.wrap = DataWrap.wrap(source);
	}

	public DataSaveRequestBuilder<K> type(String type) {
		wrap.type(type);
		return this;
	}

	public DataSaveRequestBuilder<K> id(String id) {
		wrap.id(id);
		return this;
	}

	public DataSaveRequestBuilder<K> version(long version) {
		wrap.version(version);
		return this;
	}

	public DataWrap<K> build() {
		return wrap;
	}

	public abstract DataWrap<K> go();

}
