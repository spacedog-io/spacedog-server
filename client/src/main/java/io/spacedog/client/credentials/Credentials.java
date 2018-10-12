/**
 * © David Attias 2015
 */
package io.spacedog.client.credentials;

import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.spacedog.client.http.SpaceFields;
import io.spacedog.utils.Check;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.KeyValue;
import io.spacedog.utils.Optional7;
import io.spacedog.utils.Utils;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.NONE, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class Credentials implements SpaceFields {

	public static final Credentials GUEST = new Credentials("guest").id("guest");
	public static final Credentials SUPERDOG = new Credentials(Roles.superdog)//
			.id(Roles.superdog).addRoles(Roles.superdog)//
			.passwordHasBeenChallenged(true);

	@JsonProperty
	private String id;
	@JsonProperty
	private long version;
	@JsonProperty
	private String username;
	@JsonProperty
	private String email;
	@JsonProperty
	private boolean enabled = true;
	@JsonProperty
	private DateTime enableAfter;
	@JsonProperty
	private DateTime disableAfter;
	@JsonProperty
	private Set<String> roles = Sets.newHashSet();
	@JsonProperty
	private Set<String> groups;
	@JsonProperty
	private Set<String> tags = Sets.newHashSet();
	@JsonProperty
	private boolean passwordMustChange;
	@JsonProperty
	private int invalidChallenges;
	@JsonProperty
	private DateTime lastInvalidChallengeAt;
	@JsonProperty
	private DateTime createdAt;
	@JsonProperty
	private DateTime updatedAt;

	@JsonIgnore
	private String hashedPassword;
	@JsonIgnore
	private String passwordResetCode;
	@JsonIgnore
	private Session currentSession;
	@JsonIgnore
	private List<Session> sessions;
	@JsonIgnore
	private boolean passwordHasBeenChallenged;

	public Credentials() {
	}

	public Credentials(String username) {
		this.username = username;
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
		return username;
	}

	public Credentials username(String name) {
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

	public Credentials passwordResetCode(String passwordResetCode) {
		this.passwordResetCode = passwordResetCode;
		return this;
	}

	public String hashedPassword() {
		return hashedPassword;
	}

	public Credentials hashedPassword(String hashedPassword) {
		this.hashedPassword = hashedPassword;
		return this;
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
		if (roles != null)
			for (String role : roles) {
				Roles.checkIfValid(role);
				this.roles.add(role);
			}
		return this;
	}

	public Credentials removeRoles(String... roles) {
		if (roles != null)
			for (String role : roles)
				this.roles.remove(role);
		return this;
	}

	public DateTime createdAt() {
		return createdAt;
	}

	public Credentials createdAt(DateTime value) {
		createdAt = value;
		return this;
	}

	public DateTime updatedAt() {
		return updatedAt;
	}

	public Credentials updatedAt(DateTime value) {
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
	// Groups
	//

	public String group() {
		return id;
	}

	public Set<String> groups() {
		Set<String> result = Sets.newHashSet(id);
		if (groups != null)
			result.addAll(groups);
		return result;
	}

	public Credentials addGroup(String group) {
		if (groups == null)
			groups = Sets.newHashSet();
		groups.add(group);
		return this;
	}

	public Credentials removeGroup(String group) {
		if (groups != null)
			groups.remove(group);
		return this;
	}

	public boolean hasGroupAccessTo(String group) {
		return groups == null ? false : groups.contains(group);
	}

	public Credentials createGroup(String suffix) {
		return addGroup(id + "__" + suffix);
	}

	public String checkInitGroupTo(String group) {
		if (Strings.isNullOrEmpty(group))
			return this.group();

		checkGroupAccessTo(group);
		return group;
	}

	public String checkUpdateGroupTo(String oldGroup, String newGroup) {
		if (Strings.isNullOrEmpty(newGroup) || newGroup.equals(oldGroup))
			return oldGroup;

		checkGroupAccessTo(newGroup);
		return newGroup;
	}

	public void checkGroupAccessTo(String group) {
		if (id.equals(group))
			return;
		if (Utils.isNullOrEmpty(groups) //
				|| !groups.contains(group))
			throw Exceptions.forbidden("[%s][%s] not authorized for group [%s]", //
					type(), username(), group);
	}

	//
	// Stash
	//

	public Set<String> tags() {
		return Collections.unmodifiableSet(tags);
	}

	public Credentials tags(Iterable<String> tags) {
		if (tags == null)
			this.tags.clear();
		else
			this.tags = Sets.newHashSet(tags);
		return this;
	}

	public Credentials addTag(String key, Object value) {
		return addTag(new KeyValue(key, value).asTag());
	}

	public Credentials addTag(String tag) {
		this.tags.add(tag);
		return this;
	}

	public Set<String> getTagValues(String key) {
		Set<String> values = Sets.newHashSet();
		for (String tag : tags) {
			KeyValue keyValue = KeyValue.parse(tag);
			if (key.equals(keyValue.getKey()))
				values.add(keyValue.getValue().toString());
		}
		return values;
	}

	public Credentials removeTags(String key) {
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
		if (obj instanceof Credentials == false)
			return false;
		Credentials other = (Credentials) obj;
		return Objects.equals(username, other.username);
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

	public Credentials checkRoleAccess(String... authorizedRoles) {
		checkRoleAccess(Sets.newHashSet(authorizedRoles));
		return this;
	}

	public Credentials checkRoleAccess(Iterable<String> authorizedRoles) {
		if (authorizedRoles != null) {
			for (String authorized : authorizedRoles)
				if (authorized.equals(Roles.all) //
						|| roles.contains(authorized))
					return this;
		}
		throw Exceptions.insufficientCredentials(this);
	}

	public void checkOwnerAccess(String owner, String objectType, String objectId) {
		if (!id().equals(owner))
			throw Exceptions.forbidden("[%s][%s] not owner of [%s][%s]", //
					type(), username(), objectType, objectId);
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

	@Override
	public String toString() {
		return String.format("[%s][%s]", type(), username());
	}

	//
	// Sessions and Access Tokens
	//

	public List<Session> sessions() {
		if (sessions == null)
			return Collections.emptyList();
		return Collections.unmodifiableList(sessions);
	}

	public Credentials sessions(Iterable<Session> sessions) {
		this.sessions = Lists.newArrayList(sessions);
		return this;
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
			if (obj instanceof Session == false)
				return false;

			Session other = (Session) obj;
			return Objects.equals(accessToken, other.accessToken);
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