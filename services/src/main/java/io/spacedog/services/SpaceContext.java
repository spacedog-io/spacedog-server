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

import io.spacedog.utils.Json;
import io.spacedog.utils.Passwords;
import io.spacedog.utils.SpaceHeaders;
import io.spacedog.utils.SpaceParams;
import io.spacedog.utils.Utils;
import net.codestory.http.Context;

/**
 * Context credentials should only be accessed from public static check methods
 * from this class.
 */
public class SpaceContext {

	private static ThreadLocal<SpaceContext> threadLocal = new ThreadLocal<SpaceContext>();

	private Context context;
	private Optional<Credentials> credentials;

	private SpaceContext(Context context) {
		this.context = context;
	}

	public Context context() {
		return context;
	}

	public boolean debug() {
		return context.query().getBoolean(SpaceParams.DEBUG_QUERY_PARAM, false);
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

	public static Credentials checkSuperDogCredentials() throws JsonParseException, JsonMappingException, IOException {
		Credentials credentials = checkCredentials();
		if (credentials.isSuperDogAuthenticated())
			return credentials;
		throw new AuthenticationException("invalid superdog credentials");
	}

	public static Credentials checkSuperDogCredentialsFor(String backendId)
			throws JsonParseException, JsonMappingException, IOException {
		Credentials credentials = checkSuperDogCredentials();
		credentials = Credentials.fromSuperDog(//
				backendId, credentials.name(), credentials.email().get());
		get().credentials = Optional.of(credentials);
		return credentials;
	}

	public static Credentials checkAdminCredentialsFor(String backendId)
			throws JsonParseException, JsonMappingException, IOException {
		Credentials credentials = checkAdminCredentials();
		if (!credentials.backendId().equals(backendId))
			throw new AuthenticationException(
					String.format("invalid administrator credentials for backend [%s]", backendId));
		return credentials;
	}

	public static Credentials checkAdminCredentials() throws JsonParseException, JsonMappingException, IOException {
		Credentials credentials = checkCredentials();
		if (credentials.isAdminAuthenticated())
			return credentials;
		throw new AuthenticationException("invalid administrator credentials");
	}

	public static Credentials checkUserOrAdminCredentials()
			throws JsonParseException, JsonMappingException, IOException {
		Credentials credentials = checkCredentials();
		if (credentials.isAdminAuthenticated() || credentials.isUserAuthenticated())
			return credentials;
		throw new AuthenticationException("invalid user or administrator credentials");
	}

	public static Credentials checkUserCredentials() throws IOException, JsonParseException, JsonMappingException {
		Credentials credentials = checkCredentials();
		if (credentials.isUserAuthenticated())
			return credentials;
		throw new AuthenticationException("invalid user credentials");
	}

	public static Credentials checkCredentials() throws JsonParseException, JsonMappingException, IOException {
		Optional<Credentials> credentials = getOrBuildCredentials();
		if (!credentials.isPresent())
			throw new AuthenticationException("no credentials found");
		return credentials.get();
	}

	public static Optional<Credentials> getOrBuildCredentials()
			throws JsonParseException, JsonMappingException, IOException {
		SpaceContext local = get();
		if (local.credentials == null) {
			Debug.credentialCheck();
			local.credentials = Strings.isNullOrEmpty(local.context.header(SpaceHeaders.BACKEND_KEY))//
					? SpaceContext.buildAdminCredentials(local.context)
					: SpaceContext.buildUserCredentials(local.context);
		}
		return local.credentials;
	}

	public static void setCredentials(Credentials credentials, boolean override)
			throws JsonParseException, JsonMappingException, IOException {
		if (override || !getOrBuildCredentials().isPresent())
			get().credentials = Optional.of(credentials);
	}

	public static Optional<Credentials> getCredentials() {
		SpaceContext local = get();
		return local.credentials == null ? Optional.empty() : local.credentials;
	}

	//
	// Implementation
	//

	// private void buildCredentials() throws IOException, JsonParseException,
	// JsonMappingException {
	// Debug.credentialCheck();
	// credentials =
	// Strings.isNullOrEmpty(context.header(SpaceHeaders.BACKEND_KEY))//
	// ? buildAdminCredentials(context) : buildUserCredentials(context);
	// }

	private static Optional<Credentials> buildUserCredentials(Context context)
			throws IOException, JsonParseException, JsonMappingException {

		String rawBackendKey = context.header(SpaceHeaders.BACKEND_KEY);

		String[] key = rawBackendKey.split(":", 3);

		if (key.length < 3)
			throw new AuthenticationException(
					"malformed backend key [%s], should be <backend-id>:<key-name>:<key-secret>", rawBackendKey);

		String backendId = key[0];
		String keyName = key[1];
		String keySecret = key[2];

		if (Strings.isNullOrEmpty(backendId))
			throw new AuthenticationException("invalid backend key [%s], no backend id specified", rawBackendKey);

		if (AccountResource.INTERNAL_INDICES.contains(backendId))
			throw new AuthenticationException("this backend id [%s] is reserved", backendId);

		if (Strings.isNullOrEmpty(keyName))
			throw new AuthenticationException("invalid backend key [%s], no key name specified", rawBackendKey);

		if (Strings.isNullOrEmpty(keySecret))
			throw new AuthenticationException("invalid backend key [%s], no key secret specified", rawBackendKey);

		// check backend key name/secret pairs in spacedog index account objects
		// TODO why don't we just get the account with the backend id ?
		// TODO an optim would be to ask the search method not to fetch object
		// sources since we don't need them here

		SearchHits accountHits = ElasticHelper.get().search(AccountResource.ADMIN_INDEX, AccountResource.ACCOUNT_TYPE,
				"backendId", backendId, "backendKey.name", keyName, "backendKey.secret", keySecret);

		if (accountHits.getTotalHits() == 0)
			throw new AuthenticationException("invalid backend key [%s]", rawBackendKey);

		if (accountHits.getTotalHits() > 1)
			throw new RuntimeException(String.format("more than one backend key for backend id [%s] and key name [%s]",
					backendId, keyName));

		Optional<String[]> tokens = decodeAuthorizationHeader(context.header(SpaceHeaders.AUTHORIZATION));

		if (tokens.isPresent()) {

			String username = tokens.get()[0];
			String password = tokens.get()[1];

			// check users in specific backend index

			Optional<ObjectNode> user = ElasticHelper.get()//
					.getObject(backendId, UserResource.USER_TYPE, username);

			if (user.isPresent()) {
				String providedPassword = Passwords.hash(password);
				JsonNode expectedPassword = user.get().get(UserResource.HASHED_PASSWORD);
				if (!Json.isNull(expectedPassword) && providedPassword.equals(expectedPassword.asText()))
					return Optional.of(//
							Credentials.fromUser(backendId, username, //
									user.get().get(UserResource.EMAIL).asText()));
			}

			throw new AuthenticationException("invalid username or password");

		} else {

			Account account = Json.getMapper().readValue(//
					accountHits.getAt(0).getSourceAsString(), Account.class);

			return Optional.of(//
					Credentials.fromKey(backendId, account.backendKey));
		}
	}

	private static Optional<Credentials> buildAdminCredentials(Context context)
			throws JsonParseException, JsonMappingException, IOException {

		Optional<String[]> tokens = decodeAuthorizationHeader(context.header(SpaceHeaders.AUTHORIZATION));

		if (tokens.isPresent()) {

			String username = tokens.get()[0];
			String password = tokens.get()[1];

			// check if superdog credentials

			Optional<String> hashedPassword = Start.get()//
					.configuration().getSuperDogHashedPassword(username);

			if (hashedPassword.isPresent()) {

				if (Passwords.hash(password).equals(hashedPassword.get()))
					return Optional.of(//
							Credentials.fromSuperDog(username, //
									Start.get().configuration().getSuperDogEmail(username).get()));

				throw new AuthenticationException("invalid superdog username or password");
			}

			// check admin users in spacedog index

			SearchHits accountHits = ElasticHelper.get().search(AccountResource.ADMIN_INDEX,
					AccountResource.ACCOUNT_TYPE, "username", username, "hashedPassword", Passwords.hash(password));

			if (accountHits.getTotalHits() == 0)
				throw new AuthenticationException("invalid administrator username or password");

			if (accountHits.getTotalHits() > 1)
				throw new RuntimeException(
						String.format("more than one admin user with username [%s]", tokens.get()[0]));

			Account account = Json.getMapper().readValue(accountHits.getAt(0).getSourceAsString(), Account.class);
			return Optional.of(//
					Credentials.fromAdmin(account.backendId, account.username, account.email, account.backendKey));

		} else
			return Optional.empty();
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
			throw new AuthenticationException("authorization token is not base 64 encoded", e);
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