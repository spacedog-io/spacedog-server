package io.spacedog.client.credentials;

import java.io.InputStream;
import java.util.Set;

import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.credentials.Credentials.Results;
import io.spacedog.client.http.OkHttp;
import io.spacedog.client.http.SpaceFields;
import io.spacedog.client.http.SpaceParams;
import io.spacedog.client.http.SpaceRequest;
import io.spacedog.client.http.SpaceResponse;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.Optional7;

public class CredentialsClient implements SpaceParams, SpaceFields {

	private SpaceDog dog;
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
		return findByText(null, null, null);
	}

	public Credentials.Results getAll(int from, int size) {
		return findByText(null, from, size);
	}

	public Credentials.Results findByText(String q) {
		return findByText(q, null, null);
	}

	public Credentials.Results findByText(String q, Integer from, Integer size) {
		return dog.get("/2/credentials").queryParam(Q_PARAM, q)//
				.from(from).size(size).go(200).asPojo(Credentials.Results.class);
	}

	public Credentials.Results findByRole(String role, Integer from, Integer size) {
		return dog.get("/2/credentials").queryParam(ROLE_PARAM, role)//
				.from(from).size(size).go(200).asPojo(Credentials.Results.class);
	}

	//
	// Login logout
	//

	public SpaceDog login(String password, long lifetime) {
		SpaceRequest request = SpaceRequest.post("/2/credentials/_login")//
				.backend(dog.backend()).basicAuth(dog.username(), password);

		if (lifetime > 0)
			request.queryParam(LIFETIME_PARAM, lifetime);

		ObjectNode node = request.go(200).asJsonObject();
		dog.accessToken(Json.checkStringNotNullOrEmpty(node, ACCESS_TOKEN_FIELD));
		credentials = Json.toPojo(node.get("credentials"), Credentials.class);
		return dog;
	}

	public SpaceDog logout() {
		if (dog.accessToken() != null)
			dog.post("/2/credentials/_logout").go(200).asVoid();

		dog.accessToken(null);
		dog.expiresAt(null);
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

	public Credentials updateMyUsername(String username, String password) {
		credentials = updateUsername("me", username, password);
		return credentials;
	}

	public Credentials updateUsername(String credentialsId, String username, String password) {
		return dog.put("/2/credentials/{id}/username")//
				.basicAuth(dog.username(), password)//
				.routeParam("id", credentialsId)//
				.bodyPojo(username)//
				.go(200)//
				.asPojo(Credentials.class);
	}

	public Credentials updateMyEmail(String email, String password) {
		credentials = updateEmail("me", email, password);
		return credentials;
	}

	public Credentials updateEmail(String credentialsId, String email, String password) {
		return dog.put("/2/credentials/{id}/email")//
				.basicAuth(dog.username(), password)//
				.routeParam("id", credentialsId)//
				.bodyPojo(email)//
				.go(200)//
				.asPojo(Credentials.class);
	}

	//
	// Roles method
	//

	public Credentials setRole(String credentialsId, String role) {
		return dog.put("/2/credentials/{id}/roles/{role}")//
				.routeParam("id", credentialsId).routeParam("role", role)//
				.go(200).asPojo(Credentials.class);
	}

	public Credentials unsetRole(String credentialsId, String role) {
		return dog.delete("/2/credentials/{id}/roles/{role}")//
				.routeParam("id", credentialsId).routeParam("role", role)//
				.go(200).asPojo(Credentials.class);
	}

	public Set<String> getAllRoles(String credentialsId) {
		return Sets.newHashSet(//
				dog.get("/2/credentials/{id}/roles")//
						.routeParam("id", credentialsId)//
						.go(200).asPojo(String[].class));
	}

	public Credentials setAllRoles(String credentialsId, String... roles) {
		return dog.put("/2/credentials/{id}/roles")//
				.routeParam("id", credentialsId).bodyJson(Json.toJsonNode(roles))//
				.go(200).asPojo(Credentials.class);
	}

	public Credentials unsetAllRoles(String credentialsId) {
		return dog.delete("/2/credentials/{id}/roles")//
				.routeParam("id", credentialsId)//
				.go(200).asPojo(Credentials.class);
	}

	//
	// Password methods
	//

	public String resetPassword(String credentialsId) {
		return dog.post("/2/credentials/{id}/_reset_password")//
				.routeParam("id", credentialsId).go(200)//
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

	public Credentials enable(String credentialsId) {
		return enable(credentialsId, true);
	}

	public Credentials disable(String credentialsId) {
		return enable(credentialsId, false);
	}

	public Credentials enable(String credentialsId, boolean enable) {
		StringBuilder builder = new StringBuilder("/2/credentials/") //
				.append(credentialsId).append(enable ? "/_enable" : "/_disable");
		return dog.post(builder.toString()).go(200).asPojo(Credentials.class);
	}

	public Credentials enableDisableAfter(String id, DateTime enableAfter, DateTime disableAfter) {
		EnableDisableAfterRequest pojo = new EnableDisableAfterRequest();
		pojo.enableAfter = enableAfter;
		pojo.disableAfter = disableAfter;
		return dog.post("/2/credentials/{id}/_enable_disable_after")//
				.routeParam("id", id).bodyPojo(pojo).go(200).asPojo(Credentials.class);
	}

	//
	// Groups
	//

	public String createGroup(String suffix) {
		return createGroup("me", suffix);
	}

	public String createGroup(String credentialsId, String suffix) {
		return dog.post("/2/credentials/{id}/groups")//
				.routeParam("id", credentialsId)//
				.bodyJson("suffix", suffix)//
				.go(200).getString(GROUP_FIELD);
	}

	public Credentials shareGroup(String credentialsId, String group) {
		return dog.put("/2/credentials/{id}/groups/{group}")//
				.routeParam("id", credentialsId)//
				.routeParam("group", group)//
				.go(200).asPojo(Credentials.class);
	}

	public Credentials removeGroup(String group) {
		return unshareGroup("me", group);
	}

	public Credentials unshareGroup(String credentialsId, String group) {
		return dog.delete("/2/credentials/{id}/groups/{group}")//
				.routeParam("id", credentialsId)//
				.routeParam("group", group)//
				.go(200).asPojo(Credentials.class);
	}

	//
	// Import Export
	//

	public SpaceResponse exportNow() {
		return exportNow(null);
	}

	public SpaceResponse exportNow(String query) {
		return dog.post("/2/credentials/_export")//
				.body(query)//
				.go(200);
	}

	public long importNow(InputStream export, boolean preserveIds) {
		return dog.post("/2/credentials/_import")//
				.withContentType(OkHttp.TEXT_PLAIN)//
				.queryParam(PRESERVE_IDS_PARAM, preserveIds)//
				.body(export)//
				.go(200).get(INDEXED_FIELD).asLong();
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
