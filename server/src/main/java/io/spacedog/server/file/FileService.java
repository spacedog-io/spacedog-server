/**
 * © David Attias 2015
 */
package io.spacedog.server.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;

import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.RolePermissions;
import io.spacedog.client.file.FileExportRequest;
import io.spacedog.client.file.InternalFileSettings;
import io.spacedog.client.file.InternalFileSettings.FileBucketSettings;
import io.spacedog.client.http.ContentTypes;
import io.spacedog.client.http.SpaceHeaders;
import io.spacedog.client.http.SpaceParams;
import io.spacedog.client.http.WebPath;
import io.spacedog.client.schema.Schema;
import io.spacedog.server.Index;
import io.spacedog.server.JsonPayload;
import io.spacedog.server.Server;
import io.spacedog.server.ServerConfig;
import io.spacedog.server.SettingsService;
import io.spacedog.server.SpaceFilter;
import io.spacedog.server.SpaceService;
import io.spacedog.server.file.FileStore.PutResult;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.NotFoundException;
import io.spacedog.utils.Utils;
import net.codestory.http.Context;
import net.codestory.http.constants.Methods;
import net.codestory.http.filters.PayloadSupplier;
import net.codestory.http.payload.Payload;
import net.codestory.http.payload.StreamingOutput;

public class FileService extends SpaceService {

	// field names
	public static final String PATH = "path";
	public static final String BUCKET_KEY = "bucketKey";
	public static final String NAME = "name";
	public static final String ENCRYPTION = "encryption";
	public static final String HASH = "hash";
	public static final String CONTENT_TYPE = "contentType";
	public static final String LENGTH = "length";
	public static final String TAGS = "tags";

	// file store types
	public static final String ELASTIC_FS = "elastic";
	public static final Object SYSTEM_FS = "system";
	public static final Object S3_FS = "s3";

	// fields
	private FileStore store;

	//
	// Schema
	//

	public static Schema getSchema(String bucket) {
		return Schema.builder(bucket)//
				.keyword(PATH)//
				.keyword(BUCKET_KEY)//
				.keyword(NAME)//
				.keyword(ENCRYPTION)//
				.keyword(CONTENT_TYPE)//
				.longg(LENGTH)//
				.keyword(TAGS)//
				.keyword(HASH)//
				.keyword(OWNER_FIELD)//
				.keyword(GROUP_FIELD)//
				.timestamp(CREATED_AT_FIELD)//
				.timestamp(UPDATED_AT_FIELD)//
				.build();
	}

	//
	// Routes
	//

	public SpaceFilter filter() {

		return new SpaceFilter() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean matches(String uri, Context context) {
				// accepts /1/files and /1/files/* uris
				return uri.startsWith("/1/files") //
						&& (uri.length() == 8 || uri.charAt(8) == '/');
			}

			@Override
			public Payload apply(String uri, Context context, PayloadSupplier nextFilter) {

				String method = context.method();

				if (Methods.GET.equals(method))
					return get(toWebPath(uri), context);

				if (Methods.PUT.equals(method))
					return put(toWebPath(uri), context);

				if (Methods.DELETE.equals(method))
					return delete(toWebPath(uri), context);

				if (Methods.POST.equals(method))
					return post(toWebPath(uri), context);

				throw Exceptions.unsupportedHttpRequest(method, uri);
			}

		};
	}

	//
	// GET
	//

	public Payload get(WebPath absolutePath, Context context) {

		if (absolutePath.isRoot()) {
			Server.context().credentials().checkAtLeastSuperAdmin();
			return listBuckets(context);
		}

		String bucket = absolutePath.first();
		String path = absolutePath.removeFirst().toString();
		DogFile file = getMeta(bucket, path);

		// This auto fail is necessary to test if closeable resources
		// are finally closed in error conditions
		if (isFailRequested(context))
			throw Exceptions.illegalArgument("fail is requested for test purposes");

		Payload payload = new Payload(file.getContentType(), //
				getContent(bucket, file.getBucketKey()))//
						.withHeader(SpaceHeaders.ETAG, file.getHash())//
						.withHeader(SpaceHeaders.SPACEDOG_OWNER, file.owner())//
						.withHeader(SpaceHeaders.SPACEDOG_GROUP, file.group());

		// Since fluent-http only provides gzip encoding,
		// we only set Content-Length header if Accept-encoding
		// does not contain gzip. In case client accepts gzip,
		// fluent will gzip this file stream and use 'chunked'
		// Transfer-Encoding incompatible with Content-Length header

		if (!context.header(SpaceHeaders.ACCEPT_ENCODING).contains(SpaceHeaders.GZIP))
			payload.withHeader(SpaceHeaders.CONTENT_LENGTH, //
					Long.toString(file.getLength()));

		if (context.query().getBoolean(SpaceParams.WITH_CONTENT_DISPOSITION, false))
			payload = payload.withHeader(SpaceHeaders.CONTENT_DISPOSITION, //
					SpaceHeaders.contentDisposition(file.getName()));

		return payload;
	}

	public DogFile getMeta(String bucket, String id) {
		RolePermissions bucketRoles = bucketSettings(bucket).permissions;
		Credentials credentials = Server.context().credentials();
		DogFile file = doGetMeta(bucket, id, true);
		bucketRoles.checkRead(credentials, file.owner(), file.group());
		return file;
	}

	private Payload listBuckets(Context context) {
		// TODO Auto-generated method stub
		return null;
	}

	private DogFile toDogFile(SearchHit hit) {
		byte[] bytes = BytesReference.toBytes(hit.getSourceRef());
		return Json.toPojo(bytes, DogFile.class);
	}

	//
	// PUT
	//

	Payload put(WebPath webPath, Context context) {

		checkBucket(webPath);
		String bucket = webPath.first();

		if (webPath.size() == 1)
			return createBucket(bucket, context);

		String id = webPath.removeFirst().toString();
		FileBucketSettings settings = bucketSettings(bucket);
		Credentials credentials = Server.context().credentials();
		long contentLength = checkContentLength(context, settings.sizeLimitInKB);

		DogFile file = doGetMeta(bucket, id, false);

		if (file == null) {
			settings.permissions.check(credentials, Permission.create);
			file = new DogFile(id);
			file.setName(webPath.last());
		} else
			settings.permissions.checkUpdate(credentials, file.owner(), file.group());

		file.setLength(contentLength);
		file.owner(credentials.id());
		file.group(credentials.group());
		file.setContentType(fileContentType(file.getName(), context));

		return doUpload(bucket, file, getRequestContentAsInputStream(context));
	}

	private Payload createBucket(String bucket, Context context) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		createBucketIndex(bucket, context);
		return saveBucketSettings(bucket, context);
	}

	private Payload saveBucketSettings(String bucket, Context context) {
		InternalFileSettings fileSettings = SettingsService.get()//
				.getAsObject(InternalFileSettings.class);
		fileSettings.buckets.put(bucket, Json.toPojo(//
				getRequestContentAsBytes(context), FileBucketSettings.class));

		SettingsService.get().saveAsObject(fileSettings);
		return JsonPayload.ok().build();
	}

	private void createBucketIndex(String bucket, Context context) {
		int shards = context.query().getInteger(SHARDS_PARAM, SHARDS_DEFAULT_PARAM);
		int replicas = context.query().getInteger(REPLICAS_PARAM, REPLICAS_DEFAULT_PARAM);
		boolean async = context.query().getBoolean(ASYNC_PARAM, ASYNC_DEFAULT_PARAM);

		org.elasticsearch.common.settings.Settings settings = //
				org.elasticsearch.common.settings.Settings.builder()//
						.put("number_of_shards", shards)//
						.put("number_of_replicas", replicas)//
						.build();

		Schema.checkName(bucket);
		ObjectNode mapping = getSchema(bucket).mapping();
		Index index = Index.toIndex(bucket).service("files");

		if (elastic().exists(index))
			elastic().putMapping(index, mapping.toString());
		else
			elastic().createIndex(index, mapping.toString(), settings, async);
	}

	private String fileContentType(String fileName, Context context) {

		String contentType = context.header(SpaceHeaders.CONTENT_TYPE);

		if (Strings.isNullOrEmpty(contentType) //
				|| ContentTypes.OCTET_STREAM.equals(contentType))
			contentType = ContentTypes.parseFileExtension(fileName);

		return contentType;
	}

	public Payload doUpload(String bucket, DogFile file, InputStream content) {

		PutResult result = store.put(//
				Server.backend().backendId(), //
				bucket, //
				content, //
				file.getLength());

		file.setBucketKey(result.key);
		file.setHash(result.hash);

		try {
			elastic().index(toFileIndex(bucket), file.getPath(), file);

		} catch (Exception e) {

			store.delete(Server.backend().backendId(), //
					bucket, file.getBucketKey());

			throw e;
		}

		String path = file.getEscapedPath();
		StringBuilder location = SpaceService.spaceUrl("/1/files/")//
				.append(bucket).append(path);

		return JsonPayload.ok()//
				.withFields("bucket", bucket, //
						NAME, file.getName(), //
						PATH, path, //
						LENGTH, file.getLength(), //
						HASH, file.getHash(), //
						ENCRYPTION, file.getEncryption(), //
						CREATED_AT_FIELD, file.createdAt(), //
						UPDATED_AT_FIELD, file.updatedAt(), //
						OWNER_FIELD, file.owner(), //
						GROUP_FIELD, file.group(), //
						"location", location)
				.build();
	}

	protected long checkContentLength(Context context, long sizeLimitInKB) {
		String contentLength = context.header(SpaceHeaders.CONTENT_LENGTH);
		if (Strings.isNullOrEmpty(contentLength))
			throw Exceptions.illegalArgument("Content-Length header is required");

		long length = Long.valueOf(contentLength);
		if (length > sizeLimitInKB * 1024)
			throw Exceptions.illegalArgument(//
					"content length limit is [%s] KB", //
					length, sizeLimitInKB);

		return length;
	}

	//
	// DELETE
	//

	public Payload delete(WebPath webPath, Context context) {

		checkBucket(webPath);
		String bucket = webPath.first();
		String id = webPath.removeFirst().toString();
		RolePermissions bucketPermissions = bucketSettings(bucket).permissions;
		Credentials credentials = Server.context().credentials();

		DogFile file = doGetMeta(bucket, id, false);

		if (file == null) {
			bucketPermissions.check(credentials, Permission.delete);
			return doDeleteAll(bucket, id);
		} else {
			bucketPermissions.checkDelete(credentials, file.owner(), file.group());
			return doDelete(bucket, id, file.getBucketKey());
		}
	}

	private Payload doDelete(String bucket, String id, String bucketKey) {
		boolean deleted = elastic().delete(//
				toFileIndex(bucket), id, false, false);
		try {
			store.delete(Server.backend().backendId(), //
					bucket, bucketKey);

		} catch (Exception ignore) {
			// It's not a big deal if file is not deleted:
			// - since they are not accessible anymore,
			// - since they can be updated/replaced.
			// TODO a job should garbage collect them.
		}

		return JsonPayload.ok()//
				.withFields("deleted", deleted ? 1 : 0)//
				.build();
	}

	private Payload doDeleteAll(String bucket, String path) {

		BulkByScrollResponse response = elastic().deleteByQuery(//
				QueryBuilders.prefixQuery(PATH, path), //
				toFileIndex(bucket));

		return JsonPayload.ok()//
				.withFields("deleted", response.getDeleted())//
				.build();
	}

	//
	// POST
	//

	public Payload post(WebPath webPath, Context context) {

		String op = context.get("op");

		if (!Strings.isNullOrEmpty(op)) {
			if (op.equals("list"))
				return list(webPath, context);

			if (op.equals("search"))
				return search(webPath, context);

			if (op.equals("export"))
				return export(webPath, context);
		}

		throw Exceptions.illegalArgument(//
				"operation [%s] is invalid for [POST][/1/files%s]", //
				op, webPath);
	}

	public Payload list(WebPath webPath, Context context) {
		checkBucket(webPath);
		String bucket = webPath.first();
		String path = webPath.removeFirst().toString();

		RolePermissions bucketRoles = bucketSettings(bucket).permissions;
		Credentials credentials = Server.context().credentials();

		bucketRoles.check(credentials, Permission.search);
		return doList(bucket, path, context);
	}

	public Payload doList(String bucket, String path, Context context) {

		String next = context.get("next");
		SearchResponse response = null;

		if (Strings.isNullOrEmpty(next)) {

			elastic().refreshType(toFileIndex(bucket), //
					isRefreshRequested(context, true));

			SearchSourceBuilder source = SearchSourceBuilder.searchSource()//
					.query(QueryBuilders.prefixQuery(PATH, path))//
					.size(context.query().getInteger(SIZE_PARAM, 50))//
					.sort(SortBuilders.fieldSort(PATH));

			response = elastic().prepareSearch(toFileIndex(bucket))//
					.setSource(source)//
					.setScroll(TimeValue.timeValueMinutes(1))//
					.get();

		} else {
			response = elastic().prepareSearchScroll(next)//
					.setScroll(TimeValue.timeValueMinutes(1))//
					.get();
		}

		SearchHits hits = response.getHits();
		List<DogFile> files = Lists.newArrayListWithCapacity(hits.getHits().length);
		for (SearchHit hit : hits.getHits())
			files.add(toDogFile(hit));

		next = hits.getHits().length == 0 ? null : response.getScrollId();

		return JsonPayload.ok()//
				.withFields("total", hits.totalHits, //
						"files", files, "next", next)//
				.build();
	}

	public Payload search(WebPath webPath, Context context) {
		// TODO Auto-generated method stub
		return null;
	}

	public Payload export(WebPath webPath, Context context) {
		String bucket = webPath.first();

		FileExportRequest request = Json.toPojo(//
				getRequestContentAsBytes(context), //
				FileExportRequest.class);

		List<DogFile> files = Lists.newArrayListWithCapacity(request.paths.size());
		for (String path : request.paths)
			files.add(getMeta(bucket, path));

		return new Payload(ContentTypes.OCTET_STREAM, //
				new BucketExport(bucket, files))//
						.withHeader(SpaceHeaders.CONTENT_DISPOSITION, //
								SpaceHeaders.contentDisposition(request.fileName));

	}

	private class BucketExport implements StreamingOutput {

		private String bucket;
		private List<DogFile> files;

		public BucketExport(String bucket, List<DogFile> files) {
			this.bucket = bucket;
			this.files = files;
		}

		@Override
		public void write(OutputStream output) throws IOException {
			ZipOutputStream zip = new ZipOutputStream(output);
			for (DogFile file : files) {
				zip.putNextEntry(new ZipEntry(file.getPath()));
				InputStream fileStream = getContent(bucket, file.getBucketKey());
				ByteStreams.copy(fileStream, zip);
				Utils.closeSilently(fileStream);
				zip.flush();
			}
			zip.close();
		}

	}

	//
	// public interface
	//

	public void deleteBackendFiles() {
		if (ServerConfig.awsRegion().isPresent() //
				&& !ServerConfig.isOffline()) {

			store.deleteAll(Server.backend().backendId());
		}
	}

	public void deleteAbsolutelyAllFiles() {
		if (ServerConfig.awsRegion().isPresent() //
				&& !ServerConfig.isOffline()) {

			store.deleteAll();
		}
	}

	public DogFile doGetMeta(String bucket, String path, boolean throwNotFound) {
		GetResponse response = elastic().get(toFileIndex(bucket), path);

		if (response.isExists())
			return Json.toPojo(response.getSourceAsBytes(), DogFile.class);

		if (throwNotFound)
			throw Exceptions.notFound(bucket, path);

		return null;
	}

	//
	// Implementation
	//

	public InputStream getContent(String bucket, String key) {
		return store.get(Server.backend().backendId(), bucket, key);
	}

	private void checkBucket(WebPath webPath) {
		if (webPath.isRoot())
			throw Exceptions.illegalArgument(//
					"no bucket specified in path [%s]", webPath.toString());
	}

	private Index toFileIndex(String first) {
		return Index.toIndex(first).service("files");
	}

	private static WebPath toWebPath(String uri) {
		// removes '/1/files'
		return WebPath.parse(uri.substring(8));
	}

	public FileBucketSettings bucketSettings(String bucket) {
		FileBucketSettings bucketSettings = SettingsService.get()//
				.getAsObject(InternalFileSettings.class).buckets.get(bucket);
		if (bucketSettings == null)
			throw new NotFoundException("bucket [%s] not found", bucket);
		return bucketSettings;
	}

	private void initFileStore() {
		String fsType = ServerConfig.serverFileStore().orElse(ELASTIC_FS);
		if (fsType.equals(SYSTEM_FS))
			store = new SystemFileStore();
		else if (fsType.equals(ELASTIC_FS))
			store = new ElasticFileStore();
		else if (fsType.equals(S3_FS))
			store = new S3FileStore();
		else
			throw Exceptions.runtime("file store type [%s] is invalid", fsType);
	}

	//
	// singleton
	//

	private static FileService singleton = new FileService();

	public static FileService get() {
		return singleton;
	}

	private FileService() {
		SettingsService.get().registerSettings(InternalFileSettings.class);
		initFileStore();
	}
}
