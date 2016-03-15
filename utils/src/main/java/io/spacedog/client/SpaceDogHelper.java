package io.spacedog.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mashape.unirest.http.exceptions.UnirestException;

import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
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

	public static User createUser(Backend account, String username, String password) throws Exception {
		return createUser(account, username, password, "david@spacedog.io");
	}

	public static User createUser(Backend backend, String username, String password, String email) throws Exception {
		String id = SpaceRequest.post("/1/user").backend(backend)
				.body(Json.objectBuilder().put("username", username).put("password", password).put("email", email))
				.go(201).objectNode().get("id").asText();

		return new User(backend.backendId, id, username, password, email);
	}

	public static void deleteUser(String username, Backend account) throws Exception {
		SpaceRequest.delete("/1/user/" + username).basicAuth(account).go(200, 404);
	}

	public static void deleteSchema(JsonNode schema, Backend backend) throws Exception {
		String schemaName = schema.fieldNames().next();
		SpaceRequest.delete("/1/schema/" + schemaName).basicAuth(backend).go(200, 404);
	}

	public static void setSchema(JsonNode schema, Backend backend) throws Exception {
		String schemaName = schema.fieldNames().next();
		SpaceRequest.post("/1/schema/" + schemaName).basicAuth(backend).body(schema).go(201);
	}

	public static Backend createBackend(Backend backend) throws UnirestException, Exception {
		return createBackend(backend, true);
	}

	public static Backend createBackend(Backend backend, boolean test) throws UnirestException, Exception {
		return createBackend(backend.backendId, backend.username, backend.password, backend.email, test);
	}

	public static Backend createBackend(String backendId, String username, String password, String email,
			boolean forTesting) throws Exception, UnirestException {

		JsonBuilder<ObjectNode> body = Json.objectBuilder()//
				.put("username", username)//
				.put("password", password)//
				.put("email", email);

		SpaceRequest.post("/1/backend/" + backendId)//
				.forTesting(forTesting)//
				.body(body)//
				.go(201);

		return new Backend(backendId, username, password, email);
	}

	public static void deleteTestBackend() throws UnirestException, Exception {
		deleteBackend("test", "test", "hi test");
	}

	public static void deleteBackend(Backend account) throws UnirestException, Exception {
		deleteBackend(account.backendId, account.username, account.password);
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
		return resetBackend(backendId, username, password, "hello@spacedog.io", true);
	}

	public static Backend resetBackend(Backend backend, boolean forTesting) throws Exception {
		return resetBackend(backend.backendId, backend.username, backend.password, backend.email, forTesting);
	}

	public static Backend resetBackend(String backendId, String username, String password, String email,
			boolean forTesting) throws Exception {
		deleteBackend(backendId, username, password);
		return createBackend(backendId, username, password, email, forTesting);
	}

	public static void prepareTest() throws Exception {

		SpaceRequest.setForTestingDefault(true);

		StackTraceElement parentStackTraceElement = Utils.getParentStackTraceElement();
		System.out.println(String.format("--- %s", //
				parentStackTraceElement.getClassName()//
						+ '.' + parentStackTraceElement.getMethodName())
				+ "()");
	}

	public static void deleteAll(String type, Backend account) throws Exception {
		SpaceRequest.delete("/1/data/" + type).basicAuth(account).go(200);
	}
}
