package io.spacedog.utils;

import java.util.Set;

import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class SendPulseSettings extends Settings {

	public String clientId;
	public String clientSecret;
	public String accessToken;
	public String accessTokenType;
	public DateTime accessTokenExpiresAt;
	public Set<String> authorizedRoles;

	public void setToken(ObjectNode body) {
		accessToken = body.get("access_token").asText();
		accessTokenType = body.get("token_type").asText();

		if (!"Bearer".equals(accessTokenType))
			throw Exceptions.illegalArgument(//
					"invalid sendpulse token type [%s]", accessTokenType);

		// minus 10 seconds to make sure we expire before
		// the token really expires and for server time differences
		long expiresIn = body.get("expires_in").asLong() - 10;

		// converts expiresIn from seconds to milliseconds
		accessTokenExpiresAt = DateTime.now().plus(expiresIn * 1000);
	}

	public boolean hasExpired() {
		if (accessTokenExpiresAt == null)
			return true;

		long expiresIn = accessTokenExpiresAt.getMillis() - DateTime.now().getMillis();
		return expiresIn <= 0;
	}
}
