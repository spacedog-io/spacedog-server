package io.spacedog.client;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;

import io.spacedog.model.BadgeStrategy;
import io.spacedog.model.PushProtocol;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class PushRequest {
	public String appId;
	public PushProtocol protocol;
	public BadgeStrategy badgeStrategy = BadgeStrategy.manual;
	public String[] credentialsIds;
	public String[] installationIds;
	public boolean usersOnly;
	public Set<String> tags;
	public boolean refresh;
	public String text;
	public ObjectNode data;

	public PushRequest appId(String appId) {
		this.appId = appId;
		return this;
	}

	public PushRequest protocol(PushProtocol protocol) {
		this.protocol = protocol;
		return this;
	}

	public PushRequest badgeStrategy(BadgeStrategy badgeStrategy) {
		this.badgeStrategy = badgeStrategy;
		return this;
	}

	public PushRequest credentialsId(String... credentialsId) {
		this.credentialsIds = credentialsId;
		return this;
	}

	public PushRequest installationId(String... installationId) {
		this.installationIds = installationId;
		return this;
	}

	public PushRequest tags(Set<String> tags) {
		this.tags = tags;
		return this;
	}

	public PushRequest tags(String... tags) {
		return tags(Sets.newHashSet(tags));
	}

	public PushRequest refresh(boolean refresh) {
		this.refresh = refresh;
		return this;
	}

	public PushRequest text(String text) {
		this.text = text;
		return this;
	}

	public PushRequest data(ObjectNode data) {
		this.data = data;
		return this;
	}

	public void usersOnly(boolean value) {
		this.usersOnly = value;
	}
}