/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.beust.jcommander.internal.Sets;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.utils.Backends;
import io.spacedog.utils.Json;
import io.spacedog.utils.Passwords;
import io.spacedog.utils.Usernames;

@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
getterVisibility = Visibility.NONE, //
isGetterVisibility = Visibility.NONE, //
setterVisibility = Visibility.NONE)
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
	private String passwordResetCode;
	private String hashedPassword;
	private String createdAt;
	private String updatedAt;

	public Credentials() {
	}

	public Credentials(String backendId) {
		this.backendId = backendId;
		this.level = Level.KEY;
	}

	public static Credentials signUp(String backendId, Level level, ObjectNode data) {

		Credentials credentials = new Credentials(backendId);
		credentials.username = Json.checkStringNotNullOrEmpty(data, Resource.USERNAME);
		Usernames.checkIfValid(credentials.username);

		credentials.email = Json.checkStringNotNullOrEmpty(data, Resource.EMAIL);
		credentials.level = level;

		JsonNode password = data.get(Resource.PASSWORD);

		if (Json.isNull(password))
			credentials.passwordResetCode = UUID.randomUUID().toString();
		else
			credentials.hashedPassword = Passwords.checkAndHash(password.asText());

		return credentials;
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

	public void name(String name) {
		this.username = name;
	}

	public Optional<String> email() {
		return Optional.ofNullable(email);
	}

	public void email(String value) {
		this.email = value;
	}

	public Level level() {
		return level;
	}

	public void level(Level value) {
		this.level = value;
	}

	public String hashedPassword() {
		return hashedPassword;
	}

	public void hashedPassword(String value) {
		hashedPassword = value;
	}

	public String passwordResetCode() {
		return passwordResetCode;
	}

	public void passwordResetCode(String value) {
		passwordResetCode = value;
	}

	public Set<String> roles() {
		if (roles == null)
			roles = Sets.newHashSet();
		return roles;
	}

	public void roles(Set<String> value) {
		roles = value;
	}

	public String createdAt() {
		return createdAt;
	}

	public void createdAt(String value) {
		createdAt = value;
	}

	public String updatedAt() {
		return updatedAt;
	}

	public void updatedAt(String value) {
		updatedAt = value;
	}

	public boolean isRootBackend() {
		return Backends.ROOT_API.equals(backendId);
	}
}
