/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Optional;

import com.google.common.base.Strings;

public class Credentials {

	private String backendId;
	private String username;
	private BackendKey backendKey;
	private boolean admin = false;

	public Credentials(String backendId, String username, boolean admin) {
		this.backendId = backendId;
		this.username = username;
		this.admin = admin;
	}

	public Credentials(String backendId, BackendKey apiKey) {
		this.backendId = backendId;
		this.backendKey = apiKey;
		this.admin = false;
	}

	public boolean isAdmin() {
		return admin;
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

	public boolean isUser() {
		return !Strings.isNullOrEmpty(username);
	}
}
