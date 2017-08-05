package io.spacedog.services;

import org.elasticsearch.index.query.BoolQueryBuilder;

public class BoolSearch {

	public String type;
	public int from = 0;
	public int size = 10;

	public BoolQueryBuilder query;

	public BoolSearch(String type, BoolQueryBuilder query, int from, int size) {
		this.type = type;
		this.query = query;
		this.from = from;
		this.size = size;
	}

}
