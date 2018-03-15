package io.spacedog.client.admin;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.spacedog.client.credentials.CreateCredentialsRequest;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class CreateBackendRequest {

	private String backendId;
	private Backend.Type type = Backend.Type.standard;
	private CreateCredentialsRequest superadmin;

	public Backend.Type type() {
		return type;
	}

	public CreateBackendRequest type(Backend.Type type) {
		this.type = type;
		return this;
	}

	public String backendId() {
		return backendId;
	}

	public CreateBackendRequest backendId(String backendId) {
		this.backendId = backendId;
		return this;
	}

	public CreateCredentialsRequest superadmin() {
		return superadmin;
	}

	public CreateBackendRequest superadmin(CreateCredentialsRequest superadmin) {
		this.superadmin = superadmin;
		return this;
	}

}
