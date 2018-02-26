package io.spacedog.client.admin;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import io.spacedog.client.credentials.CreateCredentialsRequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class CreateBackendRequest {

	public enum Type {
		standard, dedicated
	}

	private String backendId;
	private Type type = Type.standard;
	private CreateCredentialsRequest superadmin;

	public Type type() {
		return type;
	}

	public CreateBackendRequest type(Type type) {
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
