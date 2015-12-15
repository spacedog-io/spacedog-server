/**
 * © David Attias 2015
 */
package io.spacedog.services;

import com.google.common.base.Strings;

public class Account {

	public String backendId;
	public String username;
	public String hashedPassword;
	public String email;
	public BackendKey backendKey;

	public void checkAccountInputValidity() {
		if (Strings.isNullOrEmpty(backendId))
			throw new InvalidAccountException("backend id must not be null or empty");

		if (backendId.indexOf('-') != -1)
			throw new InvalidAccountException("backend id must not contain any '-' character");

		if (backendId.indexOf('/') != -1)
			throw new InvalidAccountException("backend id must not contain any '/' character");

		if (backendId.indexOf('_') != -1)
			throw new InvalidAccountException("backend id must not contain any '_' character");

		if (Strings.isNullOrEmpty(email))
			throw new InvalidAccountException("account email is null or empty");

		if (Strings.isNullOrEmpty(username))
			throw new InvalidAccountException("account username is null or empty");

		if (Strings.isNullOrEmpty(hashedPassword))
			throw new InvalidAccountException("account password is null or empty");
	}

	public static void checkPasswordValidity(String password) {
		if (Strings.isNullOrEmpty(password))
			throw new InvalidAccountException("account password is null or empty");
	}

	@SuppressWarnings("serial")
	public static class InvalidAccountException extends RuntimeException {
		public InvalidAccountException(String message) {
			super(message);
		}
	}

	public String defaultClientKey() {
		return new StringBuilder(backendId).append(':').append(backendKey.name).append(':').append(backendKey.secret)
				.toString();
	}

	public Credentials credentials() {
		return Credentials.fromAdmin(backendId, username, email, backendKey);
	}
}
