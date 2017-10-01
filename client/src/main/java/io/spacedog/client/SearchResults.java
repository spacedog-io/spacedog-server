package io.spacedog.client;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

public interface SearchResults<K> {

	public long total();

	public SearchResults<K> total(long total);

	public List<K> results();

	public SearchResults<K> results(List<K> results);

	public JsonNode aggregations();

	public SearchResults<K> aggregations(JsonNode aggregations);
}