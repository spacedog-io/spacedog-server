package io.spacedog.client.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.Lists;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.file.SpaceFile.FileList;
import io.spacedog.client.file.SpaceFile.FileMeta;
import io.spacedog.client.http.ContentTypes;
import io.spacedog.client.http.SpaceResponse;
import io.spacedog.client.http.WebPath;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;

public class FileClient {

	private SpaceDog dog;
	private int listSize = 100;

	public FileClient(SpaceDog session) {
		this.dog = session;
	}

	public FileClient listSize(int listSize) {
		this.listSize = listSize;
		return this;
	}

	public FileList listAll() {
		return list("/");
	}

	public FileList list(String path) {
		return list(path, null);
	}

	public FileList list(String path, String next) {
		SpaceResponse response = dog.get("/1/files" + path)//
				.size(listSize)//
				.queryParam("next", next)//
				.go(200, 404);

		if (response.status() == 200)
			return response.asPojo(FileList.class);

		// in case of 404
		return EMPTY_FILE_LIST;
	}

	private final static FileList EMPTY_FILE_LIST;

	static {
		EMPTY_FILE_LIST = new FileList();
		EMPTY_FILE_LIST.files = new FileMeta[0];
	}

	@SuppressWarnings("resource")
	public SpaceFile get(String path) {
		SpaceResponse response = dog.get("/1/files" + path).go(200);

		return new SpaceFile()//
				.withPath(path)//
				.withResponse(response);
	}

	public byte[] downloadAll(String bucket, String... paths) {
		return downloadAll(bucket, Lists.newArrayList(paths));
	}

	public byte[] downloadAll(String bucket, List<String> paths) {
		return dog.post("/1/files/{bucket}/_download")//
				.routeParam("bucket", bucket)//
				.bodyJson("paths", Json.toJsonNode(paths))//
				.go(200)//
				.asBytes();
	}

	private String randomPath(String rootPath, String fileName) {
		return WebPath.parse(rootPath)//
				.addLast(UUID.randomUUID().toString())//
				.addLast(fileName)//
				.toString();
	}

	public FileMeta share(String rootPath, File file) {
		return upload(randomPath(rootPath, file.getName()), file);
	}

	public FileMeta share(String rootPath, byte[] bytes) {
		return share(rootPath, bytes, null);
	}

	public FileMeta share(String rootPath, byte[] bytes, String fileName) {
		return upload(randomPath(rootPath, fileName), bytes);
	}

	public FileMeta upload(String path, File file) {
		try {
			return upload(path, new FileInputStream(file), file.length(), //
					ContentTypes.parseFileExtension(file.getName()));

		} catch (FileNotFoundException e) {
			throw Exceptions.runtime(e, "error accessing file [%s]", file.getName());
		}
	}

	public FileMeta upload(String path, InputStream byteStream, //
			long contentLength, String contentType) {

		return dog.put("/1/files" + path)//
				.withContentType(contentType)//
				.withContentLength(contentLength)//
				.body(byteStream)//
				.go(200)//
				.asPojo(FileMeta.class);
	}

	public FileMeta upload(String path, byte[] bytes) {
		return dog.put("/1/files" + path).body(bytes)//
				.go(200).asPojo(FileMeta.class);
	}

	public String[] deleteAll() {
		return delete("/");
	}

	public String[] delete(String path) {
		return dog.delete("/1/files" + path)//
				.go(200, 404).asPojo("deleted", String[].class);
	}

}
