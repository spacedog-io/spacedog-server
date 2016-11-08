package io.spacedog.services;

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Maps;
import com.google.common.net.HttpHeaders;

import io.spacedog.utils.AuthenticationException;
import io.spacedog.utils.AuthorizationHeader;
import io.spacedog.utils.Backends;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Settings;
import io.spacedog.utils.SpaceHeaders;
import net.codestory.http.Context;

/**
 * Context credentials should only be accessed from public static check methods
 * from this class.
 */
public class SpaceContext {

	private static ThreadLocal<SpaceContext> threadLocal = new ThreadLocal<SpaceContext>();

	private Context context;
	private boolean isTest;
	private Debug debug;
	private Credentials credentials;
	private boolean authorizationChecked;
	private boolean isForced;
	private Map<String, Settings> settings;
	private boolean www;

	private SpaceContext(Context context) {
		this.context = context;
		this.isTest = Boolean.parseBoolean(context().header(SpaceHeaders.SPACEDOG_TEST));
		String[] host = extractSubdomain(context);
		this.credentials = new Credentials(host[0]);
		www = host.length > 2 && "www".equals(host[1]);
		this.debug = new Debug(//
				Boolean.parseBoolean(context().header(SpaceHeaders.SPACEDOG_DEBUG)));
	}

	private SpaceContext(Credentials credentials, boolean test, boolean debug) {
		this.isForced = true;
		this.isTest = test;
		this.credentials = credentials;
		this.debug = new Debug(debug);
	}

	public Context context() {
		return context;
	}

	public static boolean isSetAuthorized() {
		SpaceContext spaceContext = threadLocal.get();
		return spaceContext == null || spaceContext.isForced;
	}

	public static SpaceFilter filter() {

		// uri is already checked by SpaceFilter default matches method

		return (uri, context, nextFilter) -> {
			if (isSetAuthorized()) {
				try {
					threadLocal.set(new SpaceContext(context));
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
		return get().www;
	}

	public static boolean isDebug() {
		return get().debug.isTrue();
	}

	public static Debug debug() {
		return get().debug;
	}

	public static String backendId() {
		return getCredentials().backendId();
	}

	static void forceContext(Credentials credentials, boolean test, boolean debug) {
		if (isSetAuthorized())
			threadLocal.set(new SpaceContext(credentials, test, debug));
		else
			throw Exceptions.runtime("overriding non null context is illegal");
	}

	@SuppressWarnings("unchecked")
	public static <K extends Settings> K getCachedSettings(Class<K> settingsClass) {

		SpaceContext context = get();
		if (context.settings == null)
			return null;

		return (K) context.settings.get(Settings.id(settingsClass));
	}

	public static void setCachedSettings(Settings settings) {
		SpaceContext context = get();
		if (context.settings == null)
			context.settings = Maps.newHashMap();

		context.settings.put(settings.id(), settings);
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

	public static Credentials checkSuperDogCredentials() {
		Credentials credentials = getCredentials();
		if (credentials.isSuperDog())
			return credentials;
		throw Exceptions.insufficientCredentials(credentials);
	}

	public static Credentials checkSuperAdminCredentials() {
		Credentials credentials = getCredentials();
		if (credentials.isAtLeastSuperAdmin())
			return credentials;
		throw Exceptions.insufficientCredentials(credentials);
	}

	public static Credentials checkAdminCredentials() {
		Credentials credentials = getCredentials();
		if (credentials.isAtLeastAdmin())
			return credentials;
		throw Exceptions.insufficientCredentials(credentials);
	}

	public static Credentials checkUserCredentials(String credentialsId) {
		Credentials credentials = checkUserCredentials();

		if (credentials.isAtLeastAdmin() || credentials.id().equals(credentialsId))
			return credentials;

		throw Exceptions.insufficientCredentials(credentials);
	}

	public static Credentials checkUserCredentials() {
		Credentials credentials = getCredentials();
		if (credentials.isAtLeastUser())
			return credentials;
		throw Exceptions.insufficientCredentials(credentials);
	}

	public static void checkPasswordHasBeenChallenged() {
		if (!getCredentials().isPasswordChecked())
			throw Exceptions.passwordMustBeChallenged();
	}

	public static void setCredentials(Credentials credentials) {
		get().credentials = credentials;
	}

	public static Credentials getCredentials() {
		return get().credentials;
	}

	//
	// Implementation
	//

	private String[] extractSubdomain(Context context) {
		String urlBase = Start.get().configuration().apiUrlBase();
		String host = context.request().header(HttpHeaders.HOST);
		return host.endsWith(urlBase) //
				? host.split("\\.")//
				: new String[] { Backends.ROOT_API };
	}

	private void checkAuthorizationHeader() {
		if (!authorizationChecked) {
			authorizationChecked = true;
			SpaceContext.debug().credentialCheck();
			String backendId = backendId();
			String headerValue = context.header(SpaceHeaders.AUTHORIZATION);

			if (headerValue != null) {
				boolean superdog = false;
				Optional<Credentials> userCredentials = Optional.empty();
				AuthorizationHeader authHeader = new AuthorizationHeader(headerValue);
				if (authHeader.isBasic()) {
					superdog = authHeader.username().startsWith("superdog-");

					userCredentials = CredentialsResource.get()//
							.checkUsernamePassword(//
									superdog ? Backends.ROOT_API : backendId, //
									authHeader.username(), authHeader.password());

					if (!userCredentials.isPresent())
						throw new AuthenticationException("invalid username or password for backend [%s]", backendId);

				} else if (authHeader.isBearer()) {
					userCredentials = CredentialsResource.get()//
							.checkToken(backendId, authHeader.token());

					if (!userCredentials.isPresent())
						throw new AuthenticationException("invalid access token for backend [%s]", backendId);
				}

				if (!userCredentials.get().enabled())
					throw Exceptions.disabledCredentials(userCredentials.get());

				credentials = userCredentials.get();

				// sets superdog backend id to the request backend id
				if (superdog)
					credentials.backendId(backendId);
			}

		}
	}
}