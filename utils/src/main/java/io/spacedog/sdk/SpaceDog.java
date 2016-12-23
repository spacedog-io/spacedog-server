package io.spacedog.sdk;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Json;

public class SpaceDog {

	String backendId;
	String accessToken;
	String username;
	String email;

	private SpaceDog(String backendId) {
		this.backendId = backendId;
	}

	public String backendId() {
		return backendId;
	}

	public String username() {
		return username;
	}

	public String email() {
		return email;
	}

	public static SpaceDog login(String backendId, String username, String password) {
		ObjectNode node = SpaceRequest.get("/1/login")//
				.basicAuth(backendId, username, password)//
				.go(200).objectNode();

		SpaceDog session = new SpaceDog(//
				Json.checkStringNotNullOrEmpty(node, "credentials.backendId"));
		session.accessToken = Json.checkStringNotNullOrEmpty(node, "accessToken");
		session.username = Json.checkStringNotNullOrEmpty(node, "credentials.username");
		session.email = Json.checkStringNotNullOrEmpty(node, "credentials.email");
		return session;
	}

	public void logout() {
		SpaceRequest.get("/1/logout").backendId(backendId)//
				.bearerAuth(accessToken).go(200);
	}

	SpaceData data;

	public SpaceData data() {
		if (data == null)
			data = new SpaceData(this);

		return data;
	}

	SpaceSettings settings;

	public SpaceSettings settings() {
		if (settings == null)
			settings = new SpaceSettings(this);

		return settings;
	}

	SpaceStripe stripe;

	public SpaceStripe stripe() {
		if (stripe == null)
			stripe = new SpaceStripe(this);

		return stripe;
	}

}
