package io.spacedog.client;

import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mashape.unirest.http.exceptions.UnirestException;

import io.spacedog.utils.Schema;
import io.spacedog.utils.Utils;

public class SpaceClient {

	public static class User {
		public String backendId;
		public String id;
		public String username;
		public String password;
		public String email;
		public String accessToken;
		public DateTime expiresAt;

		public User(String backendId, String id, String username, String password, String email) {
			this.backendId = backendId;
			this.id = id;
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
		public String username;
		public String password;
		public String email;

		public Backend(String backendId, String username, String password, String email) {
			this.backendId = backendId;
			this.username = username;
			this.password = password;
			this.email = email;
		}
	}

	public static void initPushDefaultSchema(Backend backend) {
		SpaceRequest.post("/1/schema/installation").adminAuth(backend).go(201);
	}

	public static User newCredentials(Backend backend, String username, String password) {
		return newCredentials(backend, username, password, "david@spacedog.io");
	}

	public static User newCredentials(Backend backend, String username, String password, String email) {
		return newCredentials(backend.backendId, username, password, email);
	}

	public static User newCredentials(User user) {
		return newCredentials(user.backendId, user.username, user.password, user.email);
	}

	public static User newCredentials(String backendId, String username, String password, String email) {
		ObjectNode node = SpaceRequest.post("/1/credentials").backendId(backendId)
				.body("username", username, "password", password, "email", email)//
				.go(201).objectNode();

		return new User(backendId, //
				node.get("id").asText(), //
				username, password, email, //
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

	public static Backend createBackend(Backend backend) throws UnirestException, Exception {
		return createBackend(backend.backendId, backend.username, backend.password, backend.email);
	}

	public static Backend createBackend(String backendId, String username, String password, String email) {

		SpaceRequest.post("/1/backend/" + backendId)//
				.body("username", username, "password", password, "email", email)//
				.go(201);

		return new Backend(backendId, username, password, email);
	}

	public static void deleteTestBackend() {
		deleteBackend("test", "test", "hi test");
	}

	public static void deleteBackend(Backend backend) {
		deleteBackend(backend.backendId, backend.username, backend.password);
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
		return resetBackend(backend.backendId, backend.username, backend.password, backend.email);
	}

	public static Backend resetBackend(String backendId, String username, String password, String email) {
		deleteBackend(backendId, username, password);
		return createBackend(backendId, username, password, email);
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
}
