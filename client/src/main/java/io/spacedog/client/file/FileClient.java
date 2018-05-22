package io.spacedog.client.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.file.InternalFileSettings.FileBucketSettings;
import io.spacedog.client.file.SpaceFile.FileList;
import io.spacedog.client.file.SpaceFile.FileMeta;
import io.spacedog.client.http.ContentTypes;
import io.spacedog.client.http.SpaceResponse;
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

	public FileList listAll(String bucket) {
		return list(bucket, "/");
	}

	public FileList list(String bucket, String path) {
		return list(bucket, path, null);
	}

	public FileList list(String bucket, String path, String next) {
		return dog.post("/1/files/" + bucket + path)//
				.queryParam("op", "list")//
				.queryParam("next", next)//
				.size(listSize)//
				.refresh()//
				.go(200)//
				.asPojo(FileList.class);
	}

	@SuppressWarnings("resource")
	public SpaceFile get(String bucket, String path) {
		SpaceResponse response = dog.get("/1/files/" + bucket + path)//
				.go(200);

		return new SpaceFile()//
				.withBucket(bucket)//
				.withPath(path)//
				.withResponse(response);
	}

	public byte[] export(String bucket, String... paths) {
		return export(bucket, Lists.newArrayList(paths));
	}

	public byte[] export(String bucket, List<String> paths) {
		return dog.post("/1/files/{bucket}")//
				.routeParam("bucket", bucket)//
				.queryParam("op", "export")//
				.bodyJson("paths", Json.toJsonNode(paths))//
				.go(200)//
				.asBytes();
	}

	private String randomPath(String fileName) {
		StringBuilder builder = new StringBuilder("/")//
				.append(UUID.randomUUID().toString());
		if (!Strings.isNullOrEmpty(fileName)) //
			builder.append('/').append(fileName);
		return builder.toString();
	}

	public FileMeta share(String bucket, File file) {
		return upload(bucket, randomPath(file.getName()), file);
	}

	public FileMeta share(String bucket, byte[] bytes) {
		return share(bucket, bytes, null);
	}

	public FileMeta share(String bucket, byte[] bytes, String fileName) {
		return upload(bucket, randomPath(fileName), bytes);
	}

	public FileMeta upload(String bucket, String path, File file) {
		try {
			return upload(bucket, path, new FileInputStream(file), file.length(), //
					ContentTypes.parseFileExtension(file.getName()));

		} catch (FileNotFoundException e) {
			throw Exceptions.runtime(e, "error accessing file [%s]", file.getName());
		}
	}

	public FileMeta upload(String bucket, String path, InputStream byteStream, //
			long contentLength, String contentType) {

		return dog.put("/1/files/" + bucket + path)//
				.withContentType(contentType)//
				.withContentLength(contentLength)//
				.body(byteStream)//
				.go(200)//
				.asPojo(FileMeta.class);
	}

	public FileMeta upload(String bucket, String path, byte[] bytes) {
		return dog.put("/1/files/" + bucket + path).body(bytes)//
				.go(200).asPojo(FileMeta.class);
	}

	public long deleteAll(String bucket) {
		return delete(bucket, "/");
	}

	public long delete(String bucket, String path) {
		return dog.delete("/1/files/" + bucket + path)//
				.go(200).get("deleted").asLong(0);
	}

	//
	// Buckets
	//

	public void setBucket(FileBucketSettings settings) {
		dog.put("/1/files/" + settings.name).bodyPojo(settings).go(200);
	}

}
