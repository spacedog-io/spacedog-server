package io.spacedog.client.data;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class DataResults<K> implements Iterable<DataWrap<K>> {

	public long total;
	public List<DataWrap<K>> objects;
	public String next;
	public ObjectNode aggregations;
	public Class<K> sourceClass;

	private DataResults() {
	}

	public static <K> DataResults<K> of(Class<K> sourceClass) {
		DataResults<K> results = new DataResults<K>();
		results.sourceClass = sourceClass;
		return results;
	}

	@Override
	public Iterator<DataWrap<K>> iterator() {
		if (objects == null)
			return Collections.emptyIterator();
		return objects.iterator();
	}
}
