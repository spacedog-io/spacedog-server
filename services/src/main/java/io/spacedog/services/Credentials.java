/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.utils.Backends;
import io.spacedog.utils.Json;
import io.spacedog.utils.Passwords;
import io.spacedog.utils.Usernames;

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
	private Set<String> roles;
	private Optional<String> passwordResetCode = Optional.empty();
	private Optional<String> hashedPassword = Optional.empty();

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

	public static Credentials signUp(String backendId, Level level, ObjectNode data) {

		Credentials credentials = new Credentials(backendId);
		credentials.username = Json.checkStringNotNullOrEmpty(data, Resource.USERNAME);
		Usernames.checkIfValid(credentials.username);

		credentials.email = Json.checkStringNotNullOrEmpty(data, Resource.EMAIL);

		credentials.level = level;
		// credentials.level =
		// Level.valueOf(data.path(Resource.CREDENTIALS_LEVEL).asText("USER"));
		// if (level.ordinal() >= Level.ADMIN.ordinal() //
		// && level.ordinal() > credentials.level().ordinal())
		// throw new AuthorizationException("you're not authorized to create
		// users of level [%s]");

		JsonNode password = data.get(Resource.PASSWORD);

		if (Json.isNull(password))
			credentials.passwordResetCode = Optional.of(UUID.randomUUID().toString());
		else
			credentials.hashedPassword = Optional.of(Passwords.checkAndHash(password.asText()));

		return credentials;
	}

	public void setUserCredentials(Credentials userCredentials) {
		this.username = userCredentials.username;
		this.email = userCredentials.email;
		this.level = userCredentials.level;
	}

	public boolean isSuperDog() {
		return Level.SUPERDOG.equals(level);
	}

	public boolean isAtLeastSuperAdmin() {
		return level.ordinal() >= Level.SUPER_ADMIN.ordinal();
	}

	public boolean isAtMostSuperAdmin() {
		return level.ordinal() <= Level.SUPER_ADMIN.ordinal();
	}

	public boolean isAtLeastAdmin() {
		return level.ordinal() >= Level.ADMIN.ordinal();
	}

	public boolean isAtMostAdmin() {
		return level.ordinal() <= Level.ADMIN.ordinal();
	}

	public boolean isAtLeastUser() {
		return level.ordinal() >= Level.USER.ordinal();
	}

	public boolean isAtMostUser() {
		return level.ordinal() <= Level.USER.ordinal();
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

	public Optional<String> hashedPassword() {
		return hashedPassword;
	}

	public Optional<String> passwordResetCode() {
		return passwordResetCode;
	}

	public Set<String> roles() {
		return Collections.unmodifiableSet(roles);
	}

	public boolean isRootBackend() {
		return Backends.ROOT_API.equals(backendId);
	}
}
