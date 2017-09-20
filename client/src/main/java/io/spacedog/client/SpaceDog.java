package io.spacedog.client;

import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.http.SpaceBackend;
import io.spacedog.http.SpaceRequest;
import io.spacedog.http.SpaceResponse;
import io.spacedog.utils.Check;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Json;
import io.spacedog.utils.Optional7;
import io.spacedog.utils.SpaceFields;
import io.spacedog.utils.SpaceParams;

public class SpaceDog implements SpaceFields, SpaceParams {

	SpaceBackend backend;
	Credentials credentials;
	String password;
	String accessToken;
	DateTime expiresAt;

	private SpaceDog(SpaceBackend backend) {
		this.backend = backend;
		this.credentials = new Credentials();
	}

	public SpaceBackend backend() {
		return backend;
	}

	public String backendId() {
		return backend.backendId();
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

	public Optional7<String> email() {
		return credentials.email();
	}

	public SpaceDog email(String email) {
		this.credentials.email(email);
		return this;
	}

	public Optional7<String> accessToken() {
		return Optional7.ofNullable(accessToken);
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

	public Optional7<String> password() {
		return Optional7.ofNullable(password);
	}

	public SpaceDog password(String password) {
		this.password = password;
		return this;
	}

	@Override
	public String toString() {
		return credentials.toString();
	}

	//
	// Factory methods
	//

	public static SpaceDog defaultBackend() {
		return new SpaceDog(SpaceRequest.env().target());
	}

	public static SpaceDog backendId(String backendId) {
		return new SpaceDog(SpaceRequest.env().target().instanciate(backendId));
	}

	public static SpaceDog backend(SpaceDog dog) {
		return new SpaceDog(dog.backend);
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

		SpaceRequest request = SpaceRequest.get("/1/login").backend(backendId()).basicAuth(username(), password);

		if (lifetime > 0)
			request.queryParam(LIFETIME_PARAM, Long.toString(lifetime));

		ObjectNode node = request.go(200).asJsonObject();

		this.accessToken = Json.checkStringNotNullOrEmpty(node, ACCESS_TOKEN_FIELD);
		this.credentials.id(Json.checkStringNotNullOrEmpty(node, "credentials.id"));
		return this;
	}

	public boolean isTokenStillValid() {
		Check.notNullOrEmpty(accessToken, "access token");

		SpaceResponse response = SpaceRequest.get("/1/login")//
				.backend(backendId()).bearerAuth(accessToken)//
				.go(200, 401);

		return response.status() == 200;
	}

	public SpaceDog logout() {
		SpaceRequest.get("/1/logout").backend(this).bearerAuth(accessToken).go(200);
		this.accessToken = null;
		this.expiresAt = null;
		return this;
	}

	//
	// sign up
	//

	public SpaceDog signUp() {
		return signUp(password().get());
	}

	public SpaceDog signUp(String password) {
		String id = SpaceDog.backendId(backendId())//
				.credentials().create(username(), password, email().get());
		this.credentials.id(id);
		return login(password);
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

	DataEndpoint dataEndpoint;

	public DataEndpoint data() {
		if (dataEndpoint == null)
			dataEndpoint = new DataEndpoint(this);

		return dataEndpoint;
	}

	SettingsEndpoint settingsEndpoint;

	public SettingsEndpoint settings() {
		if (settingsEndpoint == null)
			settingsEndpoint = new SettingsEndpoint(this);

		return settingsEndpoint;
	}

	StripeEndpoint stripeEndpoint;

	public StripeEndpoint stripe() {
		if (stripeEndpoint == null)
			stripeEndpoint = new StripeEndpoint(this);

		return stripeEndpoint;
	}

	PushEndpoint pushEndpoint;

	public PushEndpoint push() {
		if (pushEndpoint == null)
			pushEndpoint = new PushEndpoint(this);
		return pushEndpoint;
	}

	CredentialsEndpoint credentialsEndpoint;

	public CredentialsEndpoint credentials() {
		if (credentialsEndpoint == null)
			credentialsEndpoint = new CredentialsEndpoint(this);
		return credentialsEndpoint;
	}

	SchemaEndpoint schemaEndpoint;

	public SchemaEndpoint schema() {
		if (schemaEndpoint == null)
			schemaEndpoint = new SchemaEndpoint(this);
		return schemaEndpoint;
	}

	AdminEndpoint adminEndpoint;

	public AdminEndpoint admin() {
		if (adminEndpoint == null)
			adminEndpoint = new AdminEndpoint(this);
		return adminEndpoint;
	}

	MailEndpoint mailEndpoint;

	public MailEndpoint mail() {
		if (mailEndpoint == null)
			mailEndpoint = new MailEndpoint(this);
		return mailEndpoint;
	}

	FileEndpoint fileEndpoint;

	public FileEndpoint file() {
		if (fileEndpoint == null)
			fileEndpoint = new FileEndpoint(this);
		return fileEndpoint;
	}

	LogEndpoint logEndpoint;

	public LogEndpoint log() {
		if (logEndpoint == null)
			logEndpoint = new LogEndpoint(this);
		return logEndpoint;
	}

	SmsEndpoint smsEndpoint;

	public SmsEndpoint sms() {
		if (smsEndpoint == null)
			smsEndpoint = new SmsEndpoint(this);
		return smsEndpoint;
	}

}