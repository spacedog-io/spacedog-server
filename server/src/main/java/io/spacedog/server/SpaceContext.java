package io.spacedog.server;

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Maps;
import com.google.common.net.HttpHeaders;

import io.spacedog.http.SpaceBackend;
import io.spacedog.model.Settings;
import io.spacedog.utils.AuthorizationHeader;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Optional7;
import io.spacedog.utils.SpaceHeaders;
import net.codestory.http.Context;
import net.codestory.http.constants.Methods;

/**
 * Context credentials should only be accessed from public static check methods
 * from this class.
 */
public class SpaceContext {

	private static ThreadLocal<SpaceContext> threadLocal = new ThreadLocal<>();

	private String uri;
	private Context context;
	private SpaceBackend backend;
	private boolean isTest;
	private Debug debug;
	private Credentials credentials;
	private boolean authorizationChecked;

	private Map<Class<?>, Settings> settings;

	private SpaceContext(String uri, Context context) {
		this.uri = uri;
		this.context = context;
		this.isTest = Boolean.parseBoolean(//
				context().header(SpaceHeaders.SPACEDOG_TEST));
		this.debug = new Debug(Boolean.parseBoolean(//
				context().header(SpaceHeaders.SPACEDOG_DEBUG)));
		this.credentials = Credentials.GUEST;
		this.backend = backend(//
				context.request().header(HttpHeaders.HOST));
	}

	private static SpaceBackend backend(String hostAndPort) {
		ServerConfiguration conf = Start.get().configuration();

		// first try to match api backend
		SpaceBackend api = conf.apiBackend();
		Optional7<SpaceBackend> backend = api.checkAndInstantiate(hostAndPort);
		if (backend.isPresent())
			return backend.get();

		// second try to match webapp backend
		Optional<SpaceBackend> webApp = conf.wwwBackend();
		if (webApp.isPresent()) {
			backend = webApp.get().checkAndInstantiate(hostAndPort);
			if (backend.isPresent())
				return backend.get();
		}

		return api.instanciate();
	}

	public Context context() {
		return context;
	}

	public static SpaceFilter filter() {

		// uri is already checked by SpaceFilter default matches method

		return (uri, context, nextFilter) -> {
			if (threadLocal.get() == null) {
				try {
					threadLocal.set(new SpaceContext(uri, context));
					return nextFilter.get();
				} finally {
					threadLocal.set(null);
				}
			} else
				// means there is another filter higher in the stack managing
				// the space context
				return nextFilter.get();
		};
	}

	public static SpaceContext get() {
		SpaceContext context = threadLocal.get();
		if (context == null)
			throw Exceptions.runtime("no thread local context set");
		return context;
	}

	public static boolean isTest() {
		return get().isTest;
	}

	public static boolean isWww() {
		return backend().webApp();
	}

	public static boolean isDebug() {
		return get().debug.isTrue();
	}

	public static Debug debug() {
		return get().debug;
	}

	public static SpaceBackend backend() {
		SpaceContext context = threadLocal.get();
		return context == null //
				? Start.get().configuration().apiBackend()
				: context.backend;
	}

	public static String backendId() {
		return backend().backendId();
	}

	public boolean isJsonContent() {
		String contentType = context.header(SpaceHeaders.CONTENT_TYPE);
		return SpaceHeaders.isJsonContent(contentType);
	}

	@SuppressWarnings("unchecked")
	public static <T extends Settings> T getSettings(Class<T> settingsId) {
		SpaceContext context = get();
		return context.settings == null ? null //
				: (T) context.settings.get(settingsId);
	}

	public static <T extends Settings> void setSettings(Settings settings) {
		SpaceContext context = get();
		if (context.settings == null)
			context.settings = Maps.newHashMap();
		context.settings.put(settings.getClass(), settings);
	}

	//
	// Check credentials static methods
	//

	public static SpaceFilter checkAuthorizationFilter() {

		return (uri, context, nextFilter) -> {
			get().checkAuthorizationHeader();
			return nextFilter.get();
		};
	}

	public static Credentials credentials() {
		return get().credentials;
	}

	//
	// Implementation
	//

	private void checkAuthorizationHeader() {
		if (!authorizationChecked) {
			authorizationChecked = true;
			SpaceContext.debug().credentialCheck();
			String headerValue = context.header(SpaceHeaders.AUTHORIZATION);

			if (headerValue != null) {
				Credentials userCredentials = null;
				AuthorizationHeader authHeader = new AuthorizationHeader(headerValue, true);

				if (authHeader.isBasic()) {
					userCredentials = CredentialsService.get()//
							.checkUsernamePassword(authHeader.username(), //
									authHeader.password());

				} else if (authHeader.isBearer()) {
					userCredentials = CredentialsService.get()//
							.checkToken(authHeader.token());
				}

				userCredentials.checkReallyEnabled();
				checkPasswordMustChange(userCredentials, context);
				credentials = userCredentials;
			}
		}
	}

	private void checkPasswordMustChange(Credentials credentials, Context context) {
		if (credentials.passwordMustChange()) {

			if (!(Methods.PUT.equals(context.method()) //
					&& "/1/credentials/me/password".equals(uri)))

				throw Exceptions.passwordMustChange(credentials);
		}
	}
}