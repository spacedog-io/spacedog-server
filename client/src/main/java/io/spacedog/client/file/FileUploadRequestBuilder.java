package io.spacedog.client.file;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import io.spacedog.client.http.ContentTypes;
import io.spacedog.utils.Exceptions;

public abstract class FileUploadRequestBuilder {

	protected long length;
	protected String path;
	protected String bucket;
	protected InputStream inputStream;
	protected String type;
	protected String group;

	public FileUploadRequestBuilder(String bucket, String path) {
		this.bucket = bucket;
		this.path = path;
	}

	public FileUploadRequestBuilder inputStream(InputStream inputStream) {
		this.inputStream = inputStream;
		return this;
	}

	public FileUploadRequestBuilder bytes(byte[] bytes) {
		return inputStream(new ByteArrayInputStream(bytes)).length(bytes.length);
	}

	public FileUploadRequestBuilder file(File file) {
		try {
			return inputStream(new FileInputStream(file))//
					.type(ContentTypes.parseFileExtension(file.getName()))//
					.length(file.length());

		} catch (FileNotFoundException e) {
			throw Exceptions.runtime(e, "file [%s] not found", file);
		}
	}

	public FileUploadRequestBuilder length(long length) {
		this.length = length;
		return this;
	}

	public FileUploadRequestBuilder type(String type) {
		this.type = type;
		return this;
	}

	public FileUploadRequestBuilder group(String group) {
		this.group = group;
		return this;
	}

	public abstract SpaceFile go();

}
