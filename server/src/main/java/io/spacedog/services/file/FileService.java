package io.spacedog.services.file;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequest;
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
import io.spacedog.client.file.FileStoreType;
import io.spacedog.client.file.SpaceFile;
import io.spacedog.client.file.SpaceFile.FileList;
import io.spacedog.client.http.SpaceFields;
import io.spacedog.client.schema.Schema;
import io.spacedog.server.Server;
import io.spacedog.server.ServerConfig;
import io.spacedog.services.Services;
import io.spacedog.services.SpaceService;
import io.spacedog.services.elastic.ElasticIndex;
import io.spacedog.services.file.FileStore.PutResult;
import io.spacedog.services.snapshot.FileBackup;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;
import net.codestory.http.payload.StreamingOutput;

public class FileService extends SpaceService {

	public static final String SERVICE_NAME = "files";

	// fields
	private int defaultListSize = 100;
	private FileStore systemStore = new SystemFileStore(ServerConfig.filesStorePath());
	private FileStore s3Store = new S3FileStore(ServerConfig.awsBucketPrefix() + SERVICE_NAME);

	public FileService() {
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

			SearchRequest request = elastic().prepareSearch(index(bucket))//
					.scroll(TimeValue.timeValueMinutes(1))//
					.source(source);

			response = elastic().search(request);

		} else {
			response = elastic().scroll(next, TimeValue.timeValueMinutes(1));
		}

		SearchHits hits = response.getHits();
		FileList fileList = new FileList();
		fileList.next = hits.getHits().length == 0 ? null : response.getScrollId();
		fileList.total = hits.getTotalHits().value;
		fileList.files = Lists.newArrayListWithCapacity(hits.getHits().length);
		for (SearchHit hit : hits.getHits())
			fileList.files.add(toSpaceFile(hit));

		return fileList;
	}

	private SpaceFile toSpaceFile(SearchHit hit) {
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
		return getAsByteStream(bucket, file.getKey());
	}

	public byte[] getAsByteArray(String bucket, String key) {
		return Utils.toByteArray(getAsByteStream(bucket, key));
	}

	public InputStream getAsByteStream(String bucket, String key) {
		return store(bucket).get(Server.backend().id(), bucket, key);
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

	public SpaceFile upload(String bucket, SpaceFile file, InputStream bytes) {

		FileStore store = store(bucket);
		PutResult result = store.put(Server.backend().id(), //
				bucket, file.getLength(), bytes);

		file.setKey(result.key);
		file.setHash(result.hash);
		file.setSnapshot(false);

		try {
			return update(bucket, file);

		} catch (Exception e) {
			store.delete(Server.backend().id(), bucket, file.getKey());
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
			store(bucket).delete(Server.backend().id(), bucket, file.getKey());

		} catch (Exception ignore) {
			// It's not a big deal if file is not deleted:
			// - since they are not accessible anymore,
			// - since they can be updated/replaced.
			// TODO a job should garbage collect them.
		}

		return deleted;
	}

	//
	// Buckets
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
		InternalFileSettings buckets = listBuckets();
		FileBucket previousBucket = buckets.get(bucket.name);

		if (previousBucket == null) {
			createBucketIndex(bucket);
			// make sure no files from past deleted backend
			store(bucket).deleteAll(Server.backend().id(), bucket.name);

		} else if (!bucket.type.equals(previousBucket.type))
			throw Exceptions.illegalArgument("updating store type of file buckets is forbidden");

		buckets.put(bucket.name, bucket);
		Services.settings().save(buckets);
	}

	public void deleteBucket(String name) {
		elastic().deleteIndices(index(name));
		InternalFileSettings buckets = listBuckets();
		FileBucket bucket = buckets.get(name);
		store(bucket).deleteAll(Server.backend().id(), name);
		buckets.remove(name);
		Services.settings().save(buckets);
	}

	public void deleteAllBuckets() {
		InternalFileSettings buckets = listBuckets();

		for (FileBucket bucket : buckets.values())
			// delete backend to make sure
			// any unregistered bucket is deleted
			store(bucket).deleteAll(Server.backend().id());

		buckets.clear();
		Services.settings().save(buckets);
	}

	private void createBucketIndex(FileBucket bucket) {
		Schema.checkName(bucket.name);
		Schema schema = getSchema(bucket.name);
		ElasticIndex index = index(bucket.name);

		if (elastic().exists(index))
			elastic().putMapping(index, schema.mapping());
		else
			elastic().createIndex(index, schema, false);
	}

	public Schema getSchema(String bucket) {
		return Schema.builder(bucket)//
				.keyword(PATH_FIELD)//
				.keyword(KEY_FIELD)//
				.keyword(NAME_FIELD)//
				.keyword(ENCRYPTION_FIELD)//
				.keyword(CONTENT_TYPE_FIELD)//
				.longg(LENGTH_FIELD)//
				.keyword(TAGS_FIELD)//
				.keyword(HASH_FIELD)//
				.keyword(SNAPSHOT_FIELD)//
				.keyword(OWNER_FIELD)//
				.keyword(GROUP_FIELD)//
				.timestamp(CREATED_AT_FIELD)//
				.timestamp(UPDATED_AT_FIELD)//
				.build();
	}

	public ElasticIndex index(String bucket) {
		return new ElasticIndex(SERVICE_NAME).type(bucket);
	}

	//
	// Snapshot and restore
	//

	private static final TimeValue ONE_MINUTE = TimeValue.timeValueSeconds(60);

	public void snapshot(FileBackup backup) {
		ElasticIndex[] indices = listBuckets().values().stream()//
				.map(bucket -> Services.files().index(bucket.name))//
				.toArray(ElasticIndex[]::new);

		if (!Utils.isNullOrEmpty(indices)) {
			elastic().refreshIndex(indices);

			SearchSourceBuilder builder = SearchSourceBuilder.searchSource()//
					.query(QueryBuilders.termQuery(SpaceFields.SNAPSHOT_FIELD, false))//
					.size(1000);

			SearchRequest request = elastic().prepareSearch(indices)//
					.source(builder)//
					.scroll(ONE_MINUTE);

			SearchResponse response = elastic().search(request);

			do {
				for (SearchHit hit : response.getHits()) {
					SpaceFile file = toSpaceFile(hit);
					String bucket = ElasticIndex.valueOf(hit.getIndex()).type();
					snapshot(backup, bucket, file);
				}

				response = elastic().scroll(response.getScrollId(), ONE_MINUTE);

			} while (response.getHits().getHits().length != 0);
		}
	}

	private void snapshot(FileBackup backup, String bucket, SpaceFile file) {
		Utils.info("Snapshoting /%s/%s%s", backup.backendId(), bucket, file.getPath());

		try (InputStream bytes = store(bucket).get(backup.backendId(), bucket, file.getKey())) {
			backup.restore(bucket, file.getKey(), file.getLength(), bytes);
			// TODO read copy and check hash is correct
			file.setSnapshot(true);
			update(bucket, file);

		} catch (IOException e) {
			throw Exceptions.runtime(e, "snapshot file [%s][%s] failed", bucket, file.getPath());
		}
	}

	public void restore(FileBackup backup) {

		ElasticIndex[] indices = listBuckets().values().stream()//
				.map(bucket -> Services.files().index(bucket.name))//
				.toArray(ElasticIndex[]::new);

		if (!Utils.isNullOrEmpty(indices)) {
			elastic().refreshIndex(indices);

			SearchSourceBuilder builder = SearchSourceBuilder.searchSource()//
					.query(QueryBuilders.matchAllQuery())//
					.size(1000);

			SearchRequest request = elastic().prepareSearch(indices)//
					.source(builder)//
					.scroll(ONE_MINUTE);

			SearchResponse response = elastic().search(request);

			do {
				for (SearchHit hit : response.getHits()) {
					SpaceFile file = toSpaceFile(hit);
					restore(backup, hit.getType(), file);
				}

				response = elastic().scroll(response.getScrollId(), ONE_MINUTE);

			} while (response.getHits().getHits().length != 0);
		}
	}

	private void restore(FileBackup backup, String bucket, SpaceFile file) {
		Utils.info("Restoring /%s/%s%s", backup.backendId(), bucket, file.getPath());

		FileStore store = store(bucket);
		if (!store.check(backup.backendId(), bucket, file.getKey(), file.getHash())) {

			try (InputStream bytes = backup.get(bucket, file.getKey())) {
				store.restore(backup.backendId(), bucket, file.getKey(), file.getLength(), bytes);

			} catch (IOException e) {
				throw Exceptions.runtime(e, "restore file [%s][%s] failed", bucket, file.getPath());
			}
		}
	}

	//
	// Implementation
	//

	private SpaceFile update(String bucket, SpaceFile file) {
		elastic().index(index(bucket), file.getPath(), file);
		return file;
	}

	private FileStore store(String bucket) {
		return store(getBucket(bucket));
	}

	private FileStore store(FileBucket bucket) {
		if (bucket.type.equals(FileStoreType.fs))
			return systemStore;

		if (bucket.type.equals(FileStoreType.s3))
			return s3Store;

		throw Exceptions.runtime("file bucket type [%s] is invalid", bucket.type);
	}

}
