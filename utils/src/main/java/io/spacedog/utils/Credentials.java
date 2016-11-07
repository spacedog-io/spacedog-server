/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;

@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
getterVisibility = Visibility.NONE, //
isGetterVisibility = Visibility.NONE, //
setterVisibility = Visibility.NONE)
public class Credentials {

	public static enum Level {
		KEY, USER, ADMIN, SUPER_ADMIN, SUPERDOG;

		public Level[] lowerOrEqual() {
			return Arrays.copyOf(values(), ordinal() + 1);
		}
	}

	private String backendId;
	private String username;
	private String email;
	private Level level;
	private boolean enabled = true;
	private Set<String> roles;
	private String accessToken;
	private DateTime accessTokenExpiresAt;
	private String passwordResetCode;
	private String hashedPassword;
	private String createdAt;
	private String updatedAt;

	@JsonIgnore
	private boolean passwordChecked;
	@JsonIgnore
	private String id;
	@JsonIgnore
	private long version;

	public Credentials() {
	}

	public Credentials(String backendId) {
		this.backendId = backendId;
		this.level = Level.KEY;
	}

	public Credentials(String backendId, String name, Level level) {
		this.backendId = backendId;
		this.username = name;
		this.level = level;
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

	public String id() {
		return id;
	}

	public void id(String id) {
		this.id = id;
	}

	public long version() {
		return version;
	}

	public void version(long version) {
		this.version = version;
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

	public String accessToken() {
		return accessToken;
	}

	public long accessTokenExpiresIn() {
		if (accessTokenExpiresAt == null)
			return 0;
		long expiresIn = accessTokenExpiresAt.getMillis() - DateTime.now().getMillis();
		if (expiresIn < 0)
			return 0;
		return expiresIn;
	}

	public boolean isPasswordChecked() {
		return passwordChecked;
	}

	public String passwordResetCode() {
		return passwordResetCode;
	}

	public boolean enabled() {
		return this.enabled;
	}

	public void enabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Set<String> roles() {
		if (roles == null)
			roles = Sets.newHashSet();

		roles.add(defaultRole());
		return roles;
	}

	public void roles(Set<String> value) {
		roles = value;
	}

	public void checkRoles(String... authorizedRoles) {
		if (authorizedRoles != null) {
			Set<String> thisCredentialsRoles = roles();
			for (String authorizedRole : authorizedRoles)
				if (thisCredentialsRoles.contains(authorizedRole))
					return;
		}
		throw Exceptions.insufficientCredentials(this);
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

	public boolean isBrandNew() {
		return updatedAt == null ? true : updatedAt.equals(createdAt);
	}

	public boolean isRootBackend() {
		return Backends.ROOT_API.equals(backendId);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((backendId == null) ? 0 : backendId.hashCode());
		result = prime * result + ((username == null) ? 0 : username.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Credentials other = (Credentials) obj;
		if (backendId == null) {
			if (other.backendId != null)
				return false;
		} else if (!backendId.equals(other.backendId))
			return false;
		if (username == null) {
			if (other.username != null)
				return false;
		} else if (!username.equals(other.username))
			return false;
		return true;
	}

	//
	// Business logic
	//

	public boolean checkPassword(String passwordToCheck) {
		if (hashedPassword == null)
			return false;

		String hashedPasswordToCheck = Passwords.hash(passwordToCheck);
		if (hashedPassword.equals(hashedPasswordToCheck)) {
			passwordChecked = true;
			return true;
		}
		return false;
	}

	public void resetPassword() {
		hashedPassword = null;
		passwordResetCode = UUID.randomUUID().toString();
		deleteAccessToken();
	}

	public void setPassword(String password, String passwordResetCode, Optional<String> regex) {
		Check.notNullOrEmpty(password, "password");
		Check.notNullOrEmpty(passwordResetCode, "passwordResetCode");

		if (hashedPassword != null || passwordResetCode == null)
			throw Exceptions.illegalArgument(//
					"credentials [%s] password must be deleted before reset", username);

		if (!this.passwordResetCode.equals(passwordResetCode))
			throw Exceptions.illegalArgument(//
					"password reset code [%s] invalid", passwordResetCode);

		setPassword(password, regex);
	}

	public boolean setPassword(String password, Optional<String> regex) {
		hashedPassword = Passwords.checkAndHash(password, regex);
		passwordChecked = true;
		passwordResetCode = null;
		return true;
	}

	public void newAccessToken(long lifetime) {
		accessTokenExpiresAt = DateTime.now().plus(lifetime);
		accessToken = new String(Base64.getEncoder().encode(//
				UUID.randomUUID().toString().getBytes(Utils.UTF8)));
	}

	public void deleteAccessToken() {
		accessToken = null;
		accessTokenExpiresAt = null;
	}

	public void setExternalAccessToken(String accessToken, DateTime accessTokenExpiresAt) {
		this.accessToken = accessToken;
		this.accessTokenExpiresAt = accessTokenExpiresAt;
		this.passwordResetCode = null;
		this.hashedPassword = null;
	}

	public ObjectNode toJson() {
		return Json.object(//
				SpaceFieldNames.ID, id(), //
				SpaceFieldNames.BACKEND_ID, backendId(), //
				SpaceFieldNames.USERNAME, name(), //
				SpaceFieldNames.EMAIL, email().get(), //
				SpaceFieldNames.ENABLED, enabled(), //
				SpaceFieldNames.CREDENTIALS_LEVEL, level().name(), //
				SpaceFieldNames.ROLES, roles(), //
				SpaceFieldNames.CREATED_AT, createdAt(), //
				SpaceFieldNames.UPDATED_AT, updatedAt());
	}

	//
	// implementation
	//

	private String defaultRole() {
		if (Level.USER.equals(level))
			return "user";
		if (Level.ADMIN.equals(level))
			return "admin";
		if (Level.SUPER_ADMIN.equals(level))
			return "admin";
		if (Level.SUPERDOG.equals(level))
			return "admin";
		return "key";
	}

	public void setLegacyId() {
		this.id = toLegacyId(backendId, username);
	}

	public static String[] fromLegacyId(String id) {
		return id.split("-", 2);
	}

	public static String toLegacyId(String backendId, String username) {
		return String.join("-", backendId, username);
	}
}
