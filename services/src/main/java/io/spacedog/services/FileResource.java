/**
 * © David Attias 2015
 */
package io.spacedog.services;

import io.spacedog.model.FileSettings;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.WebPath;
import net.codestory.http.Context;
import net.codestory.http.constants.Methods;
import net.codestory.http.filters.PayloadSupplier;
import net.codestory.http.payload.Payload;

public class FileResource extends S3Resource {

	static final String FILE_BUCKET_SUFFIX = "files";

	//
	// Routes
	//

	public SpaceFilter filter() {

		return new SpaceFilter() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean matches(String uri, Context context) {
				// accepts '/1/file' or '/1/file/*'
				return uri.startsWith("/1/file") //
						&& (uri.length() == 7 || uri.charAt(7) == '/');
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
		Credentials credentials = SpaceContext.getCredentials();
		Payload payload = doGet(FILE_BUCKET_SUFFIX, credentials.backendId(), path, context);

		if (payload.isSuccess())
			return payload;

		return doList(FILE_BUCKET_SUFFIX, credentials.backendId(), path, context);
	}

	Payload put(WebPath path, Context context) {
		Credentials credentials = SpaceContext.checkAdminCredentials();
		FileSettings settings = SettingsResource.get().load(FileSettings.class);
		long contentLength = checkContentLength(context, settings.fileSizeLimitInKB);
		return doUpload(FILE_BUCKET_SUFFIX, "/1/file", credentials, path, context, contentLength);
	}

	Payload deleteAll() {
		return delete(WebPath.ROOT);
	}

	Payload delete(WebPath path) {
		Credentials credentials = SpaceContext.checkAdminCredentials();
		return doDelete(FILE_BUCKET_SUFFIX, credentials, path, false, false);
	}

	private static WebPath toWebPath(String uri) {
		// removes '/1/file'
		return WebPath.parse(uri.substring(7));
	}

	//
	// singleton
	//

	private static FileResource singleton = new FileResource();

	static FileResource get() {
		return singleton;
	}

	private FileResource() {
		SettingsResource.get().registerSettingsClass(FileSettings.class);
	}
}
