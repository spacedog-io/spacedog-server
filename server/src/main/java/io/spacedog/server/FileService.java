/**
 * © David Attias 2015
 */
package io.spacedog.server;

import java.io.IOException;
import java.util.List;

import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.RolePermissions;
import io.spacedog.client.file.DownloadRequest;
import io.spacedog.client.file.FileSettings;
import io.spacedog.client.http.WebPath;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import net.codestory.http.Context;
import net.codestory.http.constants.Methods;
import net.codestory.http.filters.PayloadSupplier;
import net.codestory.http.payload.Payload;

public class FileService extends S3Service {

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
					return delete(toWebPath(uri));

				if (Methods.POST.equals(method))
					return download(toWebPath(uri), context);

				throw Exceptions.unsupportedHttpRequest(method, uri);
			}

		};
	}

	//
	// Implementation
	//

	public Payload get(WebPath path, Context context) {
		S3File file = new S3File(getBucketName(), path).rootUri("/1/files");

		if (path.isRoot()) {
			SpaceContext.credentials().checkAtLeastSuperAdmin();
			return doList(file, context);
		}

		RolePermissions prefixRoles = fileSettings().permissions.get(path.first());

		file.open();

		if (file.exists()) {
			file.checkRead(prefixRoles);
			return doGet(true, file, context);
		} else {
			Credentials credentials = SpaceContext.credentials();
			prefixRoles.check(credentials, Permission.search);
			return doList(file, context);
		}

	}

	Payload put(WebPath path, Context context) {

		if (path.size() < 2)
			throw Exceptions.illegalArgument(//
					"path [%s] doesn't specify any bucket", path.toString());

		FileSettings settings = SettingsService.get().getAsObject(FileSettings.class);
		long contentLength = checkContentLength(context, settings.sizeLimitInKB);

		S3File file = new S3File(getBucketName(), path)//
				.fileName(path.last())//
				.contentLength(contentLength)//
				.owner(SpaceContext.credentials())//
				.rootUri("/1/files");

		RolePermissions prefixRoles = fileSettings().permissions.get(path.first());
		file.checkUpdate(prefixRoles);

		return doUpload(file, context);
	}

	public Payload delete(WebPath path) {
		S3File file = new S3File(getBucketName(), path);

		if (path.isRoot()) {
			SpaceContext.credentials().isAtLeastSuperAdmin();
			return doDeleteAll(file);
		}

		RolePermissions prefixPermissions = fileSettings().permissions.get(path.first());

		if (file.exists()) {
			file.checkDelete(prefixPermissions);
			return doDelete(file);
		} else {
			prefixPermissions.check(SpaceContext.credentials(), Permission.delete);
			return doDeleteAll(file);
		}
	}

	public Payload download(WebPath path, Context context) {

		if (path.size() < 2 //
				|| path.last().equals("_download") == false)
			throw Exceptions.unsupportedHttpRequest(context.method(), context.uri());

		DownloadRequest request;

		try {
			request = Json.toPojo(//
					context.request().content(), DownloadRequest.class);

		} catch (IOException e) {
			throw Exceptions.illegalArgument(e, "error reading file download request content");
		}

		List<S3File> files = toS3Files(getBucketName(), request.paths);
		S3File.checkPermissions(files, //
				fileSettings().permissions.get(path.first()), //
				Permission.read, Permission.readGroup, Permission.readMine);

		return doZip(files, request.fileName);
	}

	public Payload deleteAll() {
		return delete(WebPath.ROOT);
	}

	//
	// Implementation
	//

	private static WebPath toWebPath(String uri) {
		// removes '/1/files'
		return WebPath.parse(uri.substring(8));
	}

	private FileSettings fileSettings() {
		return SettingsService.get().getAsObject(FileSettings.class);
	}

	public static String getBucketName() {
		return getBucketName("files");
	}

	//
	// singleton
	//

	private static FileService singleton = new FileService();

	public static FileService get() {
		return singleton;
	}

	private FileService() {
		SettingsService.get().registerSettings(FileSettings.class);
	}
}
