package io.spacedog.model;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class CreateCredentialsRequest {

	private String username;
	private String password;
	private String email;
	private String[] roles;
	private boolean enabled = true;
	private DateTime enableAfter;
	private DateTime disableAfter;

	public String username() {
		return username;
	}

	public CreateCredentialsRequest username(String username) {
		this.username = username;
		return this;
	}

	public String password() {
		return password;
	}

	public CreateCredentialsRequest password(String password) {
		this.password = password;
		return this;
	}

	public String email() {
		return email;
	}

	public CreateCredentialsRequest email(String email) {
		this.email = email;
		return this;
	}

	public String[] roles() {
		return roles;
	}

	public CreateCredentialsRequest roles(String... roles) {
		this.roles = roles;
		return this;
	}

	public boolean enabled() {
		return enabled;
	}

	public CreateCredentialsRequest enabled(boolean enabled) {
		this.enabled = enabled;
		return this;
	}

	public DateTime enableAfter() {
		return enableAfter;
	}

	public CreateCredentialsRequest enableAfter(DateTime enableAfter) {
		this.enableAfter = enableAfter;
		return this;
	}

	public DateTime disableAfter() {
		return disableAfter;
	}

	public CreateCredentialsRequest disableAfter(DateTime disableAfter) {
		this.disableAfter = disableAfter;
		return this;
	}

}
