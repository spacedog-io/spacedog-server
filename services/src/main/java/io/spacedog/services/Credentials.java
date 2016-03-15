/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Arrays;
import java.util.Optional;

import org.elasticsearch.common.Strings;

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

	public static Credentials fromAdmin(String backendId, String username, String email) {
		return new Credentials(backendId, username, email, true, false);
	}

	public static Credentials fromUser(String backendId, String username, String email) {
		return new Credentials(backendId, username, email, false, false);
	}

	public static Credentials fromUser(String backendId, String username, String email, Level level) {
		return new Credentials(backendId, username, email, level);
	}

	public static Credentials fromKey(String backendId) {
		return new Credentials(backendId);
	}

	public static Credentials fromSuperDog(String username, String email) {
		return new Credentials(BackendResource.API, username, email, true, true);
	}

	public static Credentials fromSuperDog(String backendId, String username, String email) {
		if (Strings.isNullOrEmpty(backendId))
			backendId = BackendResource.API;
		return new Credentials(backendId, username, email, true, true);
	}

	private Credentials(String backendId, String username, String email, boolean admin, boolean superDog) {
		this(backendId, username, email, superDog ? Level.SUPERDOG : admin ? Level.ADMIN : Level.USER);
	}

	private Credentials(String backendId) {
		this.backendId = backendId;
		this.level = Level.KEY;
	}

	public Credentials(String backendId, String username, String email, Level level) {
		this.backendId = backendId;
		this.username = username;
		this.email = email;
		this.level = level;
	}

	public boolean isSuperDogAuthenticated() {
		return Level.SUPERDOG.equals(level);
	}

	public boolean isAdminAuthenticated() {
		return level.ordinal() >= Level.ADMIN.ordinal();
	}

	public boolean isUserAuthenticated() {
		return level.ordinal() >= Level.USER.ordinal();
	}

	public boolean isSuperAdminAuthenticated() {
		return level.ordinal() >= Level.SUPER_ADMIN.ordinal();
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

	public Level type() {
		return level;
	}

	public boolean isRootBackend() {
		return BackendResource.API.equals(backendId);
	}
}
