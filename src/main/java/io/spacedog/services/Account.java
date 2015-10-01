package io.spacedog.services;

import com.google.common.base.Strings;

public class Account {

	public String backendId;
	public String username;
	public String password;
	public String email;
	public ApiKey apiKey;

	public void checkAccountInputValidity() {
		if (Strings.isNullOrEmpty(backendId))
			throw new InvalidAccountException(
					"backend id must not be null or empty");

		if (backendId.indexOf('-') != -1)
			throw new InvalidAccountException(
					"backend id must not contain any '-' character");

		if (backendId.indexOf('/') != -1)
			throw new InvalidAccountException(
					"backend id must not contain any '/' character");

		if (backendId.indexOf('_') != -1)
			throw new InvalidAccountException(
					"backend id must not contain any '_' character");

		if (Strings.isNullOrEmpty(email))
			throw new InvalidAccountException("account email is null or empty");

		if (Strings.isNullOrEmpty(username))
			throw new InvalidAccountException(
					"account username is null or empty");

		if (Strings.isNullOrEmpty(password))
			throw new InvalidAccountException(
					"account password is null or empty");
	}

	@SuppressWarnings("serial")
	public static class InvalidAccountException extends RuntimeException {
		public InvalidAccountException(String message) {
			super(message);
		}
	}

	public String spaceDogKey() {
		return new StringBuilder().append(backendId).append(':')
				.append(apiKey.id).append(':').append(apiKey.secret).toString();
	}
}
