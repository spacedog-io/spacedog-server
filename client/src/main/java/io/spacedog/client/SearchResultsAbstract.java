package io.spacedog.client;

import com.fasterxml.jackson.databind.JsonNode;

public abstract class SearchResultsAbstract<K> implements SearchResults<K> {

	private long total;
	private JsonNode aggregations;

	public long total() {
		return total;
	}

	public SearchResults<K> total(long total) {
		this.total = total;
		return this;
	}

	public JsonNode aggregations() {
		return aggregations;
	}

	public SearchResults<K> aggregations(JsonNode aggregations) {
		this.aggregations = aggregations;
		return this;
	}
}