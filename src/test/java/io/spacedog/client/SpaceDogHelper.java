package io.spacedog.client;

import org.junit.Assert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.mashape.unirest.http.exceptions.UnirestException;

import io.spacedog.services.AdminResource;
import io.spacedog.services.Json;

public class SpaceDogHelper {

	public static class User {
		String id;
		public String username;
		String password;
		String email;

		public User(String id, String username, String password, String email) {
			this.id = id;
			this.username = username;
			this.password = password;
			this.email = email;
		}
	}

	public static class Account {
		public String backendId;
		public String backendKey;
		public String username;
		public String password;
		public String email;

		public Account(String backendId, String username, String password, String email, String backendKey) {
			this.backendId = backendId;
			this.backendKey = backendKey;
			this.username = username;
			this.password = password;
			this.email = email;
		}
	}

	public static User createUser(String backendKey, String username, String password, String email) throws Exception {

		String id = SpaceRequest.post("/v1/user/").backendKey(backendKey)
				.body(Json.startObject().put("username", username).put("password", password).put("email", email))
				.go(201).objectNode().get("id").asText();

		return new User(id, username, password, email);
	}

	public static void deleteUser(String string, Account account) throws Exception {
		deleteUser(string, account.username, account.password);
	}

	public static void deleteUser(String username, String adminUsername, String password) throws Exception {
		SpaceRequest.delete("/v1/user/{username}").routeParam("username", username).basicAuth(adminUsername, password)
				.go(200, 404);
	}

	public static void resetSchema(JsonNode schema, Account account) throws Exception {
		resetSchema(schema, account.username, account.password);
	}

	public static void resetSchema(JsonNode schema, String username, String password) throws Exception {

		String schemaName = schema.elements().next().asText();

		SpaceRequest.delete("/v1/schema/{name}").routeParam("name", schemaName).basicAuth(username, password).go(200,
				404);

		SpaceRequest.post("/v1/schema/{name}").routeParam("name", schemaName).basicAuth(username, password).body(schema)
				.go(201);
	}

	public static Account createAccount(String backendId, String username, String password, String email)
			throws Exception, UnirestException {
		String backendKey = SpaceRequest.post("/v1/admin/account/")
				.body(Json.startObject().put("backendId", backendId).put("username", username).put("password", password)
						.put("email", email))
				.go(201).httpResponse().getHeaders().get(AdminResource.BACKEND_KEY_HEADER).get(0);

		Assert.assertFalse(Strings.isNullOrEmpty(backendKey));

		SpaceRequest.refresh(AdminResource.ADMIN_INDEX);
		SpaceRequest.refresh(backendId);

		return new Account(backendId, username, password, email, backendKey);
	}

	public static Account getAccount(String backendId, Account account) throws Exception {
		return getAccount(backendId, account.username, account.password);
	}

	public static Account getAccount(String backendId, String username, String password) throws Exception {

		ObjectNode account = SpaceRequest.get("/v1/admin/account/{backendId}").routeParam("backendId", backendId)
				.basicAuth(username, password).go(200).objectNode();

		String backendKey = backendId + ':' + account.get("backendKey").get("name").asText() + ':'
				+ account.get("backendKey").get("secret").asText();

		return new Account(backendId, username, password, account.get("email").asText(), backendKey);
	}

	public static void deleteAccount(String backendId, Account account) throws Exception, UnirestException {
		deleteAccount(backendId, account.username, account.password);
	}

	public static void deleteAccount(String backendId, String username, String password)
			throws Exception, UnirestException {
		// 401 Unauthorized is valid since if this account does not exist
		// delete returns 401 because admin username and password
		// won't match any account

		SpaceRequest.delete("/v1/admin/account/{backendId}").routeParam("backendId", backendId)
				.basicAuth(username, password).go(200, 401);

		SpaceRequest.refresh(AdminResource.ADMIN_INDEX);
		SpaceRequest.refresh(backendId);
	}

	public static Account resetAccount(String backendId, String username, String password, String email)
			throws Exception {
		deleteAccount(backendId, username, password);
		return createAccount(backendId, username, password, email);
	}

	public static Account resetTestAccount() throws Exception {
		return resetAccount("test", "test", "hi test", "david@spacedog.io");
	}

}
