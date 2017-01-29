package io.spacedog.sdk;

import java.util.Optional;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Json;
import io.spacedog.utils.SpaceFields;
import io.spacedog.utils.SpaceParams;

public class SpaceCredentials implements SpaceParams, SpaceFields {

	SpaceDog dog;

	SpaceCredentials(SpaceDog session) {
		this.dog = session;
	}

	public Credentials me() {
		return dog.credentials;
	}

	public Credentials create(String username, String password, String email) {
		return create(username, password, email, false);
	}

	public Credentials create(String username, String password, String email, boolean admin) {

		ObjectNode body = Json.object(FIELD_USERNAME, username, //
				FIELD_PASSWORD, password, FIELD_EMAIL, email);

		if (admin)
			body.put(FIELD_LEVEL, "ADMIN");

		String id = dog.post("/1/credentials")//
				.body(body).go(201).getString(FIELD_ID);

		Credentials credentials = new Credentials(dog.backendId(), username);
		credentials.id(id);
		credentials.email(email);
		return credentials;
	}

	public void delete(String id) {
		SpaceRequest.delete("/1/credentials/{id}")//
				.routeParam("id", id).auth(dog).go(200, 404);
	}

	public Optional<Credentials> getByUsername(String username) {
		ObjectNode node = dog.get("/1/credentials")//
				.queryParam(PARAM_USERNAME, username)//
				.go(200).objectNode();

		if (node.get("total").asInt() == 0)
			return Optional.empty();

		// TODO throw exception if total > 1
		return Optional.of(toCredentials((ObjectNode) Json.get(node, "results.0")));
	}

	public SpaceCredentials deleteAllButSuperAdmins() {
		dog.delete("/1/credentials").go(200);
		return this;
	}

	public void setRole(String id, String role) {
		dog.put("/1/credentials/{id}/roles/{role}")//
				.routeParam("id", id).routeParam("role", role).go(200);
	}

	public Credentials get(String id) {
		return toCredentials(dog.get("/1/credentials/{id}")//
				.routeParam("id", id).go(200).objectNode());
	}

	private Credentials toCredentials(ObjectNode node) {
		// TODO use json mapper
		Credentials credentials = new Credentials(dog.backendId());
		credentials.id(Json.get(node, "id").asText());
		credentials.email(Json.get(node, "email").asText());
		credentials.name(Json.get(node, "username").asText());
		return credentials;
	}

	public void passwordMustChange(String id) {
		dog.put("/1/credentials/{id}/passwordMustChange")//
				.routeParam("id", id).body("true").go(200);
	}

	public void updateMyPassword(String oldPassword, String newPassword) {
		updatePassword("me", oldPassword, newPassword);
	}

	public void updatePassword(String id, String requesterPassword, String newPassword) {
		SpaceRequest.put("/1/credentials/{id}/password")//
				.routeParam("id", id)//
				.basicAuth(dog.backendId(), dog.username(), requesterPassword)//
				.queryParam("password", newPassword)//
				.go(200);
	}

}
