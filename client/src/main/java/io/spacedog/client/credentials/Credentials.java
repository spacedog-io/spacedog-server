/**
 * © David Attias 2015
 */
package io.spacedog.client.credentials;

import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.spacedog.client.http.SpaceFields;
import io.spacedog.utils.Check;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.KeyValue;
import io.spacedog.utils.Optional7;
import io.spacedog.utils.Utils;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class Credentials {

	public static final Credentials GUEST = new Credentials("guest").id("guest");
	public static final Credentials SUPERDOG = new Credentials(Roles.superdog)//
			.id(Roles.superdog).addRoles(Roles.superdog)//
			.passwordHasBeenChallenged(true);

	private String username;
	private String email;
	private boolean enabled = true;
	private DateTime enableAfter;
	private DateTime disableAfter;
	private Set<String> roles = Sets.newHashSet();
	private String group;
	private List<Session> sessions;
	private Set<String> tags = Sets.newHashSet();
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

	public String username() {
		return username == null ? GUEST.username() : username;
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

	public Credentials passwordHasBeenChallenged(boolean passwordHasBeenChallenged) {
		this.passwordHasBeenChallenged = passwordHasBeenChallenged;
		return this;
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
		return Collections.unmodifiableSet(roles);
	}

	public Credentials clearRoles() {
		roles.clear();
		return this;
	}

	public Credentials addRoles(String... roles) {
		for (String role : roles) {
			Roles.checkIfValid(role);
			this.roles.add(role);
		}
		return this;
	}

	public Credentials removeRoles(String... roles) {
		for (String role : roles)
			this.roles.remove(role);
		return this;
	}

	public String group() {
		return group;
	}

	public Credentials group(String group) {
		this.group = group;
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

	public Set<String> tags() {
		return Collections.unmodifiableSet(tags);
	}

	public Credentials clearTags() {
		tags.clear();
		return this;
	}

	public Credentials setTag(String key, Object value) {
		this.tags.add(new KeyValue(key, value).asTag());
		return this;
	}

	public Optional7<String> getTag(String key) {
		for (String tag : tags) {
			KeyValue keyValue = KeyValue.parse(tag);
			if (key.equals(keyValue.getKey()))
				return Optional7.of(keyValue.getValue().toString());
		}
		return Optional7.empty();
	}

	public Credentials removeTag(String key) {
		Iterator<String> iterator = tags.iterator();
		while (iterator.hasNext()) {
			String tag = iterator.next();
			KeyValue keyValue = KeyValue.parse(tag);
			if (key.equals(keyValue.getKey()))
				iterator.remove();
		}
		return this;
	}

	//
	// Override
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
	// Type
	//

	public String type() {
		if (isSuperDog())
			return Roles.superdog;
		if (isSuperAdmin())
			return Roles.superadmin;
		if (isAdmin())
			return Roles.admin;
		if (isUser())
			return Roles.user;
		return GUEST.username();
	}

	public boolean isSuperDog() {
		return roles.contains(Roles.superdog);
	}

	public Credentials checkSuperDog() {
		if (!isSuperDog())
			throw Exceptions.insufficientCredentials(this);
		return this;
	}

	public boolean isSuperAdmin() {
		return roles.contains(Roles.superadmin);
	}

	public boolean isAtLeastSuperAdmin() {
		return isSuperAdmin() || isSuperDog();
	}

	public Credentials checkAtLeastSuperAdmin() {
		if (!isAtLeastSuperAdmin())
			throw Exceptions.insufficientCredentials(this);
		return this;
	}

	public boolean isAdmin() {
		return roles.contains(Roles.admin);
	}

	public boolean isAtLeastAdmin() {
		return isAdmin() || isAtLeastSuperAdmin();
	}

	public Credentials checkAtLeastAdmin() {
		if (!isAtLeastAdmin())
			throw Exceptions.insufficientCredentials(this);
		return this;
	}

	public boolean isUser() {
		return roles.contains(Roles.user);
	}

	public boolean isAtLeastUser() {
		return isUser() || isAtLeastAdmin();
	}

	public Credentials checkAtLeastUser() {
		if (!isAtLeastUser())
			throw Exceptions.insufficientCredentials(this);
		return this;
	}

	public boolean isGuest() {
		return GUEST.id().equals(id());
	}

	//
	// Can manage
	//

	public boolean canManage(String role) {
		return level() >= level(typeManaging(role));
	}

	public boolean canManage(String... roles) {
		if (roles != null)
			for (String role : roles)
				if (!canManage(role))
					return false;
		return true;
	}

	public Credentials checkCanManage(Credentials other) {
		checkCanManage(other.type());
		return this;
	}

	public Credentials checkCanManage(String... roles) {
		if (!canManage(roles))
			throw Exceptions.insufficientCredentials(this);
		return this;
	}

	private int level() {
		return level(type());
	}

	private static int level(String type) {
		if (Roles.superdog.equals(type))
			return 4;
		if (Roles.superadmin.equals(type))
			return 3;
		if (Roles.admin.equals(type))
			return 2;
		if (Roles.user.equals(type))
			return 1;
		return 0;
	}

	private static String typeManaging(String role) {
		if (Roles.superdog.equals(role))
			return Roles.superdog;
		if (Roles.superadmin.equals(role))
			return Roles.superadmin;
		if (Roles.admin.equals(role))
			return Roles.admin;
		if (Roles.user.equals(role))
			return Roles.user;
		return Roles.admin;
	}

	//
	// Check if authorized
	//

	public Credentials checkIfAuthorized(String... authorizedRoles) {
		checkIfAuthorized(Sets.newHashSet(authorizedRoles));
		return this;
	}

	public Credentials checkIfAuthorized(Iterable<String> authorizedRoles) {
		if (authorizedRoles != null) {
			for (String authorized : authorizedRoles)
				if (authorized.equals(Roles.all) //
						|| roles.contains(authorized))
					return this;
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
			throw Exceptions.forbidden("password reset code [%s] is invalid", //
					passwordResetCode);

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
		return Json.object(//
				SpaceFields.ID_FIELD, id(), //
				SpaceFields.USERNAME_FIELD, username(), //
				SpaceFields.EMAIL_FIELD, email().orElse(null), //
				SpaceFields.GROUP_FIELD, group(), //
				"reallyEnabled", isReallyEnabled(), //
				SpaceFields.ENABLED_FIELD, enabled(), //
				SpaceFields.ENABLE_AFTER_FIELD, enableAfter(), //
				SpaceFields.DISABLE_AFTER_FIELD, disableAfter(), //
				SpaceFields.INVALID_CHALLENGES_FIELD, invalidChallenges, //
				SpaceFields.LAST_INVALID_CHALLENGE_AT_FIELD, lastInvalidChallengeAt, //
				SpaceFields.ROLES_FIELD, roles(), //
				SpaceFields.CREATED_AT_FIELD, createdAt(), //
				SpaceFields.UPDATED_AT_FIELD, updatedAt());
	}

	public static Credentials parse(JsonNode node) {
		Credentials credentials = Json.toPojo(node, Credentials.class);
		String id = Json.checkStringNotNullOrEmpty(node, SpaceFields.ID_FIELD);
		return credentials.id(id);
	}

	@Override
	public String toString() {
		return String.format("[%s][%s]", type(), username());
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

	public void deleteSession(String accessToken) {
		if (sessions != null) {
			Iterator<Session> iterator = sessions.iterator();
			while (iterator.hasNext()) {
				Session session = iterator.next();
				if (session.accessToken.equals(accessToken)) {
					iterator.remove();
					return;
				}
			}
		}

		throw Exceptions.illegalArgument(//
				"access token [%s] not found in [%s][%s]", //
				type(), username);
	}

	public boolean isBrandNew() {
		return updatedAt == null ? true : updatedAt.equals(createdAt);
	}

	//
	// Inner classes
	//

	public static class Results {
		public long total;
		public List<Credentials> results;

		public static Results parse(ObjectNode node) {
			Results results = new Results();
			results.total = node.get("total").asLong();
			results.results = Lists.newArrayList();
			for (JsonNode credsNode : Json.checkArray(node.get("results")))
				results.results.add(Credentials.parse(credsNode));
			return results;
		}

		// TODO replace this by automatic jackson serialization
		public ObjectNode toJson() {
			ArrayNode array = Json.array();
			for (Credentials creds : this.results)
				array.add(creds.toJson());
			return Json.object("total", this.total, "results", array);
		}

	}

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