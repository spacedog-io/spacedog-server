package io.spacedog.client;

import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.utils.Schema;
import io.spacedog.utils.Settings;
import io.spacedog.utils.SpaceParams;
import io.spacedog.utils.Utils;

public class SpaceClient {

	public static class User {
		public String id;
		public String backendId;
		public String username;
		public String password;
		public String email;
		public String accessToken;
		public DateTime expiresAt;

		public User(String backendId, String id, String username, String password, String email) {
			this.id = id;
			this.backendId = backendId;
			this.username = username;
			this.password = password;
			this.email = email;
		}

		public User(String backendId, String id, String username, String password, String email, //
				String accessToken, DateTime expiresAt) {
			this(backendId, id, username, password, email);
			this.accessToken = accessToken;
			this.expiresAt = expiresAt;
		}
	}

	public static class Backend {

		public String backendId;
		public User adminUser;

		public Backend(String backendId, String username, String password, String email) {
			this.backendId = backendId;
			this.adminUser = new User(backendId, username, username, password, email);
		}
	}

	public static void initPushDefaultSchema(Backend backend) {
		SpaceRequest.post("/1/schema/installation").adminAuth(backend).go(201);
	}

	public static User newCredentials(Backend backend, String username, String password) {
		return newCredentials(backend.backendId, username, password);
	}

	public static User newCredentials(String backendId, String username, String password) {
		return newCredentials(backendId, username, password, "david@spacedog.io");
	}

	public static User newCredentials(Backend backend, String username, String password, String email) {
		return newCredentials(backend.backendId, username, password, email);
	}

	public static User newCredentials(User user) {
		return newCredentials(user.backendId, user.username, user.password, user.email);
	}

	public static User newCredentials(String backendId, String username, String password, String email) {
		String userId = SpaceRequest.post("/1/credentials").backendId(backendId)
				.body("username", username, "password", password, "email", email)//
				.go(201).getFromJson("id").asText();

		ObjectNode node = SpaceRequest.get("/1/login")//
				.basicAuth(backendId, username, password)//
				.go(200).objectNode();

		return new User(backendId, //
				userId, username, password, email, //
				node.get("accessToken").asText(), //
				DateTime.now().plus(node.get("expiresIn").asLong()));
	}

	public static void deleteCredentials(String username, Backend backend) {
		SpaceRequest.delete("/1/credentials/" + username).adminAuth(backend).go(200, 404);
	}

	public static void resetSchema(Schema schema, Backend backend) {
		deleteSchema(schema, backend);
		setSchema(schema, backend);
	}

	public static void deleteSchema(Schema schema, Backend backend) {
		SpaceRequest.delete("/1/schema/" + schema.name()).adminAuth(backend).go(200, 404);
	}

	public static void setSchema(Schema schema, Backend backend) {
		SpaceRequest.put("/1/schema/" + schema.name())//
				.adminAuth(backend).body(schema).go(200, 201);
	}

	public static Schema getSchema(String name, Backend backend) {
		ObjectNode node = SpaceRequest.get("/1/schema/" + name)//
				.adminAuth(backend).go(200).objectNode();
		return new Schema(name, node);
	}

	public static Backend createBackend(Backend backend) {
		return createBackend(backend, false);
	}

	public static Backend createBackend(Backend backend, boolean notification) {
		return createBackend(backend.backendId, backend.adminUser.username, //
				backend.adminUser.password, backend.adminUser.email, notification);
	}

	public static Backend createBackend(String backendId, String username, String password, //
			String email, boolean notification) {

		SpaceRequest.post("/1/backend/" + backendId)//
				.queryParam(SpaceParams.NOTIF, Boolean.toString(notification))//
				.body("username", username, "password", password, "email", email)//
				.go(201);

		return new Backend(backendId, username, password, email);
	}

	public static void deleteTestBackend() {
		deleteBackend("test", "test", "hi test");
	}

	public static void deleteBackend(Backend backend) {
		deleteBackend(backend.backendId, backend.adminUser.username, backend.adminUser.password);
	}

	public static void deleteBackend(String backendId, String username, String password) {
		// 401 Unauthorized is valid since if this backend does not exist
		// delete returns 401 because admin username and password
		// won't match any backend
		SpaceRequest.delete("/1/backend")//
				.basicAuth(backendId, username, password).go(200, 401);
	}

	public static Backend resetTestBackend() {
		return resetBackend("test", "test", "hi test");
	}

	public static Backend resetTest2Backend() {
		return resetBackend("test2", "test2", "hi test2");
	}

	public static Backend resetBackend(String backendId, String username, String password) {
		return resetBackend(backendId, username, password, "hello@spacedog.io");
	}

	public static Backend resetBackend(Backend backend) {
		return resetBackend(backend.backendId, backend.adminUser.username, //
				backend.adminUser.password, backend.adminUser.email);
	}

	public static Backend resetBackend(String backendId, String username, String password, String email) {
		deleteBackend(backendId, username, password);
		return createBackend(backendId, username, password, email, false);
	}

	public static void prepareTest() {
		prepareTestInternal(true);
	}

	public static void prepareTest(boolean forTesting) {
		prepareTestInternal(forTesting);
	}

	private static void prepareTestInternal(boolean forTesting) {

		SpaceRequest.setForTestingDefault(forTesting);
		StackTraceElement grandParentStackTraceElement = Utils.getGrandParentStackTraceElement();

		Utils.info();
		Utils.info("--- %s.%s() ---", //
				grandParentStackTraceElement.getClassName(), //
				grandParentStackTraceElement.getMethodName());
	}

	public static void deleteAll(String type, Backend backend) {
		SpaceRequest.delete("/1/data/" + type).adminAuth(backend).go(200);
	}

	public static void saveSettings(Backend test, Settings settings) {
		SpaceRequest.put("/1/settings/" + settings.id()).adminAuth(test).body(settings).go(200, 201);
	}

	public static <K extends Settings> K loadSettings(Backend backend, Class<K> settingsClass) {
		return SpaceRequest.get("/1/settings/" + Settings.id(settingsClass))//
				.backend(backend).go(200).toObject(settingsClass);
	}
}
