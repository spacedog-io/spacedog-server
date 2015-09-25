package io.spacedog.services;

import java.util.Optional;

public class Credentials {

	private String accoundId;
	private User user;
	private ApiKey apiKey;

	public Credentials(String accountId, User user) {
		this.accoundId = accountId;
		this.user = user;
	}

	public Credentials(String accountId, ApiKey apiKey) {
		this.accoundId = accountId;
		this.apiKey = apiKey;
	}

	public String getAccountId() {
		return this.accoundId;
	}

	public Optional<User> getUser() {
		return Optional.ofNullable(this.user);
	}

	public Optional<ApiKey> getApiKey() {
		return Optional.ofNullable(this.apiKey);
	}

	public String getId() {
		if (apiKey != null)
			return apiKey.id;
		if (user != null)
			return user.username;
		throw new RuntimeException(
				"invalid credentials: apikey and user are null");
	}

	public String getSecret() {
		if (apiKey != null)
			return apiKey.secret;
		if (user != null)
			return user.password;
		throw new RuntimeException(
				"invalid credentials: apikey and user are null");
	}
}
