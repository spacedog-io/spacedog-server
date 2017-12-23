/**
 * © David Attias 2015
 */
package io.spacedog.server;

import org.elasticsearch.common.Strings;

import io.spacedog.model.Permission;
import io.spacedog.model.WebSettings;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.WebPath;
import net.codestory.http.Context;
import net.codestory.http.constants.Methods;
import net.codestory.http.filters.PayloadSupplier;
import net.codestory.http.payload.Payload;

public class WebService extends S3Service {

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

		Payload payload = Payload.notFound();

		if (path.size() > 0) {

			WebSettings settings = webSettings();
			Credentials credentials = SpaceContext.credentials();
			settings.prefixPermissions.get(path.first())//
					.check(credentials, Permission.read);

			String bucketName = FileService.getBucketName();
			S3File file = new S3File(bucketName, path);
			payload = doGet(withContent, file, context);

			if (payload.isSuccess())
				return payload;

			file = new S3File(bucketName, path.addLast("index.html"));
			payload = doGet(withContent, file, context);

			if (payload.isSuccess())
				return payload;

			if (!Strings.isNullOrEmpty(settings.notFoundPage)) {
				file = new S3File(bucketName, //
						WebPath.parse(settings.notFoundPage).addFirst(path.first()));
				payload = doGet(withContent, file, context);
			}
		}
		return payload;
	}

	private static WebPath toWebPath(String uri) {

		return SpaceContext.isWww() //
				// add www bucket prefix
				? WebPath.parse(uri).addFirst("www")
				// remove '/1/web'
				: WebPath.parse(uri.substring(6));
	}

	private WebSettings webSettings() {
		return SettingsService.get().getAsObject(WebSettings.class);
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
