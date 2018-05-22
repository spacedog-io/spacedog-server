package io.spacedog.server;

import java.util.Map;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.http.SpaceBackend;
import io.spacedog.client.http.SpaceHeaders;
import io.spacedog.client.http.SpaceParams;
import io.spacedog.client.settings.Settings;
import io.spacedog.utils.AuthorizationHeader;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Optional7;
import net.codestory.http.Request;
import net.codestory.http.Response;
import net.codestory.http.constants.Methods;

/**
 * Context credentials should only be accessed from public static check methods
 * from this class.
 */
public class SpaceContext {

	private Request request;
	private Response response;
	private SpaceBackend backend;
	private Debug debug;
	private Credentials credentials;
	private boolean authorizationChecked;
	private boolean isTest = false;
	private boolean isWww = false;

	private Map<Class<?>, Settings> settings;

	public SpaceContext(Request request, Response response) {
		this.request = request;
		this.response = response;
		this.isTest = Boolean.parseBoolean(//
				request.header(SpaceHeaders.SPACEDOG_TEST));
		this.debug = new Debug(Boolean.parseBoolean(//
				request.header(SpaceHeaders.SPACEDOG_DEBUG)));
		this.credentials = Credentials.GUEST;
		initSpaceBackend();
	}

	private void initSpaceBackend() {

		String hostAndPort = request.header(SpaceHeaders.HOST);

		// first try to match api backend
		SpaceBackend apiBackend = ServerConfig.apiBackend();
		Optional7<SpaceBackend> opt = apiBackend.fromRequest(hostAndPort);

		if (opt.isPresent())
			this.backend = opt.get();

		else {
			// second try to match www backend
			SpaceBackend wwwBackend = ServerConfig.wwwBackend();
			opt = wwwBackend.fromRequest(hostAndPort);

			if (opt.isPresent()) {
				this.backend = opt.get();
				this.isWww = true;

			} else
				throw Exceptions.illegalArgument(//
						"host [%s] is invalid", hostAndPort);
		}
	}

	//
	// Getters and setters
	//

	public Request request() {
		return request;
	}

	public Response response() {
		return response;
	}

	public boolean isTest() {
		return isTest;
	}

	public boolean isWww() {
		return isWww;
	}

	public Debug debug() {
		return debug;
	}

	public SpaceBackend backend() {
		return backend;
	}

	public boolean isJsonContent() {
		String contentType = request.header(SpaceHeaders.CONTENT_TYPE);
		return SpaceHeaders.isJsonContent(contentType);
	}

	@SuppressWarnings("unchecked")
	public <T extends Settings> T getSettings(Class<T> settingsId) {
		return settings == null ? null : (T) settings.get(settingsId);
	}

	public <T extends Settings> void setSettings(Settings settings) {
		if (this.settings == null)
			this.settings = Maps.newHashMap();
		this.settings.put(settings.getClass(), settings);
	}

	//
	// Check credentials
	//

	public static SpaceFilter checkAuthorizationFilter() {

		return (uri, context, nextFilter) -> {
			Server.context().checkAuthorizationHeader();
			return nextFilter.get();
		};
	}

	public Credentials credentials() {
		return credentials;
	}

	private void checkAuthorizationHeader() {

		if (!authorizationChecked) {
			authorizationChecked = true;
			Credentials userCredentials = null;
			debug().credentialCheck();
			String headerValue = request.header(SpaceHeaders.AUTHORIZATION);

			if (headerValue == null) {
				String token = request.query().get(SpaceParams.ACCESS_TOKEN_PARAM);
				if (!Strings.isNullOrEmpty(token))
					userCredentials = checkAccessToken(token);

			} else {
				AuthorizationHeader authHeader = new AuthorizationHeader(headerValue, true);

				if (authHeader.isBasic()) {
					userCredentials = CredentialsService.get()//
							.checkUsernamePassword(authHeader.username(), //
									authHeader.password());

				} else if (authHeader.isBearer())
					userCredentials = checkAccessToken(authHeader.token());
			}

			if (userCredentials != null) {
				userCredentials.checkReallyEnabled();
				checkPasswordMustChange(userCredentials);
				credentials = userCredentials;
			}
		}
	}

	private Credentials checkAccessToken(String token) {
		return CredentialsService.get().checkToken(token);
	}

	private void checkPasswordMustChange(Credentials credentials) {
		if (credentials.passwordMustChange()) {

			if ((request.uri().equals("/1/credentials/me/_set_password")//
					&& request.method().equals(Methods.POST)) == false)

				throw Exceptions.passwordMustChange(credentials);
		}
	}

	//
	// Other
	//

	public void runAsBackend(String backendId, Runnable action) {
		SpaceBackend mainBackend = this.backend;
		SpaceBackend tempBackend = mainBackend.fromBackendId(backendId);

		try {
			this.backend = tempBackend;
			action.run();
		} finally {
			this.backend = mainBackend;
		}
	}
}