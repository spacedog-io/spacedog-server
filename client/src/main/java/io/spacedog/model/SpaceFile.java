package io.spacedog.model;

import java.io.IOException;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.spacedog.utils.Exceptions;
import okhttp3.ResponseBody;

public class SpaceFile {
	private String path;
	private String contentType;
	private String owner;
	private String etag;
	private ResponseBody body;

	public String path() {
		return path;
	}

	public SpaceFile withPath(String path) {
		this.path = path;
		return this;
	}

	public String contentType() {
		return contentType;
	}

	public SpaceFile withContentType(String contentType) {
		this.contentType = contentType;
		return this;
	}

	public String owner() {
		return owner;
	}

	public SpaceFile withOwner(String owner) {
		this.owner = owner;
		return this;
	}

	public String etag() {
		return etag;
	}

	public SpaceFile withEtag(String etag) {
		this.etag = etag;
		return this;
	}

	public byte[] content() {
		try {
			return body.bytes();
		} catch (IOException e) {
			throw Exceptions.runtime(e, "error reading file content");
		}
	}

	public ResponseBody body() {
		return body;
	}

	public SpaceFile withBody(ResponseBody body) {
		this.body = body;
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