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

public class WebResource extends S3Resource {

	//
	// Routes
	//

	private Payload doGet(String[] path, Context context) {
		Credentials credentials = SpaceContext.checkCredentials();
		return doGet(FileResource.FILE_BUCKET_SUFFIX, credentials.backendId(), path, true, context);
	}

	private Payload doHead(String[] path, Context context) {
		return null;
	}

	//
	// Implementation
	//

	public SpaceFilter filter() {

		return new SpaceFilter() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean matches(String uri, Context context) {
				return uri.startsWith("/1/web") //
						&& (context.method().equals(HttpMethod.GET.name()) //
								|| context.method().equals(HttpMethod.HEAD.name()));
			}

			@Override
			public Payload apply(String uri, Context context, PayloadSupplier nextFilter) throws Exception {
				if (context.method().equals(HttpMethod.GET.name()))
					return doGet(toWebPath(uri), context);
				if (context.method().equals(HttpMethod.HEAD.name()))
					return doHead(toWebPath(uri), context);
				throw Exceptions.runtime("[/1/web] endpoint only honors [GET] and [HEAD] methods");
			}

		};
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
