package io.spacedog.client.credentials;

import org.joda.time.DateTime;

public class CredentialsUpdateRequest {

	public String credentialsId;
	public String username;
	public String email;
	public Boolean enabled;
	public EnableDisableAfter enableDisableAfter;

	public static class EnableDisableAfter {
		public DateTime enable;
		public DateTime disable;
	}

	public CredentialsUpdateRequest() {
	}

	public CredentialsUpdateRequest(String credentialsId) {
		this.credentialsId = credentialsId;
	}

	public CredentialsUpdateRequest username(String username) {
		this.username = username;
		return this;
	}

	public CredentialsUpdateRequest email(String email) {
		this.email = email;
		return this;
	}

	public CredentialsUpdateRequest enabled(boolean enabled) {
		this.enabled = enabled;
		return this;
	}

	public CredentialsUpdateRequest enableDisableAfter(DateTime enable, DateTime disable) {
		this.enableDisableAfter = new EnableDisableAfter();
		this.enableDisableAfter.enable = enable;
		this.enableDisableAfter.disable = disable;
		return this;
	}
}