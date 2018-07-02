package io.spacedog.client.data;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import io.spacedog.utils.Exceptions;

public abstract class DataImportRequestBuilder {

	private DataImportRequest request;

	public DataImportRequestBuilder(String type) {
		this.request = new DataImportRequest(type);
	}

	public DataImportRequestBuilder withPreserveIds(boolean value) {
		this.request.preserveIds = value;
		return this;
	}

	public DataImportRequest build() {
		return request;
	}

	public void go(String export) {
		try {
			go(new ByteArrayInputStream(export.getBytes()));
		} catch (IOException e) {
			throw Exceptions.runtime(e, "IO error when importing data");
		}
	}

	public abstract void go(InputStream export) throws IOException;
}