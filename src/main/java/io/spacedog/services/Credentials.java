/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Optional;

public class Credentials {

	private String backendId;
	private User user;
	private BackendKey backendKey;

	public Credentials(String backendId, User user) {
		this.backendId = backendId;
		this.user = user;
	}

	public Credentials(String backendId, BackendKey apiKey) {
		this.backendId = backendId;
		this.backendKey = apiKey;
	}

	public String getBackendId() {
		return this.backendId;
	}

	public Optional<User> getUser() {
		return Optional.ofNullable(this.user);
	}

	public Optional<BackendKey> getBackendKey() {
		return Optional.ofNullable(this.backendKey);
	}

	public String getName() {
		// username is first
		if (user != null)
			return user.username;
		// key name is default
		if (backendKey != null)
			return backendKey.name;
		throw new RuntimeException("invalid credentials: apikey and user are null");
	}
}
