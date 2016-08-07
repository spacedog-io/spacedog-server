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
	private Set<String> roles;
	private String accessToken;
	private DateTime accessTokenExpiresAt;
	private String passwordResetCode;
	private String hashedPassword;
	private String createdAt;
	private String updatedAt;

	@JsonIgnore
	private boolean passwordChecked;

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

	public void setAccessToken(String accessToken, DateTime accessTokenExpiresAt) {
		this.accessToken = accessToken;
		this.accessTokenExpiresAt = accessTokenExpiresAt;
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

	public Set<String> roles() {
		if (roles == null)
			roles = Sets.newHashSet();

		roles.add(defaultRole());
		return roles;
	}

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

	public void setPassword(String password, String passwordResetCode) {
		Check.notNullOrEmpty(password, "password");
		Check.notNullOrEmpty(passwordResetCode, "passwordResetCode");

		if (hashedPassword != null || passwordResetCode == null)
			throw Exceptions.illegalArgument(//
					"credentials [%s] password must be deleted before reset", username);

		if (!this.passwordResetCode.equals(passwordResetCode))
			throw Exceptions.illegalArgument(//
					"password reset code [%s] invalid", passwordResetCode);

		setPassword(password);
	}

	public boolean setPassword(String password) {
		Check.notNullOrEmpty(password, "password");
		hashedPassword = Passwords.checkAndHash(password);
		passwordChecked = true;
		passwordResetCode = null;
		newAccessToken();
		return true;
	}

	public void newAccessToken() {
		newAccessToken(false);
	}

	public void newAccessToken(boolean expiresEarly) {
		// expires in 24 hours or in 2 seconds for testing
		long expiresIn = expiresEarly ? 1000 * 2 : 1000 * 60 * 60 * 24;
		accessTokenExpiresAt = DateTime.now().plus(expiresIn);
		accessToken = new String(Base64.getEncoder().encode(//
				UUID.randomUUID().toString().getBytes(Utils.UTF8)));
	}

	public void deleteAccessToken() {
		accessToken = null;
		accessTokenExpiresAt = null;
	}
}
