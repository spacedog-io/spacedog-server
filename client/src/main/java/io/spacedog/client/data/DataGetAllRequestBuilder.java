package io.spacedog.client.data;

import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class DataGetAllRequestBuilder {

	private DataGetAllRequest request = new DataGetAllRequest();

	public DataGetAllRequestBuilder type(String type) {
		this.request.type = type;
		return this;
	}

	public DataGetAllRequestBuilder q(String q) {
		this.request.q = q;
		return this;
	}

	public DataGetAllRequestBuilder from(int from) {
		this.request.from = from;
		return this;
	}

	public DataGetAllRequestBuilder size(int size) {
		this.request.size = size;
		return this;
	}

	public DataGetAllRequestBuilder refresh() {
		this.request.refresh = true;
		return this;
	}

	public DataGetAllRequest build() {
		return request;
	}

	public DataResults<ObjectNode> go() {
		return go(ObjectNode.class);
	}

	public abstract <K> DataResults<K> go(Class<K> sourceClass);

}