/**
 * © David Attias 2015
 */
package io.spacedog.services.file;

import java.util.List;

import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.RolePermissions;
import io.spacedog.client.file.FileBucketSettings;
import io.spacedog.client.file.FileExportRequest;
import io.spacedog.client.file.SpaceFile;
import io.spacedog.client.file.SpaceFile.FileList;
import io.spacedog.client.http.ContentTypes;
import io.spacedog.client.http.SpaceHeaders;
import io.spacedog.client.http.SpaceParams;
import io.spacedog.client.http.WebPath;
import io.spacedog.server.JsonPayload;
import io.spacedog.server.Server;
import io.spacedog.server.Services;
import io.spacedog.server.SpaceFilter;
import io.spacedog.server.SpaceResty;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;
import net.codestory.http.Context;
import net.codestory.http.constants.Methods;
import net.codestory.http.filters.PayloadSupplier;
import net.codestory.http.payload.Payload;
import net.codestory.http.payload.StreamingOutput;

@SuppressWarnings("serial")
public class FileResty extends SpaceResty implements SpaceFilter {

	@Override
	public boolean matches(String uri, Context context) {
		// accepts /2/files and /2/files/* uris
		return uri.startsWith("/2/files") //
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

		throw Exceptions.methodNotAllowed(method, uri);
	}

	//
	// GET
	//

	private Payload get(WebPath absolutePath, Context context) {

		if (absolutePath.isRoot()) {
			Server.context().credentials().checkAtLeastSuperAdmin();
			return new Payload(Services.files().listBuckets());
		}

		String bucket = absolutePath.first();

		if (absolutePath.size() == 1) {
			Server.context().credentials().checkAtLeastSuperAdmin();
			return new Payload(Services.files().getBucketSettings(bucket));
		}

		String path = absolutePath.removeFirst().toString();
		return doGet(bucket, path, context);
	}

	private Payload doGet(String bucket, String path, Context context) {

		SpaceFile file = checkRead(bucket, path);

		// This auto fail is necessary to test if closeable resources
		// are finally closed in error conditions
		if (isFailRequested(context))
			throw Exceptions.illegalArgument("fail is requested for test purposes");

		Payload payload = new Payload(file.getContentType(), //
				Services.files().getAsByteStream(bucket, file.getBucketKey()))//
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

	private SpaceFile checkRead(String bucket, String path) {
		RolePermissions bucketRoles = Services.files().getBucketSettings(bucket).permissions;
		Credentials credentials = Server.context().credentials();
		SpaceFile file = Services.files().getMeta(bucket, path, true);
		bucketRoles.checkReadPermission(credentials, file.owner(), file.group());
		return file;
	}

	//
	// PUT
	//

	private Payload put(WebPath webPath, Context context) {

		String bucket = checkBucket(webPath);

		if (webPath.size() == 1)
			return createBucket(bucket, context);
		else
			return doPut(bucket, webPath, context);
	}

	private Payload doPut(String bucket, WebPath webPath, Context context) {

		String path = checkPath(webPath);
		FileBucketSettings settings = Services.files().getBucketSettings(bucket);
		Credentials credentials = Server.context().credentials();
		long contentLength = checkContentLength(context, settings.sizeLimitInKB);
		DateTime now = DateTime.now();

		SpaceFile file = Services.files().getMeta(bucket, path, false);
		String group = context.get(GROUP_PARAM);

		if (file == null) {
			settings.permissions.checkPermission(credentials, //
					Permission.create, Permission.createGroup, Permission.createMine);

			file = new SpaceFile(path);
			file.setName(webPath.last());
			file.group(checkGroupCreate(settings, credentials, group));
			file.createdAt(now);

		} else {
			settings.permissions.checkUpdatePermission(credentials, file.owner(), file.group());
			file.group(checkGroupUpdate(settings, credentials, file.group(), group));
		}

		file.setLength(contentLength);
		file.owner(credentials.id());
		file.setContentType(fileContentType(file.getName(), context));
		file.updatedAt(now);

		file = Services.files().upload(bucket, file, getRequestContentAsInputStream(context));

		ObjectNode fileNode = Json.toObjectNode(file);

		// TODO
		// Do we really need to add these?
		fileNode.put("bucket", bucket);
		fileNode.put("location", SpaceResty.spaceUrl("/1/files/")//
				.append(bucket).append(file.getEscapedPath()).toString());

		return JsonPayload.ok().withContent(fileNode).build();
	}

	private String checkGroupCreate(FileBucketSettings settings, Credentials credentials, String group) {
		if (Utils.isNullOrEmpty(group))
			return credentials.group();
		else {
			settings.permissions.checkGroupCreate(group, credentials);
			return group;
		}
	}

	private String checkGroupUpdate(FileBucketSettings settings, Credentials credentials, String oldGroup,
			String newGroup) {
		if (Strings.isNullOrEmpty(newGroup) || oldGroup.equals(newGroup))
			return oldGroup;
		settings.permissions.checkGroupUpdate(newGroup, credentials);
		return newGroup;
	}

	private Payload createBucket(String bucket, Context context) {
		Server.context().credentials().checkAtLeastSuperAdmin();
		FileBucketSettings bucketSettings = Json.toPojo(//
				getRequestContentAsBytes(context), FileBucketSettings.class);
		Services.files().setBucketSettings(bucketSettings);
		return JsonPayload.saved(false).withFields("id", bucket, "type", "bucket")//
				.withLocation("/2/files/" + bucket)//
				.build();
	}

	private String fileContentType(String fileName, Context context) {

		String contentType = context.header(SpaceHeaders.CONTENT_TYPE);

		if (Strings.isNullOrEmpty(contentType) //
				|| ContentTypes.OCTET_STREAM.equals(contentType))
			contentType = ContentTypes.parseFileExtension(fileName);

		return contentType;
	}

	private long checkContentLength(Context context, long sizeLimitInKB) {
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

	private Payload delete(WebPath webPath, Context context) {

		String bucket = checkBucket(webPath);
		String path = checkPath(webPath);
		RolePermissions bucketPermissions = Services.files().getBucketSettings(bucket).permissions;
		Credentials credentials = Server.context().credentials();

		SpaceFile file = Services.files().getMeta(bucket, path, false);

		if (file == null) {
			bucketPermissions.checkPermission(credentials, Permission.delete);
			long deleted = Services.files().deleteAll(bucket, path);
			return JsonPayload.ok()//
					.withFields("deleted", deleted)//
					.build();

		} else {
			bucketPermissions.checkDeletePermission(credentials, file.owner(), file.group());
			boolean deleted = Services.files().delete(bucket, file);
			return JsonPayload.ok()//
					.withFields("deleted", deleted ? 1 : 0)//
					.build();
		}
	}

	//
	// POST
	//

	private Payload post(WebPath webPath, Context context) {

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

	private Payload list(WebPath webPath, Context context) {
		String bucket = checkBucket(webPath);
		String path = checkPath(webPath);

		RolePermissions bucketRoles = Services.files().getBucketSettings(bucket).permissions;
		Credentials credentials = Server.context().credentials();
		bucketRoles.checkPermission(credentials, Permission.search);

		boolean refresh = isRefreshRequested(context, true);
		int size = context.query().getInteger(SIZE_PARAM, 50);
		String next = context.get(NEXT_PARAM);

		FileList fileList = Services.files().list(bucket, path, next, size, refresh);

		return JsonPayload.ok()//
				.withFields("total", fileList.total, //
						"files", fileList.files, //
						NEXT_PARAM, fileList.next)//
				.build();
	}

	private Payload search(WebPath webPath, Context context) {
		throw Exceptions.notImplemented("POST /files/...?op=search not yet implemented");
	}

	private Payload export(WebPath webPath, Context context) {
		String bucket = webPath.first();

		FileExportRequest request = Json.toPojo(//
				getRequestContentAsBytes(context), //
				FileExportRequest.class);

		List<SpaceFile> files = Lists.newArrayListWithCapacity(request.paths.size());
		for (String path : request.paths)
			files.add(checkRead(bucket, path));

		StreamingOutput output = Services.files().export(bucket, request.flatZip, files);

		return new Payload(ContentTypes.OCTET_STREAM, output)//
				.withHeader(SpaceHeaders.CONTENT_DISPOSITION, //
						SpaceHeaders.contentDisposition(request.fileName));
	}

	//
	// Implementation
	//

	private String checkBucket(WebPath webPath) {
		if (webPath.isRoot())
			throw Exceptions.illegalArgument(//
					"no bucket specified in path [%s]", webPath.toString());
		return webPath.first();
	}

	private String checkPath(WebPath webPath) {
		return webPath.removeFirst().toString();
	}

	private static WebPath toWebPath(String uri) {
		// removes '/2/files'
		return WebPath.parse(uri.substring(8));
	}

}
