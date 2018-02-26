package io.spacedog.model;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import io.spacedog.client.http.SpaceHeaders;
import io.spacedog.client.http.SpaceResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SpaceFile implements Closeable {

	private String path;
	private SpaceResponse response;

	public String path() {
		return path;
	}

	public SpaceFile withPath(String path) {
		this.path = path;
		return this;
	}

	public String contentType() {
		return response.header(SpaceHeaders.CONTENT_TYPE);
	}

	public String owner() {
		return response.header(SpaceHeaders.SPACEDOG_OWNER);
	}

	public String etag() {
		return response.header(SpaceHeaders.ETAG);
	}

	public byte[] asBytes() {
		return response.asBytes();
	}

	public String asString() {
		return response.asString();
	}

	public InputStream asByteStream() {
		return response.body().byteStream();
	}

	public Reader asCharStream() {
		return response.body().charStream();
	}

	@Override
	public void close() throws IOException {
		response.close();
	}

	public SpaceResponse response() {
		return response;
	}

	public SpaceFile withResponse(SpaceResponse body) {
		this.response = body;
		return this;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonAutoDetect(fieldVisibility = Visibility.PUBLIC_ONLY, //
			getterVisibility = Visibility.NONE, //
			isGetterVisibility = Visibility.NONE, //
			setterVisibility = Visibility.NONE)
	public static class FileMeta {
		public String path;
		public String location;
		public long size;
		public DateTime lastModified;
		public String etag;
		public String contentMd5;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonAutoDetect(fieldVisibility = Visibility.PUBLIC_ONLY, //
			getterVisibility = Visibility.NONE, //
			isGetterVisibility = Visibility.NONE, //
			setterVisibility = Visibility.NONE)
	public static class FileList {
		@JsonProperty("results")
		public FileMeta[] files;
		public String next;
	}

}