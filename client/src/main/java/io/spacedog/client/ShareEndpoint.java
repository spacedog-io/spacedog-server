package io.spacedog.client;

import java.io.File;
import java.io.IOException;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.io.Files;

import io.spacedog.http.SpaceRequest;
import io.spacedog.http.SpaceResponse;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.SpaceHeaders;

public class ShareEndpoint {

	int listSize = 100;
	SpaceDog dog;

	ShareEndpoint(SpaceDog session) {
		this.dog = session;
	}

	public ShareEndpoint listSize(int listSize) {
		this.listSize = listSize;
		return this;
	}

	public ShareList list() {
		return list(null);
	}

	public ShareList list(String next) {
		SpaceRequest request = dog.get("/1/shares")//
				.size(listSize);

		if (next != null)
			request.queryParam("next", next);

		SpaceResponse response = request.go(200, 404);

		if (response.status() == 200)
			return response.toPojo(ShareList.class);

		// in case of 404
		return ShareList.EMPTY;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonAutoDetect(fieldVisibility = Visibility.PUBLIC_ONLY, //
			getterVisibility = Visibility.NONE, //
			isGetterVisibility = Visibility.NONE, //
			setterVisibility = Visibility.NONE)
	public static class ShareList {

		@JsonProperty("results")
		public ShareMeta[] shares;
		public String next;

		private static ShareList EMPTY;

		static {
			EMPTY = new ShareList();
			EMPTY.shares = new ShareMeta[0];
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonAutoDetect(fieldVisibility = Visibility.PUBLIC_ONLY, //
			getterVisibility = Visibility.NONE, //
			isGetterVisibility = Visibility.NONE, //
			setterVisibility = Visibility.NONE)
	public static class ShareMeta {
		@JsonProperty("path")
		public String id;
		public String location;
		public String s3;
		public String etag;
		public String contentMd5;
		public long size;
		public DateTime lastModified;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonAutoDetect(fieldVisibility = Visibility.PUBLIC_ONLY, //
			getterVisibility = Visibility.NONE, //
			isGetterVisibility = Visibility.NONE, //
			setterVisibility = Visibility.NONE)
	public static class Share {
		public String id;
		public String contentType;
		public String owner;
		public String etag;
		public byte[] content;
	}

	public Share get(String id) {
		SpaceResponse response = dog.get("/1/shares/" + id).go(200);
		Share share = new Share();
		share.id = id;
		share.content = response.asBytes();
		share.contentType = response.header(SpaceHeaders.CONTENT_TYPE);
		share.owner = response.header(SpaceHeaders.SPACEDOG_OWNER);
		share.etag = response.header(SpaceHeaders.ETAG);
		return share;
	}

	public ShareMeta upload(File file) {
		try {
			return upload(Files.toByteArray(file), file.getName());
		} catch (IOException e) {
			throw Exceptions.runtime(e);
		}
	}

	public ShareMeta upload(byte[] bytes) {
		return upload(bytes, null);
	}

	public ShareMeta upload(byte[] bytes, String fileName) {
		return dog.post("/1/shares")//
				.queryParam("fileName", fileName)//
				.bodyBytes(bytes).go(200)//
				.toPojo(ShareMeta.class);
	}

	public String[] delete(String id) {
		return dog.delete("/1/shares/" + id).go(200)//
				.toPojo("deleted", String[].class);
	}

}