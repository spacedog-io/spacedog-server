package io.spacedog.sdk;

import java.util.Optional;

import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Credentials.Level;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.Passwords;
import io.spacedog.utils.SpaceFields;
import io.spacedog.utils.SpaceParams;

public class SpaceDog implements SpaceFields, SpaceParams {

	Credentials credentials;
	String password;
	String accessToken;
	DateTime expiresAt;

	private SpaceDog(Credentials credentials) {
		this.credentials = credentials;
	}

	public String backendId() {
		return credentials.backendId();
	}

	public String username() {
		return credentials.name();
	}

	public SpaceDog username(String username) {
		this.credentials.name(username);
		return this;
	}

	public String id() {
		return credentials.id();
	}

	public SpaceDog id(String id) {
		this.credentials.id(id);
		return this;
	}

	public Optional<String> email() {
		return credentials.email();
	}

	public SpaceDog email(String email) {
		this.credentials.email(email);
		return this;
	}

	public Level level() {
		return this.credentials.level();
	}

	public SpaceDog level(Level level) {
		this.credentials.level(level);
		return this;
	}

	public Optional<String> accessToken() {
		return Optional.ofNullable(accessToken);
	}

	public SpaceDog accessToken(String accessToken) {
		this.accessToken = accessToken;
		return this;
	}

	public DateTime expiresAt() {
		return this.expiresAt;
	}

	public SpaceDog expiresAt(DateTime plus) {
		this.expiresAt = plus;
		return this;
	}

	public Optional<String> password() {
		return Optional.ofNullable(password);
	}

	public SpaceDog password(String password) {
		this.password = password;
		return this;
	}

	public static SpaceDog backend(String backendId) {
		return new SpaceDog(new Credentials(backendId));
	}

	public static SpaceDog backend(SpaceDog dog) {
		return backend(dog.backendId());
	}

	public static SpaceDog fromCredentials(Credentials credentials) {
		return new SpaceDog(credentials);
	}

	//
	// login/logout
	//

	public SpaceDog login() {
		return login(password().get());
	}

	public SpaceDog login(long lifetime) {
		return login(password().get(), lifetime);
	}

	public SpaceDog login(String password) {
		return login(password, 0);
	}

	public SpaceDog login(String password, long lifetime) {

		SpaceRequest request = SpaceRequest.get("/1/login")//
				.basicAuth(backendId(), username(), password);

		if (lifetime > 0)
			request.queryParam(PARAM_LIFETIME, Long.toString(lifetime));

		ObjectNode node = request.go(200).objectNode();

		this.accessToken = Json.checkStringNotNullOrEmpty(node, FIELD_ACCESS_TOKEN);
		this.credentials.id(Json.checkStringNotNullOrEmpty(node, "credentials.id"));
		return this;
	}

	public SpaceDog logout() {
		SpaceRequest.get("/1/logout").bearerAuth(backendId(), accessToken).go(200);
		this.accessToken = null;
		this.expiresAt = null;
		return this;
	}

	//
	// sign up
	//

	public SpaceDog signUp() {
		return signUp(password()//
				.orElseThrow(() -> Exceptions.illegalArgument("no password set")));
	}

	public SpaceDog signUp(String password) {
		return signUp(username(), password, email().orElse("platform@spacedog.io"));

	}

	public SpaceDog signUp(String username, String password, String email) {
		this.credentials = SpaceDog.backend(backendId())//
				.credentials().create(username, password, email, Level.USER);
		return login(password);
	}

	//
	// backend
	//

	public SpaceDog signUpBackend() {
		return signUpBackend(password().orElse(Passwords.random()));
	}

	public SpaceDog signUpBackend(String password) {
		return signUpBackend(password, email().orElse("platform@spacedog.io"));
	}

	public SpaceDog signUpBackend(String password, String email) {
		SpaceDog.backend(backendId()).backend().create(username(), password, email, false);
		return this;
	}

	//
	// Basic REST requests
	//

	public SpaceRequest get(String uri) {
		return SpaceRequest.get(uri).auth(this);
	}

	public SpaceRequest post(String uri) {
		return SpaceRequest.post(uri).auth(this);
	}

	public SpaceRequest put(String uri) {
		return SpaceRequest.put(uri).auth(this);
	}

	public SpaceRequest delete(String uri) {
		return SpaceRequest.delete(uri).auth(this);
	}

	//
	// resources
	//

	SpaceData dataEndpoint;

	public SpaceData dataEndpoint() {
		if (dataEndpoint == null)
			dataEndpoint = new SpaceData(this);

		return dataEndpoint;
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

	SpacePush push;

	public SpacePush push() {
		if (push == null)
			push = new SpacePush(this);
		return push;
	}

	SpaceCredentials credentialsEndpoint;

	public SpaceCredentials credentials() {
		if (credentialsEndpoint == null)
			credentialsEndpoint = new SpaceCredentials(this);
		return credentialsEndpoint;
	}

	SpaceSchema schema;

	public SpaceSchema schema() {
		if (schema == null)
			schema = new SpaceSchema(this);
		return schema;
	}

	SpaceBackend backend;

	public SpaceBackend backend() {
		if (backend == null)
			backend = new SpaceBackend(this);
		return backend;
	}

	SpaceMail mailEndpoint;

	public SpaceMail mailEndpoint() {
		if (mailEndpoint == null)
			mailEndpoint = new SpaceMail(this);
		return mailEndpoint;
	}

}
