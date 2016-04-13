package io.spacedog.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mashape.unirest.http.exceptions.UnirestException;

import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;

public class SpaceDogHelper {

	public static class User {
		public String id;
		public String username;
		public String password;
		public String email;
		public String backendId;

		public User(String backendId, String id, String username, String password, String email) {
			this.backendId = backendId;
			this.id = id;
			this.username = username;
			this.password = password;
			this.email = email;
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

	public static void initPushDefaultSchema(Backend backend) throws Exception {
		SpaceRequest.post("/1/schema/installation").adminAuth(backend).go(201);
	}

	public static void initUserDefaultSchema(Backend backend) throws Exception {
		SpaceRequest.post("/1/schema/user").adminAuth(backend).go(201);
	}

	public static void initUserSchema(Backend backend, ObjectNode schema) throws Exception {
		setSchema(schema, backend);
	}

	public static User createUser(Backend account, String username, String password) throws Exception {
		return createUser(account, username, password, "david@spacedog.io");
	}

	public static User createUser(Backend backend, String username, String password, String email) throws Exception {
		String id = SpaceRequest.post("/1/user").backend(backend)
				.body(Json.object("username", username, "password", password, "email", email))//
				.go(201).objectNode().get("id").asText();

		return new User(backend.backendId, id, username, password, email);
	}

	public static void deleteUser(String username, Backend account) throws Exception {
		SpaceRequest.delete("/1/user/" + username).adminAuth(account).go(200, 404);
	}

	public static void deleteSchema(JsonNode schema, Backend backend) throws Exception {
		String schemaName = schema.fieldNames().next();
		SpaceRequest.delete("/1/schema/" + schemaName).adminAuth(backend).go(200, 404);
	}

	public static void setSchema(JsonNode schema, Backend backend) throws Exception {
		String schemaName = schema.fieldNames().next();
		SpaceRequest.post("/1/schema/" + schemaName).adminAuth(backend).body(schema).go(201);
	}

	public static Backend createBackend(Backend backend) throws UnirestException, Exception {
		return createBackend(backend.backendId, backend.username, backend.password, backend.email);
	}

	public static Backend createBackend(String backendId, String username, String password, String email)
			throws Exception, UnirestException {

		ObjectNode body = Json.object("username", username, //
				"password", password, "email", email);

		SpaceRequest.post("/1/backend/" + backendId)//
				.body(body)//
				.go(201);

		return new Backend(backendId, username, password, email);
	}

	public static void deleteTestBackend() throws UnirestException, Exception {
		deleteBackend("test", "test", "hi test");
	}

	public static void deleteBackend(Backend backend) throws UnirestException, Exception {
		deleteBackend(backend.backendId, backend.username, backend.password);
	}

	public static void deleteBackend(String backendId, String username, String password)
			throws Exception, UnirestException {
		// 401 Unauthorized is valid since if this account does not exist
		// delete returns 401 because admin username and password
		// won't match any account
		SpaceRequest.delete("/1/backend")//
				.basicAuth(backendId, username, password).go(200, 401);
	}

	public static Backend resetTestBackend() throws Exception {
		return resetBackend("test", "test", "hi test");
	}

	public static Backend resetBackend(String backendId, String username, String password) throws Exception {
		return resetBackend(backendId, username, password, "hello@spacedog.io");
	}

	public static Backend resetBackend(Backend backend) throws Exception {
		return resetBackend(backend.backendId, backend.username, backend.password, backend.email);
	}

	public static Backend resetBackend(String backendId, String username, String password, String email)
			throws Exception {
		deleteBackend(backendId, username, password);
		return createBackend(backendId, username, password, email);
	}

	public static void prepareTest() throws Exception {
		prepareTest(true);
	}

	public static void prepareTest(boolean forTesting) throws Exception {

		SpaceRequest.setForTestingDefault(forTesting);

		StackTraceElement parentStackTraceElement = Utils.getParentStackTraceElement();
		System.out.println();
		System.out.println(String.format("--- %s", //
				parentStackTraceElement.getClassName()//
						+ '.' + parentStackTraceElement.getMethodName())
				+ "()");
	}

	public static void deleteAll(String type, Backend account) throws Exception {
		SpaceRequest.delete("/1/data/" + type).adminAuth(account).go(200);
	}
}
