package io.spacedog.client.data;

public class DataExportRequest {

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

	public DataExportRequest withQuery(String query) {
		this.query = query;
		return this;
	}
}