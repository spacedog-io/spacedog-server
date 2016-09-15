package io.spacedog.services;

import org.elasticsearch.index.query.BoolQueryBuilder;

public class BoolSearchQuery {

	public String backendId;
	public String type;
	public BoolQueryBuilder query;
	public int from = 0;
	public int size = 10;

	public BoolSearchQuery(String backendId, String type, BoolQueryBuilder query) {
		this.backendId = backendId;
		this.type = type;
		this.query = query;
	}

}
