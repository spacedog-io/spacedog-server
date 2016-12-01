/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.elasticsearch.common.Strings;

import com.google.common.collect.ObjectArrays;
import com.mashape.unirest.http.HttpMethod;

import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Uris;
import io.spacedog.utils.WebSettings;
import net.codestory.http.Context;
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
				return uri.startsWith("/1/web") || SpaceContext.isWww();
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
		return doGet(true, path, context);
	}

	private Payload doHead(String[] path, Context context) {
		return doGet(false, path, context);
	}

	private Payload doGet(boolean withContent, String[] path, Context context) {

		String backendId = SpaceContext.target();

		if (path.length > 0) {

			Payload payload = doGet(withContent, FileResource.FILE_BUCKET_SUFFIX, //
					backendId, path, context);

			if (payload.isSuccess())
				return payload;

			payload = doGet(withContent, FileResource.FILE_BUCKET_SUFFIX, backendId,
					ObjectArrays.concat(path, "index.html"), context);

			if (payload.isSuccess())
				return payload;

			WebSettings settings = SettingsResource.get().load(WebSettings.class);

			if (!Strings.isNullOrEmpty(settings.notFoundPage))
				payload = doGet(withContent, FileResource.FILE_BUCKET_SUFFIX, backendId, //
						ObjectArrays.concat(path[0], Uris.split(settings.notFoundPage)), context);

			return payload;
		}

		return Payload.notFound();
	}

	private static String[] toWebPath(String uri) {

		return SpaceContext.isWww() //
				// add www bucket prefix
				? ObjectArrays.concat("www", Uris.split(uri))
				// remove '/1/web'
				: Uris.split(uri.substring(6));
	}

	//
	// singleton
	//

	private static WebResource singleton = new WebResource();

	static WebResource get() {
		return singleton;
	}

	private WebResource() {
		SettingsResource.get().registerSettingsClass(WebSettings.class);
	}
}
