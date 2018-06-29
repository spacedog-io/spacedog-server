package io.spacedog.client.data;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import io.spacedog.utils.Exceptions;

public abstract class DataImportRequest {

	public String type;
	public Boolean preserveIds;

	public DataImportRequest(String type) {
		this.type = type;
	}

	public DataImportRequest withPreserveIds(boolean value) {
		this.preserveIds = value;
		return this;
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