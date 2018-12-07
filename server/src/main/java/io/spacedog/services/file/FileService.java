package io.spacedog.services.file;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import io.spacedog.client.file.FileBucket;
import io.spacedog.client.file.SpaceFile;
import io.spacedog.client.file.SpaceFile.FileList;
import io.spacedog.client.schema.Schema;
import io.spacedog.server.Index;
import io.spacedog.server.Server;
import io.spacedog.server.ServerConfig;
import io.spacedog.server.Services;
import io.spacedog.server.SpaceService;
import io.spacedog.services.file.FileStore.PutResult;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;
import net.codestory.http.payload.StreamingOutput;

public class FileService extends SpaceService {

	public static final String SERVICE_NAME = "files";

	// file store types
	public static final String ELASTIC_FS = "elastic";
	public static final Object SYSTEM_FS = "system";
	public static final Object S3_FS = "s3";

	// fields
	private FileStore store;
	private int defaultListSize = 100;

	public FileService() {
		initStore();
	}

	public FileService setDefaultListSize(int listSize) {
		this.defaultListSize = listSize;
		return this;
	}

	public FileList listAll(String bucket) {
		return list(bucket, "/");
	}

	public FileList list(String bucket, String path) {
		return list(bucket, path, null, defaultListSize, false);
	}

	public FileList list(String bucket, String path, String next, int size, boolean refresh) {

		SearchResponse response = null;

		if (Strings.isNullOrEmpty(next)) {

			elastic().refreshIndex(refresh, index(bucket));

			SearchSourceBuilder source = SearchSourceBuilder.searchSource()//
					.query(QueryBuilders.prefixQuery(PATH_FIELD, path))//
					.size(size)//
					.sort(SortBuilders.fieldSort(PATH_FIELD));

			response = elastic().prepareSearch(index(bucket))//
					.setSource(source)//
					.setScroll(TimeValue.timeValueMinutes(1))//
					.get();

		} else {
			response = elastic().prepareSearchScroll(next)//
					.setScroll(TimeValue.timeValueMinutes(1))//
					.get();
		}

		SearchHits hits = response.getHits();
		FileList fileList = new FileList();
		fileList.next = hits.getHits().length == 0 ? null : response.getScrollId();
		fileList.total = hits.getTotalHits();
		fileList.files = Lists.newArrayListWithCapacity(hits.getHits().length);
		for (SearchHit hit : hits.getHits())
			fileList.files.add(toDogFile(hit));

		return fileList;
	}

	private SpaceFile toDogFile(SearchHit hit) {
		byte[] bytes = BytesReference.toBytes(hit.getSourceRef());
		return Json.toPojo(bytes, SpaceFile.class);
	}

	//
	// Get
	//

	public SpaceFile getMeta(String bucket, String path, boolean throwNotFound) {
		GetResponse response = elastic().get(index(bucket), path);

		if (response.isExists())
			return Json.toPojo(response.getSourceAsBytes(), SpaceFile.class);

		if (throwNotFound)
			throw Exceptions.objectNotFound(bucket, path);

		return null;
	}

	public byte[] getAsByteArray(String bucket, SpaceFile file) {
		return Utils.toByteArray(getAsByteStream(bucket, file));
	}

	public InputStream getAsByteStream(String bucket, SpaceFile file) {
		return getAsByteStream(bucket, file.getBucketKey());
	}

	public byte[] getAsByteArray(String bucket, String key) {
		return Utils.toByteArray(getAsByteStream(bucket, key));
	}

	public InputStream getAsByteStream(String bucket, String key) {
		return store.get(Server.backend().id(), bucket, key);
	}

	//
	// Export
	//

	public StreamingOutput exportFromPaths(String bucket, boolean flatZip, String... paths) {
		return exportFromPaths(bucket, flatZip, Lists.newArrayList(paths));
	}

	public StreamingOutput exportFromPaths(String bucket, boolean flatZip, List<String> paths) {
		List<SpaceFile> files = paths.stream()//
				.map(path -> getMeta(bucket, path, true))//
				.collect(Collectors.toList());

		return export(bucket, flatZip, files);
	}

	public StreamingOutput export(String bucket, boolean flatZip, List<SpaceFile> files) {
		return new FileBucketExport(bucket, flatZip, files);
	}

	//
	// Put
	//

	public SpaceFile upload(String bucket, SpaceFile file, byte[] bytes) {
		return upload(bucket, file, new ByteArrayInputStream(bytes));
	}

	public SpaceFile upload(String bucket, SpaceFile file, InputStream content) {

		PutResult result = store.put(//
				Server.backend().id(), //
				bucket, //
				content, //
				file.getLength());

		file.setBucketKey(result.key);
		file.setHash(result.hash);

		try {
			elastic().index(index(bucket), file.getPath(), file);
			return file;

		} catch (Exception e) {

			store.delete(Server.backend().id(), bucket, file.getBucketKey());
			throw e;
		}
	}

	//
	// Delete
	//

	public long deleteAll(String bucket) {
		return deleteAll(bucket, "/");
	}

	public long deleteAll(String bucket, String path) {

		BulkByScrollResponse response = elastic().deleteByQuery(//
				QueryBuilders.prefixQuery(PATH_FIELD, path), //
				index(bucket));

		return response.getDeleted();
	}

	public boolean delete(String bucket, SpaceFile file) {
		boolean deleted = elastic().delete(//
				index(bucket), file.getPath(), false, false);
		try {
			store.delete(Server.backend().id(), bucket, file.getBucketKey());

		} catch (Exception ignore) {
			// It's not a big deal if file is not deleted:
			// - since they are not accessible anymore,
			// - since they can be updated/replaced.
			// TODO a job should garbage collect them.
		}

		return deleted;
	}

	public void deleteBackendFiles() {
		if (ServerConfig.awsRegion().isPresent() //
				&& !ServerConfig.isOffline()) {

			store.deleteAll(Server.backend().id());
		}
	}

	public void deleteAbsolutelyAllFiles() {
		if (ServerConfig.awsRegion().isPresent() //
				&& !ServerConfig.isOffline()) {

			store.deleteAll();
		}
	}

	//
	// Settings
	//

	public InternalFileSettings listBuckets() {
		return Services.settings().getOrThrow(InternalFileSettings.class);
	}

	public FileBucket getBucket(String name) {
		FileBucket bucketSettings = listBuckets().get(name);
		if (bucketSettings == null)
			throw Exceptions.objectNotFound("file bucket", name);
		return bucketSettings;
	}

	public void setBucket(FileBucket bucket) {
		createBucketIndex(bucket);
		saveBucket(bucket);
	}

	private void saveBucket(FileBucket bucket) {
		InternalFileSettings fileSettings = listBuckets();
		fileSettings.put(bucket.name, bucket);
		Services.settings().save(fileSettings);
	}

	private void createBucketIndex(FileBucket bucket) {
		Schema.checkName(bucket.name);
		Schema schema = getSchema(bucket.name);
		Index index = index(bucket.name);

		if (elastic().exists(index))
			elastic().putMapping(index, schema.mapping());
		else
			elastic().createIndex(index, schema, false);
	}

	public Schema getSchema(String bucket) {
		return Schema.builder(bucket)//
				.keyword(PATH_FIELD)//
				.keyword(BUCKET_KEY_FIELD)//
				.keyword(NAME_FIELD)//
				.keyword(ENCRYPTION_FIELD)//
				.keyword(CONTENT_TYPE_FIELD)//
				.longg(LENGTH_FIELD)//
				.keyword(TAGS_FIELD)//
				.keyword(HASH_FIELD)//
				.keyword(OWNER_FIELD)//
				.keyword(GROUP_FIELD)//
				.timestamp(CREATED_AT_FIELD)//
				.timestamp(UPDATED_AT_FIELD)//
				.build();
	}

	public Index index(String bucket) {
		return new Index(SERVICE_NAME).type(bucket);
	}

	private void initStore() {
		String fsType = ServerConfig.fileStore();
		if (fsType.equals(SYSTEM_FS))
			store = new SystemFileStore();
		else if (fsType.equals(ELASTIC_FS))
			store = new ElasticFileStore();
		else if (fsType.equals(S3_FS))
			store = new S3FileStore();
		else
			throw Exceptions.runtime("file store type [%s] is invalid", fsType);
	}

}
