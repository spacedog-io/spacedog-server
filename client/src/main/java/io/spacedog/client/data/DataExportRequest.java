package io.spacedog.client.data;

import io.spacedog.client.elastic.ESQueryBuilder;
import io.spacedog.client.http.SpaceResponse;

public abstract class DataExportRequest {

	public String type;
	public Boolean refresh;
	public String query;

	public DataExportRequest(String type) {
		this.type = type;
	}

	public DataExportRequest withRefresh(boolean value) {
		this.refresh = value;
		return this;
	}

	public DataExportRequest withQuery(ESQueryBuilder query) {
		return withQuery(query.toString());
	}

	public DataExportRequest withQuery(String query) {
		this.query = query;
		return this;
	}

	public abstract SpaceResponse go();

}