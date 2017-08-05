package io.spacedog.services;

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Maps;
import com.google.common.net.HttpHeaders;

import io.spacedog.rest.SpaceBackend;
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
	private Map<String, String> settings;

	private SpaceContext(String uri, Context context) {
		this.uri = uri;
		this.context = context;

		this.isTest = Boolean.parseBoolean(//
				context().header(SpaceHeaders.SPACEDOG_TEST));
		this.debug = new Debug(Boolean.parseBoolean(//
				context().header(SpaceHeaders.SPACEDOG_DEBUG)));

		String hostAndPort = context.request().header(HttpHeaders.HOST);
		ServerConfiguration conf = Start.get().configuration();

		// first try to match api backend
		SpaceBackend apiBackend = conf.apiBackend();
		Optional7<SpaceBackend> optBackend = apiBackend.fromHostAndPort(hostAndPort);
		if (optBackend.isPresent())
			this.backend = optBackend.get();

		// second try to match webapp backend
		else {
			Optional<SpaceBackend> wwwBackend = conf.wwwBackend();
			if (wwwBackend.isPresent()) {
				optBackend = wwwBackend.get().fromHostAndPort(hostAndPort);
				if (optBackend.isPresent())
					this.backend = optBackend.get();
			}
		}

		// if none matched, use api backend
		if (this.backend == null)
			this.backend = apiBackend;

		this.credentials = new Credentials(this.backend.backendId());
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
					// } catch (Throwable t) {
					// return toPayload(t);
				} finally {
					threadLocal.set(null);
				}
			} else
				// means there is another filter higher in the stack managing
				// the space context
				return nextFilter.get();
		};
	}

	// private static Payload toPayload(Throwable t) {
	// ObjectNode node = Json8.object("success", false, "status", 400, "error", //
	// Json8.object("message", t.getMessage()));
	// return new Payload(Json7.JSON_CONTENT_UTF8, node, 400);
	// }

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
		return get().backend.webApp();
	}

	public static boolean isDebug() {
		return get().debug.isTrue();
	}

	public static Debug debug() {
		return get().debug;
	}

	public static String backendId() {
		return credentials().backendId();
	}

	public boolean isJsonContent() {
		String contentType = context.header(SpaceHeaders.CONTENT_TYPE);
		return SpaceHeaders.isJsonContent(contentType);
	}

	public static String getSettings(String id) {

		SpaceContext context = get();
		if (context.settings == null)
			return null;

		return context.settings.get(id);
	}

	public static void setSettings(String id, String settings) {
		SpaceContext context = get();
		if (context.settings == null)
			context.settings = Maps.newHashMap();

		context.settings.put(id, settings);
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
					userCredentials = CredentialsResource.get()//
							.checkUsernamePassword(authHeader.username(), //
									authHeader.password());

				} else if (authHeader.isBearer()) {
					userCredentials = CredentialsResource.get()//
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