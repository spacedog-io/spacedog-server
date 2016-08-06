/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.util.Base64;
import java.util.List;

import com.google.common.base.Strings;

public class AuthorizationHeader {

	private String scheme;
	private String username;
	private String password;
	private String token;

	public static boolean isKey(String key) {
		return SpaceHeaders.AUTHORIZATION.equalsIgnoreCase(key);
	}

	public AuthorizationHeader(List<String> value) {
		this(value.get(0));
	}

	public AuthorizationHeader(String value) {
		Check.notNull(value, "authorization header");

		String[] schemeAndToken = value.split(" ", 2);

		if (schemeAndToken.length != 2)
			throw new AuthenticationException("invalid authorization header");

		this.scheme = schemeAndToken[0];

		if (Strings.isNullOrEmpty(scheme))
			throw new AuthenticationException("no authorization scheme specified");

		if (!(isBasic() || isBearer()))
			throw new AuthenticationException("authorization scheme [%s] not supported", scheme);

		this.token = schemeAndToken[1];
	}

	public boolean isBasic() {
		return SpaceHeaders.BASIC_SCHEME.equalsIgnoreCase(scheme);
	}

	public boolean isBearer() {
		return SpaceHeaders.BEARER_SCHEME.equalsIgnoreCase(scheme);
	}

	public String token() {
		return token;
	}

	public void token(String token) {
		this.token = token;
	}

	public String password() {
		if (username == null)
			decodeBasicToken();
		return password;
	}

	public void password(String password) {
		this.password = password;
	}

	public String username() {
		if (username == null)
			decodeBasicToken();
		return username;
	}

	public void username(String username) {
		this.username = username;
	}

	private void decodeBasicToken() {
		if (isBasic()) {
			String decodedString;
			try {
				decodedString = new String(//
						Base64.getDecoder().decode(token.getBytes(Utils.UTF8)));
			} catch (IllegalArgumentException e) {
				throw new AuthenticationException(e, "basic authorization token is not base 64 encoded");
			}

			String[] decodedTokens = decodedString.split(":", 2);

			if (decodedTokens.length != 2)
				throw new AuthenticationException("invalid basic authorization token");

			username = decodedTokens[0];
			password = decodedTokens[1];

			if (Strings.isNullOrEmpty(username))
				throw new AuthenticationException("no username specified");

			if (Strings.isNullOrEmpty(password))
				throw new AuthenticationException("no password specified");
		}

	}

	@Override
	public String toString() {
		return String.join(" ", SpaceHeaders.AUTHORIZATION, scheme, token);
	}
}
