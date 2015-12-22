/**
 * © David Attias 2015
 */
package io.spacedog.services;

import com.google.common.base.Strings;

import io.spacedog.utils.BackendKey;
import io.spacedog.utils.Usernames;

public class Account {

	public String backendId;
	public String username;
	public String hashedPassword;
	public String email;
	public BackendKey backendKey;

	public void checkAccountInputValidity() {

		Usernames.checkIfValid(username);

		BackendKey.checkIfIdIsValid(backendId);

		if (Strings.isNullOrEmpty(email))
			throw new IllegalArgumentException("account email is null or empty");

		if (Strings.isNullOrEmpty(username))
			throw new IllegalArgumentException("account username is null or empty");

		if (Strings.isNullOrEmpty(hashedPassword))
			throw new IllegalArgumentException("account password is null or empty");
	}

	public String defaultClientKey() {
		return new StringBuilder(backendId).append(':').append(backendKey.name).append(':').append(backendKey.secret)
				.toString();
	}

	public Credentials credentials() {
		return Credentials.fromAdmin(backendId, username, email, backendKey);
	}
}
