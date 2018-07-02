package io.spacedog.client.data;

public abstract class DataBulkDeleteRequestBuilder {

	private DataBulkDeleteRequest request = new DataBulkDeleteRequest();

	public DataBulkDeleteRequestBuilder refresh() {
		this.request.refresh = true;
		return this;
	}

	public DataBulkDeleteRequestBuilder type(String type) {
		this.request.type = type;
		return this;
	}

	public DataBulkDeleteRequestBuilder query(String query) {
		this.request.query = query;
		return this;
	}

	public DataBulkDeleteRequest build() {
		return request;
	}

	public abstract long go();

}