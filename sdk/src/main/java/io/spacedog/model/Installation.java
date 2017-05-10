package io.spacedog.model;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Sets;

import io.spacedog.sdk.DataObject;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class Installation extends DataObject<Installation> {

	private String appId;
	private PushService pushService;
	private String token;
	private String endpoint;
	@JsonProperty("userId")
	private String username;
	private int badge;
	private Set<PushTag> tags;

	public String appId() {
		return appId;
	}

	public Installation appId(String appId) {
		this.appId = appId;
		return this;
	}

	public PushService pushService() {
		return pushService;
	}

	public Installation pushService(PushService pushService) {
		this.pushService = pushService;
		return this;
	}

	public String token() {
		return token;
	}

	public Installation token(String token) {
		this.token = token;
		return this;
	}

	public String endpoint() {
		return endpoint;
	}

	public String username() {
		return username;
	}

	public int badge() {
		return badge;
	}

	public Installation badge(int badge) {
		this.badge = badge;
		return this;
	}

	public Set<PushTag> tags() {
		return tags;
	}

	public Installation tags(PushTag... tags) {
		return tags(Sets.newHashSet(tags));
	}

	public Installation tags(Set<PushTag> tags) {
		this.tags = tags;
		return this;
	}

}
