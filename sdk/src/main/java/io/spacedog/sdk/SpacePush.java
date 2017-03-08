package io.spacedog.sdk;

import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.spacedog.rest.SpaceRequest;
import io.spacedog.utils.Check;

public class SpacePush {

	public static class PushRequest {
		public String appId;
		// TODO use or reuse enums ?
		// TODO uncomment this after SpaceDog 0.27 MEP
		// public String badgeStrategy;
		// public String pushService;
		public List<PushTag> tags;
		public boolean refresh;
		public Object message;

		public static class PushTag {
			public String key;
			public String value;

			public PushTag(String key, String value) {
				this.key = key;
				this.value = value;
			}
		}
	}

	SpaceDog dog;

	SpacePush(SpaceDog dog) {
		this.dog = dog;
	}

	public ObjectNode push(String installationId, String message) {
		return SpaceRequest.post("/1/installation/{id}/push")//
				.routeParam("id", installationId).auth(dog)//
				.body(TextNode.valueOf(message)).go(200, 404).objectNode();
	}

	public ObjectNode push(String installationId, ObjectNode message) {
		return SpaceRequest.post("/1/installation/{id}/push")//
				.routeParam("id", installationId).auth(dog)//
				.body(message).go(200, 404).objectNode();
	}

	public ObjectNode push(PushRequest request) {
		Check.notNull(request.appId, "appId");
		ObjectNode response = SpaceRequest.post("/1/push")//
				.auth(dog).bodyPojo(request).go(200, 404).objectNode();
		return response;
	}

}
