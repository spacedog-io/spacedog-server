package io.spacedog.client.credentials;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class CredentialsCreateRequest {

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

	public CredentialsCreateRequest username(String username) {
		this.username = username;
		return this;
	}

	public String password() {
		return password;
	}

	public CredentialsCreateRequest password(String password) {
		this.password = password;
		return this;
	}

	public String email() {
		return email;
	}

	public CredentialsCreateRequest email(String email) {
		this.email = email;
		return this;
	}

	public String[] roles() {
		return roles;
	}

	public CredentialsCreateRequest roles(String... roles) {
		this.roles = roles;
		return this;
	}

	public boolean enabled() {
		return enabled;
	}

	public CredentialsCreateRequest enabled(boolean enabled) {
		this.enabled = enabled;
		return this;
	}

	public DateTime enableAfter() {
		return enableAfter;
	}

	public CredentialsCreateRequest enableAfter(DateTime enableAfter) {
		this.enableAfter = enableAfter;
		return this;
	}

	public DateTime disableAfter() {
		return disableAfter;
	}

	public CredentialsCreateRequest disableAfter(DateTime disableAfter) {
		this.disableAfter = disableAfter;
		return this;
	}

}
