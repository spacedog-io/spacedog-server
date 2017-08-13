package io.spacedog.sdk;

import java.util.Set;

import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Sets;

import io.spacedog.model.CreateCredentialsRequest;
import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceResponse;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json7;
import io.spacedog.utils.Optional7;
import io.spacedog.utils.SpaceFields;
import io.spacedog.utils.SpaceParams;

public class CredentialsEndpoint implements SpaceParams, SpaceFields {

	SpaceDog dog;

	CredentialsEndpoint(SpaceDog session) {
		this.dog = session;
	}

	//
	// Get credentials method
	//

	public Credentials me() {
		return me(false);
	}

	public Credentials me(boolean reload) {
		if (dog.credentials == null)
			throw Exceptions.illegalState("you must login first");

		if (reload) {
			SpaceResponse response = dog.get("/1/credentials/me").go(200);
			dog.credentials = toCredentials(response.asJsonObject());
		}

		return dog.credentials;
	}

	public Optional7<Credentials> getByUsername(String username) {
		SpaceResponse response = dog.get("/1/credentials")//
				.queryParam(USERNAME_PARAM, username)//
				.go(200);

		int total = response.get("total").asInt();

		if (total == 0)
			return Optional7.empty();

		if (total > 1)
			throw Exceptions.runtime("[%s] credentials with username [%s]", //
					total, username);

		return Optional7.of(toCredentials(response.get("results.0")));
	}

	public Credentials get(String id) {
		return toCredentials(//
				dog.get("/1/credentials/{id}")//
						.routeParam("id", id).go(200).asJsonObject());
	}

	//
	// Create credentials method
	//

	public String create(String username, String password, String email, String... roles) {
		return create(new CreateCredentialsRequest().username(username).password(password)//
				.email(email).roles(roles));
	}

	public String create(CreateCredentialsRequest request) {
		return dog.post("/1/credentials")//
				.bodyPojo(request).go(201).getString(ID_FIELD);
	}

	//
	// Delete credentials method
	//

	public void delete() {
		dog.delete("/1/credentials/me").go(200);
	}

	public void delete(String id) {
		dog.delete("/1/credentials/{id}")//
				.routeParam("id", id).go(200, 404);
	}

	public CredentialsEndpoint deleteAllButSuperAdmins() {
		dog.delete("/1/credentials").go(200);
		return this;
	}

	//
	// Update credentials methods
	//

	public CredentialsUpdateRequest prepareUpdate() {
		return new CredentialsUpdateRequest("me");
	}

	public CredentialsUpdateRequest prepareUpdate(String credentialsId) {
		return new CredentialsUpdateRequest(credentialsId);
	}

	public class CredentialsUpdateRequest {

		String credentialsId;
		String username;
		String newPassword;
		String email;
		Boolean enabled;
		Optional7<DateTime> enableAfter;
		Optional7<DateTime> disableAfter;

		private CredentialsUpdateRequest(String credentialsId) {
			this.credentialsId = credentialsId;
		}

		public CredentialsUpdateRequest username(String username) {
			this.username = username;
			return this;
		}

		public CredentialsUpdateRequest newPassword(String password) {
			this.newPassword = password;
			return this;
		}

		public CredentialsUpdateRequest email(String email) {
			this.email = email;
			return this;
		}

		public CredentialsUpdateRequest enabled(boolean enabled) {
			this.enabled = enabled;
			return this;
		}

		public CredentialsUpdateRequest enableAfter(Optional7<DateTime> enableAfter) {
			this.enableAfter = enableAfter;
			return this;
		}

		public CredentialsUpdateRequest disableAfter(Optional7<DateTime> disableAfter) {
			this.disableAfter = disableAfter;
			return this;
		}

		public void go() {
			go(null);
		}

		public void go(String password) {
			ObjectNode body = Json7.object();

			if (username != null)
				body.put(USERNAME_FIELD, username);
			if (newPassword != null)
				body.put(PASSWORD_FIELD, newPassword);
			if (email != null)
				body.put(EMAIL_FIELD, email);
			if (enabled != null)
				body.put(ENABLED_FIELD, enabled);
			if (enableAfter != null)
				body.put(ENABLE_AFTER_FIELD, enableAfter.isPresent() //
						? enableAfter.get().toString()
						: null);
			if (disableAfter != null)
				body.put(DISABLE_AFTER_FIELD, disableAfter.isPresent() //
						? disableAfter.get().toString()
						: null);

			SpaceRequest request = dog.put("/1/credentials/{id}")//
					.routeParam("id", credentialsId).bodyJson(body);

			if (password != null)
				request.basicAuth(dog.username(), password);

			else if (dog.password().isPresent())
				request.basicAuth(dog);

			request.go(200);
		}
	}

	//
	// Roles method
	//

	public void setRole(String id, String role) {
		dog.put("/1/credentials/{id}/roles/{role}")//
				.routeParam("id", id).routeParam("role", role).go(200);
	}

	public void unsetRole(String id, String role) {
		dog.delete("/1/credentials/{id}/roles/{role}")//
				.routeParam("id", id).routeParam("role", role).go(200);
	}

	public Set<String> getAllRoles(String id) {
		return Sets.newHashSet(//
				dog.get("/1/credentials/{id}/roles")//
						.routeParam("id", id).go(200).toPojo(String[].class));
	}

	public void setAllRoles(String id, String... roles) {
		dog.put("/1/credentials/{id}/roles")//
				.routeParam("id", id).bodyJson(Json7.toNode(roles)).go(200);
	}

	public void unsetAllRoles(String id) {
		dog.delete("/1/credentials/{id}/roles")//
				.routeParam("id", id).go(200);
	}

	//
	// Password methods
	//

	public String deletePassword(String id) {
		return dog.delete("/1/credentials/{id}/password")//
				.routeParam("id", id).go(200)//
				.getString(PASSWORD_RESET_CODE_FIELD);
	}

	public void resetPassword(String id, String resetCode, String newPassword) {
		dog.post("/1/credentials/{id}/password")//
				.routeParam("id", id)//
				.formField(PASSWORD_RESET_CODE_FIELD, resetCode)//
				.formField(PASSWORD_FIELD, newPassword)//
				.go(200);
	}

	public void updateMyPassword(String oldPassword, String newPassword) {
		updatePassword("me", oldPassword, newPassword);
	}

	public void updatePassword(String id, String requesterPassword, String newPassword) {
		SpaceRequest.put("/1/credentials/{id}/password").backend(dog)//
				.routeParam("id", id)//
				.basicAuth(dog.username(), requesterPassword)//
				.bodyJson(TextNode.valueOf(newPassword))//
				.go(200);
	}

	public void passwordMustChange(String id) {
		dog.put("/1/credentials/{id}/passwordMustChange")//
				.routeParam("id", id).bodyString("true").go(200);
	}

	public void forgotMyPassword() {
		forgotMyPassword(Json7.object());
	}

	public void forgotMyPassword(ObjectNode parameters) {
		parameters.put(USERNAME_PARAM, dog.username());
		dog.post("/1/credentials/forgotPassword").bodyJson(parameters).go(200);
	}

	//
	// Enable methods
	//

	public void enable(String id, boolean enable) {
		dog.put("/1/credentials/{id}/enabled").routeParam("id", id)//
				.bodyJson(BooleanNode.valueOf(enable)).go(200);
	}

	//
	// Implementation
	//

	private Credentials toCredentials(JsonNode node) {
		Credentials credentials = Json7.toPojo(node, Credentials.class);
		credentials.id(node.get(ID_FIELD).asText());
		return credentials;
	}
}
