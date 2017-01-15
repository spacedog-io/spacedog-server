package io.spacedog.utils;

public class CredentialsSettings extends Settings {

	// default token lifetime is 24h
	public static final long SESSION_DEFAULT_LIFETIME = 60 * 60 * 24;

	// TODO all fields should be private
	public boolean disableGuestSignUp;
	public String usernameRegex = Usernames.USERNAME_DEFAULT_REGEX;
	public String passwordRegex = Passwords.PASSWORD_DEFAULT_REGEX;
	public String linkedinId;
	public String linkedinSecret;
	public boolean useLinkedinExpiresIn = true;
	@Deprecated
	public String linkedinRedirectUri;
	public String linkedinFinalRedirectUri;
	// in seconds
	public long sessionMaximumLifetime = SESSION_DEFAULT_LIFETIME;
	public int maximumInvalidChallenges = 0;
	public int resetInvalidChallengesAfterMinutes = 60;

	public String usernameRegex() {
		return usernameRegex == null ? Usernames.USERNAME_DEFAULT_REGEX : usernameRegex;
	}

	public String passwordRegex() {
		return passwordRegex == null ? Passwords.PASSWORD_DEFAULT_REGEX : passwordRegex;
	}
}