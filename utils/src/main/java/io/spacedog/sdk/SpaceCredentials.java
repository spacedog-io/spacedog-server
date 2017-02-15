package io.spacedog.sdk;

import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Sets;

import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Credentials.Level;
import io.spacedog.utils.Exceptions;
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
		return create(username, password, email, Level.USER);
	}

	public Credentials create(String username, String password, String email, Level level) {

		ObjectNode body = Json.object(FIELD_USERNAME, username, //
				FIELD_PASSWORD, password, FIELD_EMAIL, email);

		if (level.ordinal() > Level.USER.ordinal())
			body.put(FIELD_LEVEL, level.toString());

		String id = dog.post("/1/credentials")//
				.body(body).go(201).getString(FIELD_ID);

		Credentials credentials = new Credentials(dog.backendId(), username);
		credentials.id(id);
		credentials.email(email);
		credentials.level(level);
		return credentials;
	}

	public void delete() {
		delete(dog.id());
	}

	public void delete(String id) {
		dog.delete("/1/credentials/{id}")//
				.routeParam("id", id).go(200, 404);
	}

	public Optional<Credentials> getByUsername(String username) {
		ObjectNode node = dog.get("/1/credentials")//
				.queryParam(PARAM_USERNAME, username)//
				.go(200).objectNode();

		int total = node.get("total").asInt();

		if (total == 0)
			return Optional.empty();

		if (total > 1)
			throw Exceptions.runtime("[%s] credentials with username [%s]", //
					total, username);

		return Optional.of(toCredentials(Json.get(node, "results.0")));
	}

	private Credentials toCredentials(JsonNode node) {
		try {
			Credentials credentials = Json.mapper()//
					.treeToValue(node, Credentials.class);
			credentials.id(node.get(FIELD_ID).asText());
			return credentials;

		} catch (JsonProcessingException e) {
			throw Exceptions.runtime(e);
		}
	}

	public SpaceCredentials deleteAllButSuperAdmins() {
		dog.delete("/1/credentials").go(200);
		return this;
	}

	public void setRole(String id, String role) {
		dog.put("/1/credentials/{id}/roles/{role}")//
				.routeParam("id", id).routeParam("role", role).go(200);
	}

	public void unsetRole(String id, String role) {
		dog.delete("/1/credentials/{id}/roles/{role}")//
				.routeParam("id", id).routeParam("role", role).go(200);
	}

	public Set<String> getRoles(String id) {
		return Sets.newHashSet(//
				dog.get("/1/credentials/{id}/roles")//
						.routeParam("id", id).go(200).toObject(String[].class));
	}

	public void setAllRoles(String id, String... roles) {
		dog.put("/1/credentials/{id}/roles")//
				.routeParam("id", id).body(Json.toNode(roles)).go(200);
	}

	public void unsetAllRoles(String id) {
		dog.delete("/1/credentials/{id}/roles")//
				.routeParam("id", id).go(200);
	}

	public Credentials get(String id) {
		return toCredentials(//
				dog.get("/1/credentials/{id}")//
						.routeParam("id", id).go(200).objectNode());
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
				.body(TextNode.valueOf(newPassword))//
				.go(200);
	}

	public void forgotPassword() {
		forgotPassword(Json.object());
	}

	public void forgotPassword(ObjectNode parameters) {
		parameters.put(PARAM_USERNAME, dog.username());
		dog.post("/1/credentials/forgotPassword").body(parameters).go(200);
	}

}
