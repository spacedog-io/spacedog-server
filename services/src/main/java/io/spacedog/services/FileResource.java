/**
 * © David Attias 2015
 */
package io.spacedog.services;

import com.mashape.unirest.http.HttpMethod;

import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Uris;
import net.codestory.http.Context;
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
				return uri.startsWith("/1/file");
			}

			@Override
			public Payload apply(String uri, Context context, PayloadSupplier nextFilter) throws Exception {
				if (context.method().equals(HttpMethod.GET.name()))
					return get(toWebPath(uri), context);
				if (context.method().equals(HttpMethod.PUT.name()))
					return put(toWebPath(uri), context.request().contentAsBytes(), context);
				if (context.method().equals(HttpMethod.DELETE.name()))
					return delete(toWebPath(uri));

				throw Exceptions.runtime("[%s] path is invalid for [%s] method", uri, context.method());
			}

		};
	}

	//
	// Implementation
	//

	Payload get(String[] path, Context context) {
		Credentials credentials = SpaceContext.checkCredentials();
		return doGet(FILE_BUCKET_SUFFIX, credentials.backendId(), path, context);
	}

	Payload put(String[] path, byte[] bytes, Context context) {
		Credentials credentials = SpaceContext.checkAdminCredentials();
		return doUpload(FILE_BUCKET_SUFFIX, "/1/file", credentials, path, bytes, context);
	}

	Payload deleteAll() {
		return delete(Uris.ROOT_PATH);
	}

	Payload delete(String[] path) {
		Credentials credentials = SpaceContext.checkAdminCredentials();
		return doDelete(FILE_BUCKET_SUFFIX, credentials, path);
	}

	private static String[] toWebPath(String uri) {
		// remove '/1/file'
		return Uris.split(uri.substring(7));
	}

	//
	// singleton
	//

	private static FileResource singleton = new FileResource();

	static FileResource get() {
		return singleton;
	}

	private FileResource() {
	}
}
