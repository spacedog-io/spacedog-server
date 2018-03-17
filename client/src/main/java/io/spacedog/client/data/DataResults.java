package io.spacedog.client.data;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DataResults<K> {
	public long total;
	public List<DataWrap<K>> results;
}
