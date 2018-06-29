package io.spacedog.client.credentials;

import java.util.Set;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.http.SpaceFields;
import io.spacedog.client.http.SpaceParams;
import io.spacedog.client.http.SpaceRequest;
import io.spacedog.client.http.SpaceResponse;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.Optional7;

public class CredentialsClient implements SpaceParams, SpaceFields {

	SpaceDog dog;
	private Credentials credentials;

	public CredentialsClient(SpaceDog session) {
		this.dog = session;
	}

	//
	// Get credentials method
	//

	public Credentials me() {
		return me(false);
	}

	public Credentials me(boolean reload) {
		if (credentials == null || reload) {
			SpaceResponse response = dog.get("/1/credentials/me").go(200);
			credentials = Credentials.parse(response.asJsonObject());
		}
		return credentials;
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

		return Optional7.of(Credentials.parse(response.get("results.0")));
	}

	public Credentials get(String id) {
		SpaceResponse response = dog.get("/1/credentials/{id}")//
				.routeParam("id", id).go(200);
		return Credentials.parse(response.asJsonObject());
	}

	//
	// Get All Credentials
	//

	public Credentials.Results getAll() {
		return getAll(null, null, null);
	}

	public Credentials.Results getAll(int from, int size) {
		return getAll(null, from, size);
	}

	public Credentials.Results getAll(String q) {
		return getAll(q, null, null);
	}

	public Credentials.Results getAll(String q, Integer from, Integer size) {
		SpaceResponse response = dog.get("/1/credentials")//
				.queryParam("q", q).from(from).size(size).go(200);
		return Credentials.Results.parse(response.asJsonObject());
	}

	//
	// Login logout
	//

	public SpaceDog login(String password, long lifetime) {

		SpaceRequest request = SpaceRequest.get("/1/login")//
				.backend(dog.backend()).basicAuth(dog.username(), password);

		if (lifetime > 0)
			request.queryParam(LIFETIME_PARAM, lifetime);

		ObjectNode node = request.go(200).asJsonObject();
		dog.accessToken(Json.checkStringNotNullOrEmpty(node, ACCESS_TOKEN_FIELD));
		credentials = Credentials.parse(Json.checkObject(node.get("credentials")));

		return dog;
	}

	public SpaceDog logout() {
		if (dog.accessToken() != null) {
			dog.get("/1/logout").go(200);
			dog.accessToken(null);
			dog.expiresAt(null);
		}
		return dog;
	}

	//
	// Create credentials method
	//

	public SpaceDog create(String username, String password, String email, String... roles) {
		return create(new CredentialsCreateRequest()//
				.username(username).password(password).email(email).roles(roles));
	}

	public SpaceDog create(CredentialsCreateRequest request) {
		dog.post("/1/credentials").bodyPojo(request).go(201);
		return SpaceDog.dog(dog.backend())//
				.username(request.username())//
				.password(request.password());
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

	public void deleteByUsername(String username) {
		Optional7<Credentials> optional = dog.credentials().getByUsername(username);
		if (optional.isPresent())
			dog.credentials().delete(optional.get().id());
	}

	public CredentialsClient deleteAllButSuperAdmins() {
		dog.delete("/1/credentials").go(200);
		return this;
	}

	//
	// Update credentials methods
	//

	public CredentialsUpdateRequestBuilder prepareUpdate() {
		return prepareUpdate("me");
	}

	public CredentialsUpdateRequestBuilder prepareUpdate(String credentialsId) {

		return new CredentialsUpdateRequestBuilder(credentialsId) {

			@Override
			public SpaceResponse go(String password) {
				return update(this.request, password);
			}
		};
	}

	public SpaceResponse update(CredentialsUpdateRequest request) {
		return update(request, null);
	}

	public SpaceResponse update(CredentialsUpdateRequest updateRequest, String password) {

		SpaceRequest spaceRequest = dog.put("/1/credentials/{id}")//
				.routeParam("id", updateRequest.credentialsId)//
				.bodyPojo(updateRequest);

		if (password != null)
			spaceRequest.basicAuth(dog.username(), password);

		return spaceRequest.go(200);
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
						.routeParam("id", id).go(200).asPojo(String[].class));
	}

	public void setAllRoles(String id, String... roles) {
		dog.put("/1/credentials/{id}/roles")//
				.routeParam("id", id).bodyJson(Json.toJsonNode(roles)).go(200);
	}

	public void unsetAllRoles(String id) {
		dog.delete("/1/credentials/{id}/roles")//
				.routeParam("id", id).go(200);
	}

	//
	// Password methods
	//

	public String resetPassword(String id) {
		return dog.post("/1/credentials/{id}/_reset_password")//
				.routeParam("id", id).go(200)//
				.getString(PASSWORD_RESET_CODE_FIELD);
	}

	public void setMyPasswordWithCode(String newPassword, String passwordResetCode) {
		setPasswordWithCode(dog.id(), newPassword, passwordResetCode);
	}

	public void setPasswordWithCode(String credentialsId, //
			String newPassword, String passwordResetCode) {
		SpaceRequest.post("/1/credentials/{id}/_set_password")//
				.backend(dog.backend())//
				.routeParam("id", credentialsId)//
				.bodyPojo(new SetPasswordRequest()//
						.withPassword(newPassword)//
						.withPasswordResetCode(passwordResetCode))//
				.go(200);
	}

	public void setMyPassword(String oldPassword, String newPassword) {
		setPassword("me", oldPassword, newPassword);
	}

	public void setPassword(String credentialsId, //
			String requesterPassword, String newPassword) {
		SpaceRequest.post("/1/credentials/{id}/_set_password")//
				.backend(dog.backend())//
				.basicAuth(dog.username(), requesterPassword)//
				.routeParam("id", credentialsId)//
				.bodyPojo(new SetPasswordRequest().withPassword(newPassword))//
				.go(200);
	}

	public void passwordMustChange(String credentialsId) {
		dog.post("/1/credentials/{id}/_password_must_change")//
				.routeParam("id", credentialsId).go(200);
	}

	public void sendPasswordResetEmail(String username) {
		sendPasswordResetEmail(username, Json.object());
	}

	public void sendPasswordResetEmail(String username, ObjectNode parameters) {
		parameters.put(USERNAME_FIELD, username);
		dog.post("/1/credentials/_send_password_reset_email")//
				.bodyJson(parameters)//
				.go(200);
	}

	//
	// Enable methods
	//

	public void enable(String id) {
		enable(id, true);
	}

	public void disable(String id) {
		enable(id, false);
	}

	public void enable(String id, boolean enable) {
		StringBuilder builder = new StringBuilder("/1/credentials/") //
				.append(id).append(enable ? "/_enable" : "/_disable");
		dog.post(builder.toString()).go(200);
	}

	//
	// Credentials settings methods
	//

	public CredentialsSettings settings() {
		return dog.settings().get(CredentialsSettings.class);
	}

	public void enableGuestSignUp(boolean enable) {
		CredentialsSettings settings = settings();
		settings.guestSignUpEnabled = enable;
		dog.settings().save(settings);
	}

}
