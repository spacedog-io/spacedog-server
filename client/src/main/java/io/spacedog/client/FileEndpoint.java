package io.spacedog.client;

import java.io.File;
import java.io.IOException;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.io.Files;

import io.spacedog.http.SpaceHeaders;
import io.spacedog.http.SpaceResponse;
import io.spacedog.utils.Exceptions;

public class FileEndpoint {

	int listSize = 100;
	SpaceDog dog;

	FileEndpoint(SpaceDog session) {
		this.dog = session;
	}

	public FileEndpoint listSize(int listSize) {
		this.listSize = listSize;
		return this;
	}

	public FileList listAll() {
		return list("/");
	}

	public FileList list(String webPath) {
		return list(webPath, null);
	}

	public FileList list(String webPath, String next) {
		SpaceResponse response = dog.get("/1/files" + webPath)//
				.size(listSize)//
				.queryParam("next", next)//
				.go(200, 404);

		if (response.status() == 200)
			return response.asPojo(FileList.class);

		// in case of 404
		return FileList.EMPTY;
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

		private static FileList EMPTY;

		static {
			EMPTY = new FileList();
			EMPTY.files = new FileMeta[0];
		}
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
	public static class SpaceFile {
		public String id;
		public String contentType;
		public String owner;
		public String etag;
		public byte[] content;
	}

	public SpaceFile get(String webPath) {
		SpaceResponse response = dog.get("/1/files" + webPath).go(200);
		SpaceFile file = new SpaceFile();
		file.id = webPath;
		file.content = response.asBytes();
		file.contentType = response.header(SpaceHeaders.CONTENT_TYPE);
		file.owner = response.header(SpaceHeaders.SPACEDOG_OWNER);
		file.etag = response.header(SpaceHeaders.ETAG);
		return file;
	}

	public FileMeta upload(String webPath, File file) {
		try {
			return upload(webPath, Files.toByteArray(file));
		} catch (IOException e) {
			throw Exceptions.runtime(e);
		}
	}

	public FileMeta upload(String webPath, byte[] bytes) {
		return dog.put("/1/files/" + webPath).bodyBytes(bytes)//
				.go(200).asPojo(FileMeta.class);
	}

	public String[] deleteAll() {
		return delete("/");
	}

	public String[] delete(String webPath) {
		return dog.delete("/1/files/" + webPath)//
				.go(200, 404).asPojo("deleted", String[].class);
	}

}
