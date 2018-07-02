package io.spacedog.client.data;

public class DataImportRequest {

	public String type;
	public Boolean preserveIds;

	public DataImportRequest(String type) {
		this.type = type;
	}

	public DataImportRequest withPreserveIds(boolean value) {
		this.preserveIds = value;
		return this;
	}

}