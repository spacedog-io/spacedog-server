/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Arrays;
import java.util.Optional;

import io.spacedog.utils.BackendKey;

public class Credentials {

	public static enum Level {
		KEY, USER, OPERATOR, ADMIN, SUPER_ADMIN, SUPERDOG;

		public Level[] lowerOrEqual() {
			return Arrays.copyOf(values(), ordinal() + 1);
		}
	}

	private String backendId;
	private String username;
	private String email;
	private Level level;

	public Credentials(String backendId) {
		this.backendId = backendId;
		this.level = Level.KEY;
	}

	public Credentials(String backendId, String username, String email, Level level) {
		this.backendId = backendId;
		this.username = username;
		this.email = email;
		this.level = level;
	}

	public boolean isSuperDog() {
		return Level.SUPERDOG.equals(level);
	}

	public boolean isAtLeastSuperAdmin() {
		return level.ordinal() >= Level.SUPER_ADMIN.ordinal();
	}

	public boolean isAtLeastAdmin() {
		return level.ordinal() >= Level.ADMIN.ordinal();
	}

	public boolean isAtLeastUser() {
		return level.ordinal() >= Level.USER.ordinal();
	}

	public String backendId() {
		return this.backendId;
	}

	public void backendId(String backendId) {
		this.backendId = backendId;
	}

	public String name() {
		return username == null ? "default" : username;
	}

	public Optional<String> email() {
		return Optional.of(email);
	}

	public Level level() {
		return level;
	}

	public boolean isRootBackend() {
		return BackendKey.ROOT_API.equals(backendId);
	}
}
