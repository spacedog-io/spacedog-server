package io.spacedog.client.admin;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.spacedog.client.credentials.CredentialsCreateRequest;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class BackendCreateRequest {

	private String backendId;
	private Backend.Type type = Backend.Type.standard;
	private CredentialsCreateRequest superadmin;

	public Backend.Type type() {
		return type;
	}

	public BackendCreateRequest type(Backend.Type type) {
		this.type = type;
		return this;
	}

	public String backendId() {
		return backendId;
	}

	public BackendCreateRequest backendId(String backendId) {
		this.backendId = backendId;
		return this;
	}

	public CredentialsCreateRequest superadmin() {
		return superadmin;
	}

	public BackendCreateRequest superadmin(CredentialsCreateRequest superadmin) {
		this.superadmin = superadmin;
		return this;
	}

}
