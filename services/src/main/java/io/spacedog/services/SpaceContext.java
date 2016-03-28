package io.spacedog.services;

import java.util.Base64;
import java.util.Optional;

import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;

import io.spacedog.utils.BackendKey;
import io.spacedog.utils.Exceptions;
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
	private String subdomain;
	private Optional<Credentials> credentials;

	private SpaceContext(Context context) {
		this.context = context;
		this.isTest = Boolean.parseBoolean(context().header(SpaceHeaders.SPACEDOG_TEST));
		this.isDebug = Boolean.parseBoolean(context().header(SpaceHeaders.SPACEDOG_DEBUG));
		extractSubdomain(context);
	}

	private void extractSubdomain(Context context) {
		String host = context.request().header(HttpHeaders.HOST);
		String[] terms = host.split("\\.");
		if (terms.length != 3)
			throw Exceptions.illegalArgument("host [%s] not valid: missing backend subdomain", host);
		this.subdomain = terms[0];
	}

	public static String subdomain() {
		return get().subdomain;
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

	//
	// Check credentials static methods
	//

	public static Credentials checkSuperDogCredentials() {
		return checkSuperDogCredentials(true);
	}

	public static Credentials checkSuperDogCredentials(boolean checkCustomerBackend) {
		Credentials credentials = checkCredentials(checkCustomerBackend);
		if (credentials.isSuperDog())
			return credentials;
		throw new AuthorizationException("invalid superdog credentials of name [%s] and type [%s]", credentials.name(),
				credentials.level());
	}

	public static Credentials checkSuperAdminCredentials() {
		return checkSuperAdminCredentials(true);
	}

	public static Credentials checkSuperAdminCredentials(boolean checkCustomerBackend) {
		Credentials credentials = checkCredentials(checkCustomerBackend);
		if (credentials.isAtLeastSuperAdmin())
			return credentials;
		throw new AuthorizationException("invalid super administrator credentials of name [%s] and type [%s]",
				credentials.name(), credentials.level());
	}

	public static Credentials checkAdminCredentials() {
		return checkAdminCredentials(true);
	}

	public static Credentials checkAdminCredentials(boolean checkCustomerBackend) {
		Credentials credentials = checkCredentials(checkCustomerBackend);
		if (credentials.isAtLeastAdmin())
			return credentials;
		throw new AuthorizationException("invalid administrator credentials of name [%s] and type [%s]",
				credentials.name(), credentials.level());
	}

	public static Credentials checkUserOrAdminCredentials() {
		return checkUserOrAdminCredentials(true);
	}

	public static Credentials checkUserOrAdminCredentials(boolean checkCustomerBackend) {
		Credentials credentials = checkCredentials(checkCustomerBackend);
		if (credentials.isAtLeastAdmin() || credentials.isAtLeastUser())
			return credentials;
		throw new AuthorizationException("invalid user or administrator credentials of name [%s] and type [%s]",
				credentials.name(), credentials.level());
	}

	public static Credentials checkUserCredentials() {
		return checkUserCredentials(true);
	}

	public static Credentials checkUserCredentials(String username) {
		Credentials credentials = checkUserCredentials(true);

		if (credentials.isAtLeastAdmin() || credentials.name().equals(username))
			return credentials;

		throw new AuthorizationException(credentials);
	}

	public static Credentials checkUserCredentials(boolean checkCustomerBackend) {
		Credentials credentials = checkCredentials(checkCustomerBackend);
		if (credentials.isAtLeastUser())
			return credentials;
		throw new AuthorizationException("invalid user credentials of name [%s] and type [%s]", credentials.name(),
				credentials.level());
	}

	public static Credentials checkCredentials() {
		return checkCredentials(true);
	}

	public static Credentials checkCredentials(boolean checkCustomerBackend) {
		Optional<Credentials> credentials = buildCredentials();
		if (!credentials.isPresent())
			throw new AuthorizationException("no credentials found");
		if (checkCustomerBackend //
				&& credentials.get().isRootBackend())
			throw new AuthorizationException(//
					"no customer backend subdomain found");
		return credentials.get();
	}

	public static void setCredentials(Credentials credentials) {
		get().credentials = Optional.of(credentials);
	}

	public static Optional<Credentials> getCredentials() {
		SpaceContext local = get();
		return local.credentials == null ? Optional.empty() : local.credentials;
	}

	public static Optional<Credentials> buildCredentials() {
		SpaceContext local = get();
		if (local.credentials == null) {
			Debug.credentialCheck();
			local.credentials = buildNewGenCredentials(local.context);
		}
		return local.credentials;
	}

	//
	// Implementation
	//

	private static Optional<Credentials> buildNewGenCredentials(Context context) {

		String backendId = subdomain();
		Optional<String[]> tokens = decodeAuthorizationHeader(context.header(SpaceHeaders.AUTHORIZATION));

		if (tokens.isPresent()) {

			String username = tokens.get()[0];
			String password = tokens.get()[1];

			String backendToCheck = username.startsWith("superdog-") ? BackendKey.ROOT_API : backendId;
			Optional<Credentials> credentials = CredentialsResource.get().check(backendToCheck, username, password);

			if (credentials.isPresent()) {
				// force to requested backendId for superdogs
				credentials.get().backendId(backendId);
				return credentials;
			} else
				throw new AuthorizationException("invalid username or password");
		}

		return Optional.of(new Credentials(backendId));
	}

	public static Optional<String[]> decodeAuthorizationHeader(String authzHeaderValue) {

		if (Strings.isNullOrEmpty(authzHeaderValue))
			return Optional.empty();

		String[] schemeAndTokens = authzHeaderValue.split(" ", 2);

		if (schemeAndTokens.length != 2)
			throw new AuthorizationException("invalid authorization header");

		if (Strings.isNullOrEmpty(schemeAndTokens[0]))
			throw new AuthorizationException("no authorization scheme specified");

		if (!schemeAndTokens[0].equalsIgnoreCase(SpaceHeaders.BASIC_SCHEME))
			throw new AuthorizationException("authorization scheme [%s] not supported", schemeAndTokens[0]);

		byte[] encodedBytes = schemeAndTokens[1].getBytes(Utils.UTF8);

		String decoded = null;

		try {
			decoded = new String(Base64.getDecoder().decode(encodedBytes));
		} catch (IllegalArgumentException e) {
			throw new AuthorizationException("authorization token is not base 64 encoded", e);
		}

		String[] tokens = decoded.split(":", 2);

		if (tokens.length != 2)
			throw new AuthorizationException("invalid authorization token");

		if (Strings.isNullOrEmpty(tokens[0]))
			throw new AuthorizationException("no username specified");

		if (Strings.isNullOrEmpty(tokens[1]))
			throw new AuthorizationException("no password specified");

		return Optional.of(tokens);
	}
}