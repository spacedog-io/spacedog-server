/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;

// ignore deprecated fields still in elastic data
@JsonIgnoreProperties(ignoreUnknown = true)
// only map to fields
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

	@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
	getterVisibility = Visibility.NONE, //
	isGetterVisibility = Visibility.NONE, //
	setterVisibility = Visibility.NONE)
	public static class Session {
		private String accessToken;
		private DateTime accessTokenExpiresAt;

		@Override
		public boolean equals(Object obj) {
			if (obj == null || !(obj instanceof Session))
				return false;

			Session other = (Session) obj;

			if (accessToken == other.accessToken)
				return true;

			return accessToken.equals(other.accessToken);
		}

		@Override
		public int hashCode() {
			return accessToken == null ? super.hashCode() : accessToken.hashCode();
		}

		public long expiresIn() {
			if (accessTokenExpiresAt == null)
				return 0;

			long expiresIn = accessTokenExpiresAt.getMillis() - DateTime.now().getMillis();

			if (expiresIn < 0)
				return 0;

			// expiresIn must be converted and rounded up to seconds
			return (long) Math.ceil(expiresIn / 1000.0);
		}

		public static Session newSession(long lifetime) {
			String token = new String(Base64.getEncoder().encode(//
					UUID.randomUUID().toString().getBytes(Utils.UTF8)));

			return newSession(token, lifetime);
		}

		public static Session newSession(String accessToken, long lifetime) {
			Session session = new Session();
			session.accessToken = accessToken;
			// lifetime in seconds is converted to milliseconds
			session.accessTokenExpiresAt = DateTime.now().plus(lifetime * 1000);
			return session;
		}

	}

	private String backendId;
	private String username;
	private String email;
	private Level level;
	private boolean enabled = true;
	private Set<String> roles;
	private Set<Session> sessions;
	private String passwordResetCode;
	private String hashedPassword;
	private String createdAt;
	private String updatedAt;

	/**
	 * 'onBehalf' stores the id of the backend I'm accessing. 'backendId' stores
	 * the if of this credentials real backend. 'onBehalf' must not reolaced
	 * 'backendId' and be saved to database.
	 */
	@JsonIgnore
	private String target;

	@JsonIgnore
	public Session currentSession;
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

	public String backendId() {
		return backendId;
	}

	public String target() {
		return target == null ? backendId : target;
	}

	public void target(String backendId) {
		this.target = backendId;
	}

	public boolean isTargetingRootApi() {
		return Backends.isRootApi(target());
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
		return currentSession == null ? null : currentSession.accessToken;
	}

	public long accessTokenExpiresIn() {
		return currentSession == null ? 0 : currentSession.expiresIn();
	}

	public boolean isPasswordChecked() {
		return passwordChecked;
	}

	public String passwordResetCode() {
		return passwordResetCode;
	}

	public void newPasswordResetCode() {
		passwordResetCode = UUID.randomUUID().toString();
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

	public void clearPasswordAndTokens() {
		hashedPassword = null;
		passwordResetCode = null;
		currentSession = null;
		if (sessions != null)
			sessions.clear();
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

	public ObjectNode toJson() {
		return Json.object(//
				SpaceFieldNames.ID, id(), //
				SpaceFieldNames.BACKEND_ID, target(), //
				SpaceFieldNames.USERNAME, name(), //
				SpaceFieldNames.EMAIL, email().get(), //
				SpaceFieldNames.ENABLED, enabled(), //
				SpaceFieldNames.CREDENTIALS_LEVEL, level().name(), //
				SpaceFieldNames.ROLES, roles(), //
				SpaceFieldNames.CREATED_AT, createdAt(), //
				SpaceFieldNames.UPDATED_AT, updatedAt());
	}

	//
	// Sessions and Access Tokens
	//

	public void setCurrentSession(String accessToken) {
		boolean found = false;

		if (sessions != null)
			for (Session session : sessions)
				if (accessToken.equals(session.accessToken)) {
					currentSession = session;
					found = true;
				}

		if (!found)
			throw Exceptions.invalidAccessToken(backendId);

	}

	public void setCurrentSession(Session session) {

		currentSession = session;

		if (sessions == null)
			sessions = Sets.newHashSet();

		sessions.add(currentSession);
	}

	public boolean hasCurrentSession() {
		return currentSession != null;
	}

	public void deleteCurrentSession() {
		if (hasCurrentSession()) {
			if (sessions != null)
				sessions.remove(currentSession);
			currentSession = null;
		}
	}

	public void purgeExpiredSessions() {
		if (sessions != null) {
			Iterator<Session> iterator = sessions.iterator();
			while (iterator.hasNext())
				if (iterator.next().expiresIn() == 0)
					iterator.remove();
		}
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
