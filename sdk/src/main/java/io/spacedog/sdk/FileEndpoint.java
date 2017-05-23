package io.spacedog.sdk;

import java.io.File;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceResponse;

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

	public FileList list(String webPath) {
		return list(webPath, null);
	}

	public FileList list(String webPath, String next) {
		SpaceRequest request = dog.get("/1/file" + webPath)//
				.size(listSize);

		if (next != null)
			request.queryParam("next", next);

		SpaceResponse response = request.go(200, 404);

		if (response.status() == 200)
			return response.toPojo(FileList.class);

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
		public SpaceFile[] files;
		public String next;

		private static FileList EMPTY;

		static {
			EMPTY = new FileList();
			EMPTY.files = new SpaceFile[0];
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonAutoDetect(fieldVisibility = Visibility.PUBLIC_ONLY, //
			getterVisibility = Visibility.NONE, //
			isGetterVisibility = Visibility.NONE, //
			setterVisibility = Visibility.NONE)
	public static class SpaceFile {
		public String path;
		public long size;
		public DateTime lastModified;
		public String etag;
	}

	public byte[] get(String webPath) {
		return dog.get("/1/file" + webPath).go(200).asBytes();
	}

	public void save(String webPath, File file) {
		dog.put("/1/file" + webPath).bodyFile(file).go(200);
	}

	public void save(String webPath, byte[] bytes) {
		dog.put("/1/file" + webPath).bodyBytes(bytes).go(200);
	}

	public void delete(String webPath) {
		dog.delete("/1/file" + webPath).go(200, 404);
	}

}
