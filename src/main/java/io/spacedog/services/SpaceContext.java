package io.spacedog.services;

import java.io.IOException;
import java.util.Base64;
import java.util.Optional;

import org.elasticsearch.search.SearchHits;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import net.codestory.http.Context;

/**
 * Context credentials should only be accessed from public static check methods
 * from this class.
 */
public class SpaceContext {

	public static final String BASIC_AUTHENTICATION_SCHEME = "Basic";
	public static final String AUTHORIZATION_HEADER = "Authorization";
	public static final String BACKEND_KEY_HEADER = "x-spacedog-backend-key";

	private static ThreadLocal<SpaceContext> threadLocal = new ThreadLocal<SpaceContext>();

	private Context context;
	private Credentials credentials;

	private SpaceContext(Context context) {
		this.context = context;
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
			throw new RuntimeException("unexpected error: no thread local context set");
		return context;
	}

	//
	// Check credentials static methods
	//

	public static Credentials checkAdminCredentials() throws JsonParseException, JsonMappingException, IOException {
		Credentials credentials = checkCredentials();
		if (credentials.isAdminAuthenticated())
			return credentials;
		throw new AuthenticationException("invalid administrator credentials");
	}

	public static Credentials checkAdminCredentialsFor(String backendId)
			throws JsonParseException, JsonMappingException, IOException {
		Credentials credentials = checkAdminCredentials();
		if (!credentials.backendId().equals(backendId))
			throw new AuthenticationException(
					String.format("invalid administrator credentials for backend [%s]", backendId));
		return credentials;
	}

	public static Credentials checkUserCredentials() throws IOException, JsonParseException, JsonMappingException {
		Credentials credentials = checkCredentials();
		if (credentials.isUserAuthenticated())
			return credentials;
		throw new AuthenticationException("invalid user credentials");
	}

	public static Credentials checkCredentials() throws JsonParseException, JsonMappingException, IOException {
		SpaceContext local = get();
		if (local.credentials == null)
			local.buildCredentials();
		return local.credentials;
	}

	//
	// Implementation
	//

	private void buildCredentials() throws IOException, JsonParseException, JsonMappingException {

		credentials = Strings.isNullOrEmpty(context.header(BACKEND_KEY_HEADER))//
				? buildAdminCredentials() : buildUserCredentials();
	}

	private Credentials buildUserCredentials() throws IOException, JsonParseException, JsonMappingException {

		String rawBackendKey = context.header(BACKEND_KEY_HEADER);

		String[] key = rawBackendKey.split(":", 3);

		if (key.length < 3)
			throw new AuthenticationException(
					"malformed backend key [%s], should be <backend-id>:<key-name>:<key-secret>", rawBackendKey);

		String backendId = key[0];
		String keyName = key[1];
		String keySecret = key[2];

		if (Strings.isNullOrEmpty(backendId))
			throw new AuthenticationException("invalid backend key [%s], no backend id specified", rawBackendKey);

		if (AdminResource.INTERNAL_INDICES.contains(backendId))
			throw new AuthenticationException("this backend id [%s] is reserved", backendId);

		if (Strings.isNullOrEmpty(keyName))
			throw new AuthenticationException("invalid backend key [%s], no key name specified", rawBackendKey);

		if (Strings.isNullOrEmpty(keySecret))
			throw new AuthenticationException("invalid backend key [%s], no key secret specified", rawBackendKey);

		// check backend key name/secret pairs in spacedog index account objects
		// TODO why don't we just get the account with the backend id ?
		// TODO an optim would be to ask the search method not to fetch object
		// sources since we don't need them here

		SearchHits accountHits = ElasticHelper.get().search(AdminResource.ADMIN_INDEX, AdminResource.ACCOUNT_TYPE,
				"backendId", backendId, "backendKey.name", keyName, "backendKey.secret", keySecret);

		if (accountHits.getTotalHits() == 0)
			throw new AuthenticationException("invalid backend key [%s]", rawBackendKey);

		if (accountHits.getTotalHits() > 1)
			throw new RuntimeException(String.format("more than one backend key for backend id [%s] and key name [%s]",
					backendId, keyName));

		Optional<String[]> tokens = decodeAuthorizationHeader(context.header(AUTHORIZATION_HEADER));

		if (tokens.isPresent()) {

			// check users in specific backend index

			Optional<ObjectNode> user = ElasticHelper.get().getObject(backendId, UserResource.USER_TYPE,
					tokens.get()[0]);

			if (user.isPresent()) {
				String providedPassword = UserUtils.hashPassword(tokens.get()[1]);
				JsonNode expectedPassword = user.get().get("hashedPassword");
				if (!Json.isNull(expectedPassword) && providedPassword.equals(expectedPassword.asText()))
					return Credentials.fromUser(backendId, tokens.get()[0]);
			}

			throw new AuthenticationException("invalid username or password");

		} else {

			Account account = Json.getMapper().readValue(accountHits.getAt(0).getSourceAsString(), Account.class);
			return Credentials.fromKey(backendId, account.backendKey);
		}
	}

	private Credentials buildAdminCredentials() throws JsonParseException, JsonMappingException, IOException {

		Optional<String[]> tokens = decodeAuthorizationHeader(context.header(SpaceContext.AUTHORIZATION_HEADER));

		if (tokens.isPresent()) {

			// check admin users in spacedog index

			SearchHits accountHits = ElasticHelper.get().search(AdminResource.ADMIN_INDEX, AdminResource.ACCOUNT_TYPE,
					"username", tokens.get()[0], "hashedPassword", UserUtils.hashPassword(tokens.get()[1]));

			if (accountHits.getTotalHits() == 0)
				throw new AuthenticationException("invalid administrator username or password");

			if (accountHits.getTotalHits() > 1)
				throw new RuntimeException(
						String.format("more than one admin user with username [%s]", tokens.get()[0]));

			Account account = Json.getMapper().readValue(accountHits.getAt(0).getSourceAsString(), Account.class);
			return Credentials.fromAdmin(account.backendId, account.username, account.backendKey);

		} else
			throw new AuthenticationException("no 'Authorization' header found");
	}

	public static Optional<String[]> decodeAuthorizationHeader(String authzHeaderValue) {

		if (Strings.isNullOrEmpty(authzHeaderValue))
			return Optional.empty();

		String[] schemeAndTokens = authzHeaderValue.split(" ", 2);

		if (schemeAndTokens.length != 2)
			throw new AuthenticationException("invalid authorization header");

		if (Strings.isNullOrEmpty(schemeAndTokens[0]))
			throw new AuthenticationException("no authorization scheme specified");

		if (!schemeAndTokens[0].equalsIgnoreCase(SpaceContext.BASIC_AUTHENTICATION_SCHEME))
			throw new AuthenticationException("authorization scheme [%s] not supported", schemeAndTokens[0]);

		byte[] encodedBytes = schemeAndTokens[1].getBytes(Utils.UTF8);

		String decoded = null;

		try {
			decoded = new String(Base64.getDecoder().decode(encodedBytes));
		} catch (IllegalArgumentException e) {
			throw new AuthenticationException("authorization token is not base 64 encoded", e);
		}

		String[] tokens = decoded.split(":", 2);

		if (tokens.length != 2)
			throw new AuthenticationException("invalid authorization token");

		if (Strings.isNullOrEmpty(tokens[1]))
			throw new AuthenticationException("no password specified");

		return Optional.of(tokens);
	}
}