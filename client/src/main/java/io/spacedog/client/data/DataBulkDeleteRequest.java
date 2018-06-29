package io.spacedog.client.data;

import io.spacedog.client.elastic.ESQueryBuilder;

public abstract class DataBulkDeleteRequest {
	public boolean refresh;
	public String type;
	public String query;

	public DataBulkDeleteRequest refresh() {
		this.refresh = true;
		return this;
	}

	public DataBulkDeleteRequest type(String type) {
		this.type = type;
		return this;
	}

	public DataBulkDeleteRequest query(String query) {
		this.query = query;
		return this;
	}

	public DataBulkDeleteRequest query(ESQueryBuilder query) {
		return query(query.toString());
	}

	public abstract long go();

}