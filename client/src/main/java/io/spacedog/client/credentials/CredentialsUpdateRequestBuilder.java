package io.spacedog.client.credentials;

import org.joda.time.DateTime;

import io.spacedog.client.http.SpaceResponse;

public abstract class CredentialsUpdateRequestBuilder {

	CredentialsUpdateRequest request;

	public CredentialsUpdateRequestBuilder(String credentialsId) {
		this.request = new CredentialsUpdateRequest(credentialsId);
	}

	public CredentialsUpdateRequestBuilder username(String username) {
		request.username = username;
		return this;
	}

	public CredentialsUpdateRequestBuilder email(String email) {
		request.email = email;
		return this;
	}

	public CredentialsUpdateRequestBuilder enabled(boolean enabled) {
		request.enabled = enabled;
		return this;
	}

	public CredentialsUpdateRequestBuilder enableDisableAfter(DateTime enable, DateTime disable) {
		request.enableDisableAfter(enable, disable);
		return this;
	}

	public SpaceResponse go() {
		return go(null);
	}

	public abstract SpaceResponse go(String password);
}