package io.spacedog.client.credentials;

import java.util.Set;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.credentials.Credentials.Results;
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
		if (credentials == null || reload)
			credentials = dog.get("/2/credentials/me")//
					.go(200).asPojo(Credentials.class);
		return credentials;
	}

	public Optional7<Credentials> getByUsername(String username) {
		Results results = dog.get("/2/credentials")//
				.queryParam(USERNAME_PARAM, username)//
				.go(200)//
				.asPojo(Credentials.Results.class);

		if (results.total == 0)
			return Optional7.empty();

		if (results.total > 1)
			throw Exceptions.runtime("[%s] credentials with username [%s]", //
					results.total, username);

		return Optional7.of(results.results.get(0));
	}

	public Credentials get(String id) {
		return dog.get("/2/credentials/{id}")//
				.routeParam("id", id).go(200).asPojo(Credentials.class);
	}

	//
	// Get All Credentials
	//

	public Credentials.Results getAll() {
		return getAll(false);
	}

	public Credentials.Results getAll(boolean refresh) {
		return getAll(null, null, null, refresh);
	}

	public Credentials.Results getAll(int from, int size) {
		return getAll(null, from, size, false);
	}

	public Credentials.Results getAll(String q) {
		return getAll(q, null, null, false);
	}

	public Credentials.Results getAll(String q, Integer from, Integer size, boolean refresh) {
		return dog.get("/2/credentials").queryParam("q", q)//
				.refresh(refresh).from(from).size(size)//
				.go(200).asPojo(Credentials.Results.class);
	}

	//
	// Login logout
	//

	public SpaceDog login(String password, long lifetime) {
		SpaceRequest request = SpaceRequest.get("/2/login")//
				.backend(dog.backend()).basicAuth(dog.username(), password);

		if (lifetime > 0)
			request.queryParam(LIFETIME_PARAM, lifetime);

		ObjectNode node = request.go(200).asJsonObject();
		dog.accessToken(Json.checkStringNotNullOrEmpty(node, ACCESS_TOKEN_FIELD));
		credentials = Json.toPojo(node.get("credentials"), Credentials.class);
		return dog;
	}

	public SpaceDog logout() {
		if (dog.accessToken() != null) {
			dog.get("/2/logout").go(200).asVoid();
			dog.accessToken(null);
			dog.expiresAt(null);
		}
		return dog;
	}

	//
	// Create credentials method
	//

	public String create(String username, String password, String email, String... roles) {
		return create(new CredentialsCreateRequest()//
				.username(username).password(password).email(email).roles(roles));
	}

	public String create(CredentialsCreateRequest request) {
		return dog.post("/2/credentials").bodyPojo(request).go(201).getString(ID_FIELD);
	}

	//
	// Delete credentials method
	//

	public void delete() {
		delete("me");
	}

	public void delete(String id) {
		dog.delete("/2/credentials/{id}").routeParam("id", id).go(200).asVoid();
	}

	public void deleteByUsername(String username) {
		Optional7<Credentials> optional = dog.credentials().getByUsername(username);
		if (optional.isPresent())
			dog.credentials().delete(optional.get().id());
	}

	public void deleteAllButSuperAdmins() {
		dog.delete("/2/credentials").go(200).asVoid();
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

		SpaceRequest spaceRequest = dog.put("/2/credentials/{id}")//
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
		dog.put("/2/credentials/{id}/roles/{role}")//
				.routeParam("id", id).routeParam("role", role).go(200).asVoid();
	}

	public void unsetRole(String id, String role) {
		dog.delete("/2/credentials/{id}/roles/{role}")//
				.routeParam("id", id).routeParam("role", role).go(200).asVoid();
	}

	public Set<String> getAllRoles(String id) {
		return Sets.newHashSet(//
				dog.get("/2/credentials/{id}/roles")//
						.routeParam("id", id).go(200).asPojo(String[].class));
	}

	public void setAllRoles(String id, String... roles) {
		dog.put("/2/credentials/{id}/roles")//
				.routeParam("id", id).bodyJson(Json.toJsonNode(roles)).go(200);
	}

	public void unsetAllRoles(String id) {
		dog.delete("/2/credentials/{id}/roles")//
				.routeParam("id", id).go(200).asVoid();
	}

	//
	// Password methods
	//

	public String resetPassword(String id) {
		return dog.post("/2/credentials/{id}/_reset_password")//
				.routeParam("id", id).go(200)//
				.getString(PASSWORD_RESET_CODE_FIELD);
	}

	public void setMyPasswordWithCode(String newPassword, String passwordResetCode) {
		setPasswordWithCode(dog.id(), newPassword, passwordResetCode);
	}

	public void setPasswordWithCode(String credentialsId, //
			String newPassword, String passwordResetCode) {
		SpaceRequest.post("/2/credentials/{id}/_set_password")//
				.backend(dog.backend())//
				.routeParam("id", credentialsId)//
				.bodyPojo(new SetPasswordRequest()//
						.withPassword(newPassword)//
						.withPasswordResetCode(passwordResetCode))//
				.go(200)//
				.asVoid();
	}

	public void setMyPassword(String oldPassword, String newPassword) {
		setPassword("me", oldPassword, newPassword);
	}

	public void setPassword(String credentialsId, //
			String requesterPassword, String newPassword) {
		SpaceRequest.post("/2/credentials/{id}/_set_password")//
				.backend(dog.backend())//
				.basicAuth(dog.username(), requesterPassword)//
				.routeParam("id", credentialsId)//
				.bodyPojo(new SetPasswordRequest().withPassword(newPassword))//
				.go(200)//
				.asVoid();
	}

	public void passwordMustChange(String credentialsId) {
		dog.post("/2/credentials/{id}/_password_must_change")//
				.routeParam("id", credentialsId).go(200).asVoid();
	}

	public void sendPasswordResetEmail(String username) {
		sendPasswordResetEmail(username, Json.object());
	}

	public void sendPasswordResetEmail(String username, ObjectNode parameters) {
		parameters.put(USERNAME_FIELD, username);
		dog.post("/2/credentials/_send_password_reset_email")//
				.bodyJson(parameters)//
				.go(200)//
				.asVoid();
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
		StringBuilder builder = new StringBuilder("/2/credentials/") //
				.append(id).append(enable ? "/_enable" : "/_disable");
		dog.post(builder.toString()).go(200).asVoid();
	}

	//
	// Groups
	//

	public Credentials createGroup(String suffix) {
		return dog.post("/2/credentials/me/groups")//
				.bodyJson("suffix", suffix)//
				.go(200).asPojo(Credentials.class);
	}

	public Credentials deleteGroup(String group) {
		return dog.delete("/2/credentials/me/groups/{group}")//
				.routeParam("group", group)//
				.go(200).asPojo(Credentials.class);
	}

	public void shareGroup(String credentialsId, String group) {
		dog.put("/2/credentials/{id}/groups/{group}")//
				.routeParam("id", credentialsId)//
				.routeParam("group", group)//
				.go(200).asVoid();
	}

	public void unshareGroup(String credentialsId, String group) {
		dog.delete("/2/credentials/{id}/groups/{group}")//
				.routeParam("id", credentialsId)//
				.routeParam("group", group)//
				.go(200).asVoid();
	}

	//
	// Credentials settings methods
	//

	public CredentialsSettings settings() {
		return dog.settings().get(CredentialsSettings.class);
	}

	public void settings(CredentialsSettings settings) {
		dog.settings().save(settings);
	}

	public void enableGuestSignUp(boolean enable) {
		CredentialsSettings settings = settings();
		settings.guestSignUpEnabled = enable;
		settings(settings);
	}

}
