package io.spacedog.sdk;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Sets;

import io.spacedog.model.BadgeStrategy;
import io.spacedog.model.PushService;
import io.spacedog.model.PushTag;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class PushRequest {
	public String appId;
	public PushService pushService;
	public BadgeStrategy badgeStrategy;
	public boolean usersOnly;
	public Set<PushTag> tags;
	public boolean refresh;
	public Object message;

	public PushRequest appId(String appId) {
		this.appId = appId;
		return this;
	}

	public PushRequest pushService(PushService pushService) {
		this.pushService = pushService;
		return this;
	}

	public PushRequest badgeStrategy(BadgeStrategy badgeStrategy) {
		this.badgeStrategy = badgeStrategy;
		return this;
	}

	public PushRequest tags(Set<PushTag> tags) {
		this.tags = tags;
		return this;
	}

	public PushRequest tags(PushTag... tags) {
		return tags(Sets.newHashSet(tags));
	}

	public PushRequest refresh(boolean refresh) {
		this.refresh = refresh;
		return this;
	}

	public PushRequest message(Object message) {
		this.message = message;
		return this;
	}

	public void usersOnly(boolean value) {
		this.usersOnly = value;
	}
}