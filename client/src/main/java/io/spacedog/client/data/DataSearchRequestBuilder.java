package io.spacedog.client.data;

import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class DataSearchRequestBuilder {

	private DataSearchRequest request = new DataSearchRequest();

	public DataSearchRequestBuilder refresh() {
		this.request.refresh = true;
		return this;
	}

	public DataSearchRequestBuilder type(String type) {
		this.request.type = type;
		return this;
	}

	public DataSearchRequestBuilder source(String source) {
		this.request.source = source;
		return this;
	}

	public DataSearchRequest build() {
		return request;
	}

	public DataResults<ObjectNode> go() {
		return go(ObjectNode.class);
	}

	public abstract <K> DataResults<K> go(Class<K> sourceClass);
}