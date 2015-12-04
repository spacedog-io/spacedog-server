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

	public static Credentials fromAdmin(String backendId, String username, BackendKey backendKey) {
		return new Credentials(backendId, username, backendKey, true);
	}

	public static Credentials fromUser(String backendId, String username) {
		return new Credentials(backendId, username, null, false);
	}

	public static Credentials fromKey(String backendId, BackendKey backendKey) {
		return new Credentials(backendId, null, backendKey, false);
	}

	private Credentials(String backendId, String username, BackendKey backendKey, boolean admin) {
		this.backendId = backendId;
		this.username = username;
		this.backendKey = backendKey;
		this.admin = admin;
	}

	public boolean isAdminAuthenticated() {
		return admin;
	}

	public boolean isUserAuthenticated() {
		return !Strings.isNullOrEmpty(username);
	}

	public String backendId() {
		return this.backendId;
	}

	public Optional<BackendKey> backendKey() {
		return Optional.ofNullable(this.backendKey);
	}

	public Optional<String> backendKeyAsString() {
		if (backendKey == null)
			return Optional.empty();

		return Optional.of(new StringBuilder(backendId).append(':').append(backendKey.name).append(':')
				.append(backendKey.secret).toString());
	}

	public String name() {
		// username is first
		if (username != null)
			return username;
		// key name is default
		if (backendKey != null)
			return backendKey.name;
		throw new RuntimeException("invalid credentials: no key nor user data");
	}
}
