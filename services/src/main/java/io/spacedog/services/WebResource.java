/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Optional;

import com.google.common.collect.ObjectArrays;
import com.mashape.unirest.http.HttpMethod;

import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Uris;
import net.codestory.http.Context;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.filters.PayloadSupplier;
import net.codestory.http.payload.Payload;

public class WebResource extends S3Resource {

	//
	// Routes
	//

	public SpaceFilter filter() {

		return new SpaceFilter() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean matches(String uri, Context context) {
				return uri.startsWith("/1/web");
			}

			@Override
			public Payload apply(String uri, Context context, PayloadSupplier nextFilter) throws Exception {

				String method = context.method();

				if (HttpMethod.GET.name().equals(method))
					return doGet(toWebPath(uri), context);

				if (HttpMethod.HEAD.name().equals(method))
					return doHead(toWebPath(uri), context);

				throw Exceptions.runtime("path [%s] invalid for method [%s]", uri, method);
			}

		};
	}

	//
	// Implementation
	//

	private Payload doGet(String[] path, Context context) {
		Credentials credentials = SpaceContext.checkCredentials();

		if (path.length == 0)
			throw Exceptions.illegalArgument("web prefix not specified");

		Optional<Payload> payload = doGet(FileResource.FILE_BUCKET_SUFFIX, //
				credentials.backendId(), path, context);

		if (payload.isPresent())
			return payload.get();

		payload = doGet(FileResource.FILE_BUCKET_SUFFIX, credentials.backendId(),
				ObjectArrays.concat(path, "index.html"), context);

		if (payload.isPresent())
			return payload.get();

		payload = doGet(FileResource.FILE_BUCKET_SUFFIX, credentials.backendId(), //
				new String[] { path[0], "404.html" }, context);

		if (payload.isPresent())
			return payload.get().withCode(HttpStatus.NOT_FOUND);

		return Payload.notFound();
	}

	private Payload doHead(String[] path, Context context) {
		return null;
	}

	private static String[] toWebPath(String uri) {
		// remove '/1/web'
		return Uris.split(uri.substring(6));
	}

	//
	// singleton
	//

	private static WebResource singleton = new WebResource();

	static WebResource get() {
		return singleton;
	}

	private WebResource() {
	}
}
