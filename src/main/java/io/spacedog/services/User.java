package io.spacedog.services;

import io.spacedog.services.Account.InvalidAccountException;

import java.util.ArrayList;
import java.util.List;

import com.eclipsesource.json.JsonObject;
import com.google.common.base.Strings;

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

	public JsonObject toJsonObject() {
		return Json.builder().add("username", username).add("email", email)
				.add("password", password).stArr("groups").add(groups).build();
	}

	public static User fromJsonObject(JsonObject json) {
		User user = new User();
		user.username = json.get("username").asString();
		user.email = json.get("email").asString();
		user.password = json.get("password").asString();
		user.groups = new ArrayList<String>(json.get("groups").asArray().size());
		json.get("groups").asArray()
				.forEach(jsonValue -> user.groups.add(jsonValue.asString()));
		return user;
	}
}
