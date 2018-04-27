package io.spacedog.client.credentials;

import java.util.Set;

import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.node.BooleanNode;
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
		if (credentials == null || reload) {
			SpaceResponse response = dog.get("/1/credentials/me").go(200);
			credentials = Credentials.parse(response.asJsonObject());
		}
		return credentials;
	}

	CredentialsClient me(Credentials credentials) {
		this.credentials = credentials;
		return this;
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
		return create(new CreateCredentialsRequest()//
				.username(username).password(password).email(email).roles(roles));
	}

	public SpaceDog create(CreateCredentialsRequest request) {
		dog.post("/1/credentials").bodyPojo(request).go(201);
		return SpaceDog.dog(dog.backend()).username(request.username())//
				.password(request.password()).email(request.email());
	}

	public SpaceDog signUp() {
		return signUp(dog.password().get());
	}

	public SpaceDog signUp(String password) {
		SpaceDog.dog(dog.backend()).credentials()//
				.create(dog.username(), password, dog.email().get());
		return dog;
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

		public SpaceResponse go() {
			return go(null);
		}

		public SpaceResponse go(String password) {
			ObjectNode body = Json.object();

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

			return request.go(200);
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

	public void passwordMustChange(String id) {
		dog.put("/1/credentials/{id}/passwordMustChange")//
				.routeParam("id", id).body("true").go(200);
	}

	public void sendMePasswordResetEmail() {
		sendMePasswordResetEmail(Json.object());
	}

	public void sendMePasswordResetEmail(ObjectNode parameters) {
		sendPasswordResetEmail(dog.username(), parameters);
	}

	public void sendPasswordResetEmail(String username) {
		sendPasswordResetEmail(username, Json.object());
	}

	public void sendPasswordResetEmail(String username, ObjectNode parameters) {
		parameters.put(USERNAME_FIELD, username);
		SpaceDog.dog().post("/1/credentials/_send_password_reset_email")//
				.bodyJson(parameters)//
				.go(200);
	}

	//
	// Enable methods
	//

	public void enable(String id, boolean enable) {
		dog.put("/1/credentials/{id}/enabled").routeParam("id", id)//
				.bodyJson(BooleanNode.valueOf(enable)).go(200);
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
