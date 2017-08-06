/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
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
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class Credentials {

	public static enum Type {
		guest, user, admin, superadmin, superdog;

		public static Type fromRoles(Set<String> roles) {
			if (roles.contains(superdog.toString()))
				return superdog;
			if (roles.contains(superadmin.toString()))
				return superadmin;
			if (roles.contains(admin.toString()))
				return admin;
			if (roles.contains(user.toString()))
				return user;
			return guest;
		}

		public boolean isGreaterThanOrEqualTo(Type type) {
			return ordinal() >= type.ordinal();
		}

		public boolean isGreaterThan(Type type) {
			return ordinal() > type.ordinal();
		}

		public static Type authorizedToManage(String role) {
			try {
				return Type.valueOf(role);
			} catch (IllegalArgumentException e) {
				return Type.admin;
			}
		}
	}

	public static final Credentials GUEST = new Credentials("guest");

	private String username;
	private String email;
	private boolean enabled = true;
	private DateTime enableAfter;
	private DateTime disableAfter;
	private Set<String> roles;
	private List<Session> sessions;
	private ObjectNode stash;
	private String passwordResetCode;
	private String hashedPassword;
	private boolean passwordMustChange;
	private int invalidChallenges;
	private DateTime lastInvalidChallengeAt;
	private String createdAt;
	private String updatedAt;

	@JsonIgnore
	private Session currentSession;
	@JsonIgnore
	private boolean passwordHasBeenChallenged;
	@JsonIgnore
	private String id;
	@JsonIgnore
	private long version;
	@JsonIgnore
	private Type type;

	public Credentials() {
	}

	public Credentials(String name) {
		this.username = name;
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

	public String name() {
		return username == null ? Type.guest.name() : username;
	}

	public Credentials name(String name) {
		this.username = name;
		return this;
	}

	public Optional7<String> email() {
		return Optional7.ofNullable(email);
	}

	public Credentials email(String value) {
		this.email = value;
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
			return Collections.emptySet();
		return roles;
	}

	public Credentials clearRoles() {
		if (roles != null)
			roles.clear();
		return this;
	}

	public Credentials addRoles(String... newRoles) {
		if (newRoles == null)
			return this;

		if (roles == null)
			roles = Sets.newHashSet();

		for (String role : newRoles) {
			Roles.checkIfValid(role);
			roles.add(role);
		}

		type = Type.fromRoles(this.roles);
		return this;
	}

	public Credentials removeRoles(String... values) {
		if (roles != null && values != null) {
			for (String role : values)
				roles.remove(role);

			type = Type.fromRoles(this.roles);
		}
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
		return stash == null ? null : Json7.get(stash, path);
	}

	public void addToStash(String path, Object value) {
		if (stash == null)
			stash = Json7.object();

		Json7.set(stash, path, value);
	}

	public void removeFromStash(String path) {
		if (stash != null)
			Json7.remove(stash, path);
	}

	//
	// Object overrides
	//

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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

	public Type type() {
		if (type == null)
			type = Type.fromRoles(roles());
		return type;
	}

	public boolean isSuperDog() {
		return Type.superdog.equals(type());
	}

	public Credentials checkSuperDog() {
		if (!isSuperDog())
			throw Exceptions.insufficientCredentials(this);
		return this;
	}

	public boolean isSuperAdmin() {
		return Type.superadmin.equals(type());
	}

	public boolean isAtLeastSuperAdmin() {
		return type().ordinal() >= Type.superadmin.ordinal();
	}

	public Credentials checkAtLeastSuperAdmin() {
		if (!isAtLeastSuperAdmin())
			throw Exceptions.insufficientCredentials(this);
		return this;
	}

	public boolean isAdmin() {
		return Type.admin.equals(type());
	}

	public boolean isAtLeastAdmin() {
		return type().ordinal() >= Type.admin.ordinal();
	}

	public Credentials checkAtLeastAdmin() {
		if (!isAtLeastAdmin())
			throw Exceptions.insufficientCredentials(this);
		return this;
	}

	public boolean isUser() {
		return Type.user.equals(type());
	}

	public boolean isAtLeastUser() {
		return type().ordinal() >= Type.user.ordinal();
	}

	public Credentials checkAtLeastUser() {
		if (!isAtLeastUser())
			throw Exceptions.insufficientCredentials(this);
		return this;
	}

	public boolean isGuest() {
		return Type.guest.equals(type());
	}

	public boolean isGreaterThan(Credentials other) {
		return type().isGreaterThan(other.type());
	}

	public boolean isGreaterThanOrEqualTo(Credentials other) {
		return type().isGreaterThanOrEqualTo(other.type());
	}

	public void checkAuthorizedToSet(String... roles) {
		if (roles != null)
			for (String role : roles)
				if (Type.authorizedToManage(role).isGreaterThan(type()))
					throw Exceptions.insufficientCredentials(this);
	}

	public boolean isReal() {
		return !Strings.isNullOrEmpty(id);
	}

	public void checkRoles(String... authorizedRoles) {
		checkRoles(Sets.newHashSet(authorizedRoles));
	}

	public void checkRoles(Iterable<String> authorizedRoles) {
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

	public void changePassword(String password, String passwordResetCode, Optional7<String> regex) {
		Check.notNullOrEmpty(password, "password");
		Check.notNullOrEmpty(passwordResetCode, "passwordResetCode");

		if (!passwordResetCode.equals(this.passwordResetCode))
			throw Exceptions.illegalArgument(//
					"password reset code [%s] invalid", passwordResetCode);

		changePassword(password, regex);
	}

	public void changePassword(String password, Optional7<String> regex) {
		Passwords.check(password, regex);
		clearPasswordAndTokens();
		hashedPassword = Passwords.hash(password);
		passwordHasBeenChallenged = true;
		passwordMustChange = false;
	}

	//
	// Other logic
	//

	public ObjectNode toJson() {
		return Json7.object(//
				SpaceFields.FIELD_ID, id(), //
				SpaceFields.FIELD_USERNAME, name(), //
				SpaceFields.FIELD_EMAIL, email().orElse(null), //
				"reallyEnabled", isReallyEnabled(), //
				SpaceFields.FIELD_ENABLED, enabled(), //
				SpaceFields.FIELD_ENABLE_AFTER, enableAfter(), //
				SpaceFields.FIELD_DISABLE_AFTER, disableAfter(), //
				SpaceFields.FIELD_INVALID_CHALLENGES, invalidChallenges, //
				SpaceFields.FIELD_LAST_INVALID_CHALLENGE_AT, lastInvalidChallengeAt, //
				SpaceFields.FIELD_ROLES, roles(), //
				SpaceFields.FIELD_CREATED_AT, createdAt(), //
				SpaceFields.FIELD_UPDATED_AT, updatedAt());
	}

	@Override
	public String toString() {
		return String.format("[%s][%s]", type(), name());
	}

	//
	// Sessions and Access Tokens
	//

	public List<Session> sessions() {
		return Collections.unmodifiableList(sessions);
	}

	public void setCurrentSession(String accessToken) {
		boolean found = false;

		if (sessions != null)
			for (Session session : sessions)
				if (accessToken.equals(session.accessToken)) {
					currentSession = session;
					found = true;
				}

		if (!found)
			throw Exceptions.invalidAccessToken();

	}

	public void setCurrentSession(Session session) {

		currentSession = session;

		if (sessions == null)
			sessions = Lists.newArrayList();

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

	public void purgeOldSessions(int sessionsSizeMax) {
		Check.isTrue(sessionsSizeMax > 0, "sessions size max should be greater than 0");
		if (sessions != null && sessions.size() > sessionsSizeMax) {
			Collections.sort(sessions);
			for (int i = sessions.size() - 1; i >= sessionsSizeMax; i--)
				sessions.remove(i);
		}
	}

	public boolean isBrandNew() {
		return updatedAt == null ? true : updatedAt.equals(createdAt);
	}

	//
	// Inner classes
	//

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
			getterVisibility = Visibility.NONE, //
			isGetterVisibility = Visibility.NONE, //
			setterVisibility = Visibility.NONE)
	public static class Session implements Comparable<Session> {
		// not private for testing purpose
		DateTime createdAt;
		private String accessToken;
		private DateTime accessTokenExpiresAt;

		private Session() {
			// use static helpers to instantiate sessions
		}

		public String accessToken() {
			return accessToken;
		}

		public DateTime createAt() {
			return createdAt;
		}

		public DateTime expiresAt() {
			return accessTokenExpiresAt;
		}

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
			session.createdAt = DateTime.now();
			session.accessToken = accessToken;
			// lifetime in seconds is converted to milliseconds
			session.accessTokenExpiresAt = session.createdAt.plus(lifetime * 1000);
			return session;
		}

		@Override
		// reversed from natural DateTime order to have
		// most recent sessions first and oldest session at the end
		public int compareTo(Session s) {
			if (this.createdAt == null)
				return +1;
			if (s.createdAt == null)
				return -1;
			return s.createdAt.compareTo(this.createdAt);
		}

	}
}