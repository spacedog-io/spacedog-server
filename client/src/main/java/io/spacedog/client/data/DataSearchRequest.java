package io.spacedog.client.data;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.elastic.ESSearchSourceBuilder;

public abstract class DataSearchRequest {

	public boolean refresh;
	public String type;
	public String source;

	public DataSearchRequest refresh() {
		this.refresh = true;
		return this;
	}

	public DataSearchRequest type(String type) {
		this.type = type;
		return this;
	}

	public DataSearchRequest source(String source) {
		this.source = source;
		return this;
	}

	public DataSearchRequest source(ESSearchSourceBuilder source) {
		return source(source.toString());
	}

	public DataResults<ObjectNode> go() {
		return go(ObjectNode.class);
	}

	public abstract <K> DataResults<K> go(Class<K> sourceClass);
}