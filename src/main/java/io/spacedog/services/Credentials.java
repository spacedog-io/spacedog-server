package io.spacedog.services;

import java.util.Optional;

public class Credentials {

	private String accoundId;
	private User user;
	private BackendKey backendKey;

	public Credentials(String accountId, User user) {
		this.accoundId = accountId;
		this.user = user;
	}

	public Credentials(String accountId, BackendKey apiKey) {
		this.accoundId = accountId;
		this.backendKey = apiKey;
	}

	public String getAccountId() {
		return this.accoundId;
	}

	public Optional<User> getUser() {
		return Optional.ofNullable(this.user);
	}

	public Optional<BackendKey> getApiKey() {
		return Optional.ofNullable(this.backendKey);
	}

	public String getId() {
		if (backendKey != null)
			return backendKey.name;
		if (user != null)
			return user.username;
		throw new RuntimeException(
				"invalid credentials: apikey and user are null");
	}

	public String getSecret() {
		if (backendKey != null)
			return backendKey.secret;
		if (user != null)
			return user.hashedPassword;
		throw new RuntimeException(
				"invalid credentials: apikey and user are null");
	}
}
