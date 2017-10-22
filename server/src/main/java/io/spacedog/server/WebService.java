/**
 * © David Attias 2015
 */
package io.spacedog.server;

import org.elasticsearch.common.Strings;

import io.spacedog.model.WebSettings;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.WebPath;
import net.codestory.http.Context;
import net.codestory.http.constants.Methods;
import net.codestory.http.filters.PayloadSupplier;
import net.codestory.http.payload.Payload;

public class WebService extends S3Resource {

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

				if (Methods.GET.equals(method))
					return doGet(toWebPath(uri), context);

				if (Methods.HEAD.equals(method))
					return doHead(toWebPath(uri), context);

				throw Exceptions.runtime("path [%s] invalid for method [%s]", uri, method);
			}

		};
	}

	//
	// Implementation
	//

	private Payload doGet(WebPath path, Context context) {
		return doGet(true, path, context);
	}

	private Payload doHead(WebPath path, Context context) {
		return doGet(false, path, context);
	}

	private Payload doGet(boolean withContent, WebPath path, Context context) {

		if (path.size() > 0) {

			Payload payload = doGet(withContent, FileService.FILE_BUCKET_SUFFIX, //
					path, context);

			if (payload.isSuccess())
				return payload;

			payload = doGet(withContent, FileService.FILE_BUCKET_SUFFIX, path.addLast("index.html"), //
					context);

			if (payload.isSuccess())
				return payload;

			WebSettings settings = SettingsService.get().getAsObject(WebSettings.class);

			if (!Strings.isNullOrEmpty(settings.notFoundPage))
				payload = doGet(withContent, FileService.FILE_BUCKET_SUFFIX,
						WebPath.parse(settings.notFoundPage).addFirst(path.first()), //
						context);

			return payload;
		}

		return Payload.notFound();
	}

	private static WebPath toWebPath(String uri) {

		return SpaceContext.isWww() //
				// add www bucket prefix
				? WebPath.parse(uri).addFirst("www")
				// remove '/1/web'
				: WebPath.parse(uri.substring(6));
	}

	//
	// singleton
	//

	private static WebService singleton = new WebService();

	static WebService get() {
		return singleton;
	}

	private WebService() {
		SettingsService.get().registerSettings(WebSettings.class);
	}
}
