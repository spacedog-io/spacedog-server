/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Optional;

import com.mashape.unirest.http.HttpMethod;

import io.spacedog.utils.Credentials;
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

				String method = context.method();

				if (HttpMethod.GET.name().equals(method))
					return get(toWebPath(uri), context);

				if (HttpMethod.PUT.name().equals(method))
					return put(toWebPath(uri), context.request().contentAsBytes(), context);

				if (HttpMethod.DELETE.name().equals(method))
					return delete(toWebPath(uri));

				throw Exceptions.runtime("path [%s] invalid for method [%s]", uri, method);
			}

		};
	}

	//
	// Implementation
	//

	Payload get(String[] path, Context context) {
		Credentials credentials = SpaceContext.getCredentials();
		Optional<Payload> payload = doGet(FILE_BUCKET_SUFFIX, credentials.target(), path, context);

		if (payload.isPresent())
			return payload.get();

		return doList(FILE_BUCKET_SUFFIX, credentials.target(), path, context);
	}

	Payload put(String[] path, byte[] bytes, Context context) {
		Credentials credentials = SpaceContext.checkAdminCredentials();
		return doUpload(FILE_BUCKET_SUFFIX, "/1/file", credentials, path, bytes, context);
	}

	Payload deleteAll() {
		return delete(Uris.ROOT);
	}

	Payload delete(String[] path) {
		Credentials credentials = SpaceContext.checkAdminCredentials();
		return doDelete(FILE_BUCKET_SUFFIX, credentials, path, false, false);
	}

	private static String[] toWebPath(String uri) {
		// removes '/1/file'
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
