/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

// ignore deprecated fields still in elastic data
@JsonIgnoreProperties(ignoreUnknown = true)
// only map to fields
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
getterVisibility = Visibility.NONE, //
isGetterVisibility = Visibility.NONE, //
setterVisibility = Visibility.NONE)
public class Credentials {

	// Standard roles
	public static final String KEY = "key";
	public static final String USER = "user";
	public static final String ADMIN = "admin";
	public static final String SUPER_ADMIN = "super_admin";
	public static final String SUPERDOG = "superdog";

	private String backendId;
	private String username;
	private String email;
	private Level level;
	private boolean enabled = true;
	private DateTime enableAfter;
	private DateTime disableAfter;
	private Set<String> roles;
	private Set<Session> sessions;
	private ObjectNode stash;
	private String passwordResetCode;
	private String hashedPassword;
	private boolean passwordMustChange;
	private int invalidChallenges;
	private DateTime lastInvalidChallengeAt;
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
	private boolean passwordHasBeenChallenged;
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

	public Credentials(String backendId, String name) {
		this.backendId = backendId;
		this.username = name;
	}

	public Credentials(String backendId, String name, Level level) {
		this(backendId, name);
		this.level = level;
	}

	public String id() {
		return id;
	}

	public Credentials id(String id) {
		this.id = id;
		return this;
	}

	public long version() {
		return version;
	}

	public Credentials version(long version) {
		this.version = version;
		return this;
	}

	public String backendId() {
		return backendId;
	}

	public String target() {
		return target == null ? backendId : target;
	}

	public Credentials target(String backendId) {
		this.target = backendId;
		return this;
	}

	public String name() {
		return username == null ? "default" : username;
	}

	public Credentials name(String name) {
		this.username = name;
		return this;
	}

	public Optional<String> email() {
		return Optional.ofNullable(email);
	}

	public Credentials email(String value) {
		this.email = value;
		return this;
	}

	public Level level() {
		return level;
	}

	public Credentials level(Level value) {
		this.level = value;
		return this;
	}

	public String accessToken() {
		return currentSession == null ? null : currentSession.accessToken;
	}

	public long accessTokenExpiresIn() {
		return currentSession == null ? 0 : currentSession.expiresIn();
	}

	public boolean hasPasswordBeenChallenged() {
		return passwordHasBeenChallenged;
	}

	public String passwordResetCode() {
		return passwordResetCode;
	}

	public boolean passwordMustChange() {
		return passwordMustChange;
	}

	public void passwordMustChange(Boolean passwordMustChange) {
		this.passwordMustChange = passwordMustChange;
	}

	public boolean enabled() {
		return enabled;
	}

	public Credentials enabled(boolean enabled) {
		this.enabled = enabled;
		return this;
	}

	public DateTime enableAfter() {
		return enableAfter;
	}

	public Credentials enableAfter(DateTime enableAfter) {
		this.enableAfter = enableAfter;
		return this;
	}

	public DateTime disableAfter() {
		return disableAfter;
	}

	public Credentials disableAfter(DateTime disableAfter) {
		this.disableAfter = disableAfter;
		return this;
	}

	public Set<String> roles() {
		if (roles == null)
			roles = Sets.newHashSet();

		roles.addAll(defaultRoles());
		return roles;
	}

	public Credentials roles(Set<String> value) {
		roles = value;
		return this;
	}

	public String createdAt() {
		return createdAt;
	}

	public Credentials createdAt(String value) {
		createdAt = value;
		return this;
	}

	public String updatedAt() {
		return updatedAt;
	}

	public Credentials updatedAt(String value) {
		updatedAt = value;
		return this;
	}

	public int invalidChallenges() {
		return invalidChallenges;
	}

	public Credentials invalidChallenges(int invalidChallenges) {
		this.invalidChallenges = invalidChallenges;
		return this;
	}

	public DateTime lastInvalidChallengeAt() {
		return lastInvalidChallengeAt;
	}

	public Credentials lastInvalidChallengeAt(DateTime lastInvalidChallengeAt) {
		this.lastInvalidChallengeAt = lastInvalidChallengeAt;
		return this;
	}

	//
	// Stash
	//

	public JsonNode getFromStash(String path) {
		return stash == null ? null : Json.get(stash, path);
	}

	public void addToStash(String path, Object value) {
		if (stash == null)
			stash = Json.object();

		Json.set(stash, path, value);
	}

	public void removeFromStash(String path) {
		if (stash != null)
			Json.remove(stash, path);
	}

	//
	// Object overrides
	//

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
	// Level and roles
	//

	public boolean isSuperDog() {
		return Level.SUPERDOG.equals(level);
	}

	public void checkSuperDog() {
		if (!isSuperDog())
			throw Exceptions.insufficientCredentials(this);
	}

	public boolean isSuperAdmin() {
		return Level.SUPER_ADMIN.equals(level);
	}

	public boolean isAtLeastSuperAdmin() {
		return level.ordinal() >= Level.SUPER_ADMIN.ordinal();
	}

	public void checkAtLeastSuperAdmin() {
		if (!isAtLeastSuperAdmin())
			throw Exceptions.insufficientCredentials(this);
	}

	public boolean isAdmin() {
		return Level.ADMIN.equals(level);
	}

	public boolean isAtLeastAdmin() {
		return level.ordinal() >= Level.ADMIN.ordinal();
	}

	public void checkAtLeastAdmin() {
		if (!isAtLeastAdmin())
			throw Exceptions.insufficientCredentials(this);
	}

	public boolean isUser() {
		return Level.USER.equals(level);
	}

	public boolean isAtLeastUser() {
		return level.ordinal() >= Level.USER.ordinal();
	}

	public void checkAtLeastUser() {
		if (!isAtLeastUser())
			throw Exceptions.insufficientCredentials(this);
	}

	public boolean isKey() {
		return Level.KEY.equals(level);
	}

	public boolean isReal() {
		return !Strings.isNullOrEmpty(id);
	}

	public void checkRoles(Set<String> authorizedRoles) {
		if (authorizedRoles != null) {
			Set<String> thisCredentialsRoles = roles();
			for (String authorizedRole : authorizedRoles)
				if (thisCredentialsRoles.contains(authorizedRole))
					return;
		}
		throw Exceptions.insufficientCredentials(this);
	}

	//
	// Enable disable logic
	//

	public void doEnableOrDisable(boolean enable) {
		if (enable) {
			enabled(true);
			invalidChallenges(0);
			lastInvalidChallengeAt(null);

		} else
			enabled(false);
	}

	public void checkReallyEnabled() {
		if (!isReallyEnabled())
			throw Exceptions.disabledCredentials(this);
	}

	public boolean isReallyEnabled() {

		if (this.enabled) {
			if (isEnableAfterAndDisableAfterAreNull())
				return true;
			DateTime now = DateTime.now();
			if (isNowBeforeDisableAfter(now))
				return true;
			if (isEnableAfterBeforeNow(now))
				return true;
			if (isDisableAfterBeforeEnableAfterBeforeNow(now))
				return true;
			if (isEnableAfterBeforeNowBeforeDisableAfter(now))
				return true;
		}

		return false;
	}

	private boolean isEnableAfterAndDisableAfterAreNull() {
		return enableAfter == null && disableAfter == null;
	}

	private boolean isDisableAfterBeforeEnableAfterBeforeNow(DateTime now) {
		return enableAfter != null //
				&& disableAfter != null//
				&& disableAfter.isBefore(enableAfter)//
				&& enableAfter.isBefore(now);
	}

	private boolean isEnableAfterBeforeNowBeforeDisableAfter(DateTime now) {
		return enableAfter != null //
				&& disableAfter != null//
				&& enableAfter.isBefore(now)//
				&& now.isBefore(disableAfter);
	}

	private boolean isEnableAfterBeforeNow(DateTime now) {
		return enableAfter != null //
				&& disableAfter == null//
				&& enableAfter.isBefore(now);
	}

	private boolean isNowBeforeDisableAfter(DateTime now) {
		return enableAfter == null //
				&& disableAfter != null//
				&& now.isBefore(disableAfter);
	}

	//
	// Password logic
	//

	public void checkPasswordHasBeenChallenged() {
		if (!hasPasswordBeenChallenged())
			throw Exceptions.passwordMustBeChallenged();
	}

	public void newPasswordResetCode() {
		passwordResetCode = UUID.randomUUID().toString();
	}

	public boolean challengePassword(String passwordToChallenge) {
		if (hashedPassword == null)
			return false;

		String hashedPasswordToChallenge = Passwords.hash(passwordToChallenge);

		if (hashedPassword.equals(hashedPasswordToChallenge)) {
			passwordHasBeenChallenged = true;
			return true;
		}
		return false;
	}

	public void clearPasswordAndTokens() {
		hashedPassword = null;
		passwordResetCode = null;
		invalidChallenges = 0;
		lastInvalidChallengeAt = null;
		currentSession = null;
		if (sessions != null)
			sessions.clear();
	}

	public void changePassword(String password, String passwordResetCode, Optional<String> regex) {
		Check.notNullOrEmpty(password, "password");
		Check.notNullOrEmpty(passwordResetCode, "passwordResetCode");

		if (hashedPassword != null || passwordResetCode == null)
			throw Exceptions.illegalArgument(//
					"credentials [%s] password must be deleted before reset", username);

		if (!this.passwordResetCode.equals(passwordResetCode))
			throw Exceptions.illegalArgument(//
					"password reset code [%s] invalid", passwordResetCode);

		changePassword(password, regex);
	}

	public void changePassword(String password, Optional<String> regex) {
		Passwords.check(password, regex);
		clearPasswordAndTokens();
		hashedPassword = Passwords.hash(password);
		passwordHasBeenChallenged = true;
		passwordMustChange = false;
	}

	//
	// Other logic
	//

	public boolean isTargetingRootApi() {
		return Backends.isRootApi(target());
	}

	public ObjectNode toJson() {
		return Json.object(//
				SpaceFields.FIELD_ID, id(), //
				SpaceFields.FIELD_BACKEND_ID, target(), //
				SpaceFields.FIELD_USERNAME, name(), //
				SpaceFields.FIELD_EMAIL, email().get(), //
				"reallyEnabled", isReallyEnabled(), //
				SpaceFields.FIELD_ENABLED, enabled(), //
				SpaceFields.FIELD_ENABLE_AFTER, enableAfter(), //
				SpaceFields.FIELD_DISABLE_AFTER, disableAfter(), //
				SpaceFields.FIELD_INVALID_CHALLENGES, invalidChallenges, //
				SpaceFields.FIELD_LAST_INVALID_CHALLENGE_AT, lastInvalidChallengeAt, //
				SpaceFields.FIELD_CREDENTIALS_LEVEL, level().name(), //
				SpaceFields.FIELD_ROLES, roles(), //
				SpaceFields.FIELD_CREATED_AT, createdAt(), //
				SpaceFields.FIELD_UPDATED_AT, updatedAt());
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

	public boolean isBrandNew() {
		return updatedAt == null ? true : updatedAt.equals(createdAt);
	}

	//
	// implementation
	//

	private Set<String> defaultRoles() {
		if (Level.USER.equals(level))
			return Collections.singleton(USER);
		if (Level.ADMIN.equals(level))
			return Collections.singleton(ADMIN);
		if (Level.SUPER_ADMIN.equals(level))
			return Sets.newHashSet(ADMIN, SUPER_ADMIN);
		if (Level.SUPERDOG.equals(level))
			return Sets.newHashSet(ADMIN, SUPER_ADMIN, SUPERDOG);
		return Collections.singleton(KEY);
	}

	public void initIdFromLegacy() {
		this.id = toLegacyId(backendId, username);
	}

	public static String[] fromLegacyId(String id) {
		return id.split("-", 2);
	}

	public static String toLegacyId(String backendId, String username) {
		return String.join("-", backendId, username);
	}

	//
	// Inner classes
	//

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

}
