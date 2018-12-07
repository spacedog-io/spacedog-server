package io.spacedog.client.file;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.file.SpaceFile.FileList;
import io.spacedog.client.http.SpaceParams;
import io.spacedog.client.http.SpaceResponse;
import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;

public class FileClient implements SpaceParams {

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
		return dog.post("/2/files/" + bucket + path)//
				.queryParam(OP_PARAM, "list")//
				.queryParam(NEXT_PARAM, next)//
				.size(listSize)//
				.refresh()//
				.go(200)//
				.asPojo(FileList.class);
	}

	public byte[] getAsByteArray(String bucket, String path) {
		return get(bucket, path).asBytes();
	}

	public String getAsString(String bucket, String path) {
		return get(bucket, path).asString();
	}

	public InputStream getAsByteStream(String bucket, String path) {
		return get(bucket, path).asByteStream();
	}

	private SpaceResponse get(String bucket, String path) {
		return dog.get("/2/files/{bucket}{path}")//
				.routeParam("bucket", bucket)//
				.routeParam("path", path)//
				.go(200);
	}

	public byte[] exportAsByteArray(String bucket, boolean flatZip, String... paths) {
		return exportAsByteArray(bucket, flatZip, Lists.newArrayList(paths));
	}

	public byte[] exportAsByteArray(String bucket, boolean flatZip, List<String> paths) {
		return Utils.toByteArray(exportAsByteStream(bucket, flatZip, paths));
	}

	public InputStream exportAsByteStream(String bucket, boolean flatZip, String... paths) {
		return exportAsByteStream(bucket, flatZip, Lists.newArrayList(paths));
	}

	public InputStream exportAsByteStream(String bucket, boolean flatZip, List<String> paths) {
		return dog.post("/2/files/{bucket}")//
				.routeParam("bucket", bucket)//
				.queryParam(OP_PARAM, "export")//
				.bodyJson("flatZip", flatZip, "paths", Json.toJsonNode(paths))//
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
		return prepareShare(bucket, file.getName()).file(file).go();
	}

	public SpaceFile share(String bucket, byte[] bytes) {
		return prepareShare(bucket).bytes(bytes).go();
	}

	public SpaceFile share(String bucket, byte[] bytes, String fileName) {
		return prepareShare(bucket, fileName).bytes(bytes).go();
	}

	public FileUploadRequestBuilder prepareShare(String bucket) {
		return prepareShare(bucket, null);
	}

	public FileUploadRequestBuilder prepareShare(String bucket, String fileName) {
		return prepareUpload(bucket, randomPath(fileName));
	}

	public SpaceFile upload(String bucket, String path, File file) {
		return prepareUpload(bucket, path).file(file).go();
	}

	public SpaceFile upload(String bucket, String path, byte[] bytes) {
		return prepareUpload(bucket, path).bytes(bytes).go();
	}

	public FileUploadRequestBuilder prepareUpload(String bucket, String path) {
		return new FileUploadRequestBuilder(bucket, path) {

			@Override
			public SpaceFile go() {
				return dog.put("/2/files/" + bucket + path)//
						.withContentType(type)//
						.withContentLength(length)//
						.queryParam(SpaceParams.GROUP_PARAM, group)//
						.body(inputStream)//
						.go(200)//
						.asPojo(SpaceFile.class);
			}
		};
	}

	public long deleteAll(String bucket) {
		return delete(bucket, "/");
	}

	public long delete(String bucket, String path) {
		return dog.delete("/2/files/" + bucket + path)//
				.go(200).get("deleted").asLong(0);
	}

	//
	// Buckets
	//

	public Map<String, FileBucket> listBuckets() {
		return dog.get("/2/files").go(200).asPojo(TypeFactory.defaultInstance()//
				.constructMapLikeType(Map.class, String.class, FileBucket.class));
	}

	public FileBucket getBucket(String name) {
		return dog.get("/2/files/{name}").routeParam("name", name).go(200).asPojo(FileBucket.class);
	}

	public void setBucket(FileBucket bucket) {
		dog.put("/2/files/{name}").routeParam("name", bucket.name).bodyPojo(bucket).go(200).asVoid();
	}
}
