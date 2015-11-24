/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Optional;

public class Credentials {

	private String backendId;
	private String username;
	private BackendKey backendKey;

	public Credentials(String backendId, String username) {
		this.backendId = backendId;
		this.username = username;
	}

	public Credentials(String backendId, BackendKey apiKey) {
		this.backendId = backendId;
		this.backendKey = apiKey;
	}

	public String getBackendId() {
		return this.backendId;
	}

	public Optional<BackendKey> getBackendKey() {
		return Optional.ofNullable(this.backendKey);
	}

	public String getName() {
		// username is first
		if (username != null)
			return username;
		// key name is default
		if (backendKey != null)
			return backendKey.name;
		throw new RuntimeException("invalid credentials: apikey and user are null");
	}
}
