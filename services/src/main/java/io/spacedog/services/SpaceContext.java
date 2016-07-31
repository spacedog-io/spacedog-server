package io.spacedog.services;

import java.util.Base64;
import java.util.Optional;

import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;

import io.spacedog.utils.AuthenticationException;
import io.spacedog.utils.Backends;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.ForbiddenException;
import io.spacedog.utils.SpaceHeaders;
import io.spacedog.utils.Utils;
import net.codestory.http.Context;

/**
 * Context credentials should only be accessed from public static check methods
 * from this class.
 */
public class SpaceContext {

	private static ThreadLocal<SpaceContext> threadLocal = new ThreadLocal<SpaceContext>();

	private Context context;
	private boolean isTest;
	private boolean isDebug;
	private Credentials credentials;
	private boolean authorizationChecked;

	private SpaceContext(Context context) {
		this.context = context;
		this.isTest = Boolean.parseBoolean(context().header(SpaceHeaders.SPACEDOG_TEST));
		this.isDebug = Boolean.parseBoolean(context().header(SpaceHeaders.SPACEDOG_DEBUG));
		this.credentials = new Credentials(extractSubdomain(context));
	}

	public Context context() {
		return context;
	}

	public static SpaceFilter filter() {

		// uri is already checked by SpaceFilter default matches method

		return (uri, context, nextFilter) -> {
			if (threadLocal.get() == null) {
				try {
					init(context);
					return nextFilter.get();

				} finally {
					reset();
				}
			} else
				// means there is another filter higher in the stack managing
				// the space context
				return nextFilter.get();
		};
	}

	public static void reset() {
		threadLocal.set(null);
	}

	public static void init(Context context) {
		threadLocal.set(new SpaceContext(context));
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

	public static boolean isDebug() {
		return get().isDebug;
	}

	public static String backendId() {
		return getCredentials().backendId();
	}

	//
	// Check credentials static methods
	//

	public static SpaceFilter checkAuthorizationFilter() {

		return (uri, context, nextFilter) -> {
			get().checkAuthorization();
			return nextFilter.get();
		};
	}

	public static Credentials checkSuperDogCredentials() {
		return checkSuperDogCredentials(true);
	}

	public static Credentials checkSuperDogCredentials(boolean checkCustomerBackend) {
		Credentials credentials = checkCredentials(checkCustomerBackend);
		if (credentials.isSuperDog())
			return credentials;
		throw insufficientCredentials(credentials);
	}

	public static Credentials checkSuperAdminCredentials() {
		return checkSuperAdminCredentials(true);
	}

	public static Credentials checkSuperAdminCredentials(boolean checkCustomerBackend) {
		Credentials credentials = checkCredentials(checkCustomerBackend);
		if (credentials.isAtLeastSuperAdmin())
			return credentials;
		throw insufficientCredentials(credentials);
	}

	public static Credentials checkAdminCredentials() {
		return checkAdminCredentials(true);
	}

	public static Credentials checkAdminCredentials(boolean checkCustomerBackend) {
		Credentials credentials = checkCredentials(checkCustomerBackend);
		if (credentials.isAtLeastAdmin())
			return credentials;
		throw insufficientCredentials(credentials);
	}

	public static Credentials checkUserOrAdminCredentials() {
		return checkUserOrAdminCredentials(true);
	}

	public static Credentials checkUserOrAdminCredentials(boolean checkCustomerBackend) {
		Credentials credentials = checkCredentials(checkCustomerBackend);
		if (credentials.isAtLeastAdmin() || credentials.isAtLeastUser())
			return credentials;
		throw insufficientCredentials(credentials);
	}

	public static Credentials checkUserCredentials() {
		return checkUserCredentials(true);
	}

	public static Credentials checkUserCredentials(String username) {
		Credentials credentials = checkUserCredentials(true);

		if (credentials.isAtLeastAdmin() || credentials.name().equals(username))
			return credentials;

		throw insufficientCredentials(credentials);
	}

	public static Credentials checkUserCredentials(boolean checkCustomerBackend) {
		Credentials credentials = checkCredentials(checkCustomerBackend);
		if (credentials.isAtLeastUser())
			return credentials;
		throw insufficientCredentials(credentials);
	}

	public static Credentials checkCredentials() {
		return checkCredentials(true);
	}

	public static Credentials checkCredentials(boolean checkCustomerBackend) {
		Credentials credentials = getCredentials();
		if (checkCustomerBackend && credentials.isRootBackend())
			throw Exceptions.illegalArgument("host doesn't specify any backend id");
		return credentials;
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

	private static ForbiddenException insufficientCredentials(Credentials credentials) {
		return Exceptions.forbidden("[%s][%s] has insufficient credentials", //
				credentials.level(), credentials.name());
	}

	private String extractSubdomain(Context context) {
		String host = context.request().header(HttpHeaders.HOST);
		String[] terms = host.split("\\.");
		return terms.length == 3 ? terms[0] : Backends.ROOT_API;
	}

	private void checkAuthorization() {
		if (!authorizationChecked) {
			authorizationChecked = true;
			Debug.credentialCheck();

			Optional<String[]> tokens = decodeAuthorizationHeader(context.header(SpaceHeaders.AUTHORIZATION));

			if (tokens.isPresent()) {

				String username = tokens.get()[0];
				String password = tokens.get()[1];
				boolean superdog = username.startsWith("superdog-");
				String backendId = credentials.backendId();

				Optional<Credentials> userCredentials = CredentialsResource.get().check(//
						superdog ? Backends.ROOT_API : backendId, //
						username, password);

				if (userCredentials.isPresent()) {
					credentials = userCredentials.get();
					if (superdog)
						credentials.backendId(backendId);
				} else
					throw new AuthenticationException("invalid username or password for backend [%s]", backendId);
			}
		}
	}

	public static Optional<String[]> decodeAuthorizationHeader(String authzHeaderValue) {

		if (Strings.isNullOrEmpty(authzHeaderValue))
			return Optional.empty();

		String[] schemeAndTokens = authzHeaderValue.split(" ", 2);

		if (schemeAndTokens.length != 2)
			throw new AuthenticationException("invalid authorization header");

		if (Strings.isNullOrEmpty(schemeAndTokens[0]))
			throw new AuthenticationException("no authorization scheme specified");

		if (!schemeAndTokens[0].equalsIgnoreCase(SpaceHeaders.BASIC_SCHEME))
			throw new AuthenticationException("authorization scheme [%s] not supported", schemeAndTokens[0]);

		byte[] encodedBytes = schemeAndTokens[1].getBytes(Utils.UTF8);

		String decoded = null;

		try {
			decoded = new String(Base64.getDecoder().decode(encodedBytes));
		} catch (IllegalArgumentException e) {
			throw new AuthenticationException(e, "authorization token is not base 64 encoded");
		}

		String[] tokens = decoded.split(":", 2);

		if (tokens.length != 2)
			throw new AuthenticationException("invalid authorization token");

		if (Strings.isNullOrEmpty(tokens[0]))
			throw new AuthenticationException("no username specified");

		if (Strings.isNullOrEmpty(tokens[1]))
			throw new AuthenticationException("no password specified");

		return Optional.of(tokens);
	}
}