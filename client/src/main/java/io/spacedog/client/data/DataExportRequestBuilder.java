package io.spacedog.client.data;

import io.spacedog.client.http.SpaceResponse;

public abstract class DataExportRequestBuilder {

	private final DataExportRequest request;

	public DataExportRequestBuilder(String type) {
		this.request = new DataExportRequest(type);
	}

	public DataExportRequestBuilder withRefresh(boolean value) {
		this.request.refresh = value;
		return this;
	}

	public DataExportRequestBuilder withQuery(String query) {
		this.request.query = query;
		return this;
	}

	public DataExportRequest build() {
		return request;
	}

	public abstract SpaceResponse go();

}