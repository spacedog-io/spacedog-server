package com.magiclabs.restapi;

import java.util.List;

import com.google.common.base.Strings;
import com.magiclabs.restapi.Account.InvalidAccountException;

public class User {

	public String username;
	public String email;
	public String password;
	public List<String> groups;

	public void checkUserInputValidity() {
		if (Strings.isNullOrEmpty(email))
			throw new InvalidAccountException("user email is null or empty");
		if (Strings.isNullOrEmpty(username))
			throw new InvalidAccountException("user name is null or empty");
		if (Strings.isNullOrEmpty(password))
			throw new InvalidAccountException("user password is null or empty");
	}
}
