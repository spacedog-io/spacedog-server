/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Arrays;
import java.util.Optional;

import com.google.common.base.Strings;

import io.spacedog.utils.BackendKey;

public class Credentials {

	public static enum Type {
		KEY, USER, ADMIN, SUPERDOG;

		public Type[] lowerOrEqual() {
			return Arrays.copyOf(values(), ordinal() + 1);
		}
	}

	private String backendId;
	private String username;
	private String email;
	private BackendKey backendKey;
	private boolean admin = false;
	private boolean superDog;

	public static Credentials fromAdmin(String backendId, String username, String email, BackendKey backendKey) {
		return new Credentials(backendId, username, email, backendKey, true, false);
	}

	public static Credentials fromUser(String backendId, String username, String email) {
		return new Credentials(backendId, username, email, null, false, false);
	}

	public static Credentials fromKey(String backendId, BackendKey backendKey) {
		return new Credentials(backendId, null, null, backendKey, false, false);
	}

	public static Credentials fromSuperDog(String username, String email) {
		return new Credentials("spacedog", username, email, null, true, true);
	}

	public static Credentials fromSuperDog(String backendId, String username, String email) {
		return new Credentials(backendId, username, email, null, true, true);
	}

	private Credentials(String backendId, String username, String email, BackendKey backendKey, boolean admin,
			boolean superDog) {
		this.backendId = backendId;
		this.username = username;
		this.email = email;
		this.backendKey = backendKey;
		this.admin = admin;
		this.superDog = superDog;
	}

	public boolean isSuperDogAuthenticated() {
		return superDog;
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

	public Optional<String> email() {
		return Optional.of(email);
	}

	public Type type() {
		if (isSuperDogAuthenticated())
			return Type.SUPERDOG;
		if (isAdminAuthenticated())
			return Type.ADMIN;
		if (isUserAuthenticated())
			return Type.USER;
		else
			return Type.KEY;
	}
}
