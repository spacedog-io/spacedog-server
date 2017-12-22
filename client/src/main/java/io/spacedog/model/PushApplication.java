package io.spacedog.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class PushApplication {
	public String name;
	public String backendId;
	public PushProtocol protocol;
	public Map<String, String> attributes;
	@JsonIgnore
	public Credentials credentials;

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
			getterVisibility = Visibility.NONE, //
			isGetterVisibility = Visibility.NONE, //
			setterVisibility = Visibility.NONE)
	public static class Credentials {
		public String principal;
		public String credentials;
	}

	public PushApplication name(String name) {
		this.name = name;
		return this;
	}

	public PushApplication backendId(String backendId) {
		this.backendId = backendId;
		return this;
	}

	public PushApplication protocol(PushProtocol protocol) {
		this.protocol = protocol;
		return this;
	}

	public PushApplication principal(String principal) {
		if (this.credentials == null)
			this.credentials = new Credentials();
		this.credentials.principal = principal;
		return this;
	}

	public PushApplication credentials(String credentials) {
		if (this.credentials == null)
			this.credentials = new Credentials();
		this.credentials.credentials = credentials;
		return this;
	}

	public String id() {
		return backendId + '-' + name;
	}

}