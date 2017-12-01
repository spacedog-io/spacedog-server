/**
 * © David Attias 2015
 */
package io.spacedog.server;

import io.spacedog.model.FileSettings;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.WebPath;
import net.codestory.http.Context;
import net.codestory.http.constants.Methods;
import net.codestory.http.filters.PayloadSupplier;
import net.codestory.http.payload.Payload;

public class FileService extends S3Service {

	static final String FILE_BUCKET_SUFFIX = "files";

	//
	// Routes
	//

	public SpaceFilter filter() {

		return new SpaceFilter() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean matches(String uri, Context context) {
				// accepts '/1/files' or '/1/files/*'
				return uri.startsWith("/1/files") //
						&& (uri.length() == 8 || uri.charAt(8) == '/');
			}

			@Override
			public Payload apply(String uri, Context context, PayloadSupplier nextFilter) throws Exception {

				String method = context.method();

				if (Methods.GET.equals(method))
					return get(toWebPath(uri), context);

				if (Methods.PUT.equals(method))
					return put(toWebPath(uri), context);

				if (Methods.DELETE.equals(method))
					return delete(toWebPath(uri));

				throw Exceptions.methodNotAllowed(method, uri);
			}

		};
	}

	//
	// Implementation
	//

	Payload get(WebPath path, Context context) {
		Payload payload = doGet(FILE_BUCKET_SUFFIX, path, context);

		if (payload.isSuccess())
			return payload;

		return doList(FILE_BUCKET_SUFFIX, "/1/files", path, context);
	}

	Payload put(WebPath path, Context context) {
		Credentials credentials = SpaceContext.credentials().checkAtLeastAdmin();

		if (path.size() < 2)
			throw Exceptions.illegalArgument("path [%s] has no prefix", path.toString());

		FileSettings settings = SettingsService.get().getAsObject(FileSettings.class);
		long contentLength = checkContentLength(context, settings.sizeLimitInKB);
		return doUpload(FILE_BUCKET_SUFFIX, "/1/files", credentials, //
				path, path.last(), contentLength, context);
	}

	Payload deleteAll() {
		return delete(WebPath.ROOT);
	}

	Payload delete(WebPath path) {
		SpaceContext.credentials().checkAtLeastAdmin();
		return doDelete(FILE_BUCKET_SUFFIX, path, false);
	}

	private static WebPath toWebPath(String uri) {
		// removes '/1/files'
		return WebPath.parse(uri.substring(8));
	}

	//
	// singleton
	//

	private static FileService singleton = new FileService();

	static FileService get() {
		return singleton;
	}

	private FileService() {
		SettingsService.get().registerSettings(FileSettings.class);
	}
}
