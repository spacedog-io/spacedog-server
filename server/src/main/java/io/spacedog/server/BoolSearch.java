package io.spacedog.server;

import org.elasticsearch.index.query.BoolQueryBuilder;

public class BoolSearch {

	public Index index;
	public int from = 0;
	public int size = 10;

	public BoolQueryBuilder query;

	public BoolSearch(Index index, BoolQueryBuilder query, int from, int size) {
		this.index = index;
		this.query = query;
		this.from = from;
		this.size = size;
	}

}
