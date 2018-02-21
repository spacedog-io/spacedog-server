package io.spacedog.client;

import org.joda.time.DateTime;

import io.spacedog.http.SpaceBackend;
import io.spacedog.http.SpaceEnv;
import io.spacedog.http.SpaceFields;
import io.spacedog.http.SpaceParams;
import io.spacedog.http.SpaceRequest;
import io.spacedog.http.SpaceResponse;
import io.spacedog.utils.Check;
import io.spacedog.utils.Optional7;

public class SpaceDog implements SpaceFields, SpaceParams {

	private SpaceBackend backend;
	private String username;
	private String email;
	private String password;
	private String accessToken;
	private DateTime expiresAt;

	private SpaceDog(SpaceBackend backend) {
		this.backend = backend;
	}

	public SpaceBackend backend() {
		return backend;
	}

	public String backendId() {
		return backend.backendId();
	}

	public String username() {
		return username;
	}

	public SpaceDog username(String username) {
		this.username = username;
		return this;
	}

	public String id() {
		return credentials().me().id();
	}

	public String group() {
		return credentials().me().group();
	}

	public Optional7<String> email() {
		return Optional7.ofNullable(email);
	}

	public SpaceDog email(String email) {
		this.email = email;
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
		return String.format("SpaceDog[%s]", username());
	}

	//
	// Factory methods
	//

	public static SpaceDog dog() {
		return dog(SpaceEnv.env().apiBackend());
	}

	public static SpaceDog dog(String backend) {
		return dog(SpaceBackend.valueOf(backend));
	}

	public static SpaceDog dog(SpaceBackend backend) {
		return new SpaceDog(backend);
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
		return credentials().login(password, lifetime);
	}

	public boolean isTokenStillValid() {
		Check.notNullOrEmpty(accessToken, "access token");
		SpaceResponse response = get("/1/login").go(200, 401);
		return response.status() == 200;
	}

	public SpaceDog logout() {
		return credentials().logout();
	}

	//
	// Basic REST requests
	//

	public SpaceRequest get(String uri) {
		return auth(SpaceRequest.get(uri));
	}

	public SpaceRequest post(String uri) {
		return auth(SpaceRequest.post(uri));
	}

	public SpaceRequest put(String uri) {
		return auth(SpaceRequest.put(uri));
	}

	public SpaceRequest delete(String uri) {
		return auth(SpaceRequest.delete(uri));
	}

	public SpaceRequest options(String uri) {
		return auth(SpaceRequest.options(uri));
	}

	private SpaceRequest auth(SpaceRequest request) {
		request.backend(backend());

		Optional7<String> accessToken = accessToken();
		if (accessToken.isPresent())
			return request.bearerAuth(accessToken.get());

		Optional7<String> password = password();
		if (password.isPresent())
			return request.basicAuth(username(), password.get());

		// if no password nor access token then no auth
		return request;
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

	public SchemaEndpoint schemas() {
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

	EmailEndpoint emailEndpoint;

	public EmailEndpoint emails() {
		if (emailEndpoint == null)
			emailEndpoint = new EmailEndpoint(this);
		return emailEndpoint;
	}

	FileEndpoint fileEndpoint;

	public FileEndpoint files() {
		if (fileEndpoint == null)
			fileEndpoint = new FileEndpoint(this);
		return fileEndpoint;
	}

	LogEndpoint logEndpoint;

	public LogEndpoint logs() {
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
