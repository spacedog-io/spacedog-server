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
import io.spacedog.client.file.SpaceFile.FileList;
import io.spacedog.client.http.ContentTypes;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;

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

	public byte[] getAsByteArray(String bucket, String path) {
		return Utils.toByteArray(getAsByteStream(bucket, path));
	}

	public InputStream getAsByteStream(String bucket, String path) {
		return dog.get("/1/files/{bucket}{path}")//
				.routeParam("bucket", bucket)//
				.routeParam("path", path)//
				.go(200).asByteStream();
	}

	public byte[] exportAsByteArray(String bucket, String... paths) {
		return exportAsByteArray(bucket, Lists.newArrayList(paths));
	}

	public byte[] exportAsByteArray(String bucket, List<String> paths) {
		return Utils.toByteArray(exportAsByteStream(bucket, paths));
	}

	public InputStream exportAsByteStream(String bucket, String... paths) {
		return exportAsByteStream(bucket, Lists.newArrayList(paths));
	}

	public InputStream exportAsByteStream(String bucket, List<String> paths) {
		return dog.post("/1/files/{bucket}")//
				.routeParam("bucket", bucket)//
				.queryParam("op", "export")//
				.bodyJson("paths", Json.toJsonNode(paths))//
				.go(200)//
				.asByteStream();
	}

	private String randomPath(String fileName) {
		StringBuilder builder = new StringBuilder("/")//
				.append(UUID.randomUUID().toString());
		if (!Strings.isNullOrEmpty(fileName)) //
			builder.append('/').append(fileName);
		return builder.toString();
	}

	public SpaceFile share(String bucket, File file) {
		return upload(bucket, randomPath(file.getName()), file);
	}

	public SpaceFile share(String bucket, byte[] bytes) {
		return share(bucket, bytes, null);
	}

	public SpaceFile share(String bucket, byte[] bytes, String fileName) {
		return upload(bucket, randomPath(fileName), bytes);
	}

	public SpaceFile upload(String bucket, String path, File file) {
		try {
			return upload(bucket, path, new FileInputStream(file), file.length(), //
					ContentTypes.parseFileExtension(file.getName()));

		} catch (FileNotFoundException e) {
			throw Exceptions.runtime(e, "error accessing file [%s]", file.getName());
		}
	}

	public SpaceFile upload(String bucket, String path, InputStream byteStream, //
			long contentLength, String contentType) {

		return dog.put("/1/files/" + bucket + path)//
				.withContentType(contentType)//
				.withContentLength(contentLength)//
				.body(byteStream)//
				.go(200)//
				.asPojo(SpaceFile.class);
	}

	public SpaceFile upload(String bucket, String path, byte[] bytes) {
		return dog.put("/1/files/" + bucket + path).body(bytes)//
				.go(200).asPojo(SpaceFile.class);
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
