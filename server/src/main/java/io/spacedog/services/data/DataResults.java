package io.spacedog.services.data;

import java.util.List;
import java.util.Map;

import org.elasticsearch.search.aggregations.Aggregation;

import io.spacedog.client.data.DataWrap;

public class DataResults<K> {
	public long total;
	public List<DataWrap<K>> objects;
	public String next;
	public Map<String, Aggregation> aggregations;
	public Class<K> sourceClass;

	private DataResults() {
	}

	public static <K> DataResults<K> of(Class<K> sourceClass) {
		DataResults<K> results = new DataResults<K>();
		results.sourceClass = sourceClass;
		return results;
	}
}
