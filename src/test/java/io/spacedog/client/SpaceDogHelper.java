package io.spacedog.client;

import java.util.Optional;

import org.junit.Assert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import io.spacedog.services.AdminResource;
import io.spacedog.services.Json;
import io.spacedog.services.Utils;

public class SpaceDogHelper {

	public static class User {
		public String id;
		public String username;
		public String password;
		public String email;

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

	public static User createUser(Account account, String username, String password, String email) throws Exception {
		return createUser(account.backendKey, username, password, email);
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
		deleteSchema(schema, username, password);
		setSchema(schema, username, password);
	}

	public static void deleteSchema(JsonNode schema, Account account) throws Exception {
		deleteSchema(schema, account.username, account.password);
	}

	public static void deleteSchema(JsonNode schema, String username, String password) throws Exception {
		String schemaName = schema.fieldNames().next();
		SpaceRequest.delete("/v1/schema/{name}").routeParam("name", schemaName).basicAuth(username, password).go(200,
				404);
	}

	public static void setSchema(JsonNode schema, Account account) throws Exception {
		setSchema(schema, account.username, account.password);
	}

	public static void setSchema(JsonNode schema, String username, String password) throws Exception {
		String schemaName = schema.fieldNames().next();
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

		SpaceDogHelper.refresh(AdminResource.ADMIN_INDEX);
		SpaceDogHelper.refresh(backendId);

		return new Account(backendId, username, password, email, backendKey);
	}

	public static Optional<Account> getAccount(String backendId, Account account) throws Exception {
		return getAccount(backendId, account.username, account.password);
	}

	public static Optional<Account> getAccount(String backendId, String username, String password) throws Exception {

		SpaceResponse response = SpaceRequest.get("/v1/admin/account/{backendId}").routeParam("backendId", backendId)
				.basicAuth(username, password).go(200, 401);

		if (response.httpResponse().getStatus() == 200) {
			ObjectNode account = response.objectNode();

			String backendKey = backendId + ':' + account.get("backendKey").get("name").asText() + ':'
					+ account.get("backendKey").get("secret").asText();

			return Optional.of(new Account(backendId, username, password, account.get("email").asText(), backendKey));
		}
		return Optional.empty();
	}

	public static Account getOrCreateTestAccount() throws Exception {
		return getOrCreateAccount("test", "test", "hi test", "david@spacedog.io");
	}

	public static Account getOrCreateAccount(String backendId, String username, String password, String email)
			throws Exception {
		Optional<Account> opt = getAccount(backendId, username, password);
		if (opt.isPresent())
			return opt.get();
		return createAccount(backendId, username, password, email);
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

		SpaceDogHelper.refresh(AdminResource.ADMIN_INDEX);
		SpaceDogHelper.refresh(backendId);
	}

	public static Account resetAccount(String backendId, String username, String password, String email)
			throws Exception {
		deleteAccount(backendId, username, password);
		return createAccount(backendId, username, password, email);
	}

	public static Account resetTestAccount() throws Exception {
		StackTraceElement parentStackTraceElement = Utils.getParentStackTraceElement();
		printTestName(parentStackTraceElement.getClassName() + '.' + parentStackTraceElement.getMethodName());
		return resetAccount("test", "test", "hi test", "david@spacedog.io");
	}

	public static void deleteAll(String type, Account account) throws Exception {
		SpaceRequest.delete("/v1/data/{type}").routeParam("type", type).basicAuth(account).go(200);
	}

	public static void printTestName(String testName) {
		System.out.println();
		System.out.println(String.format("==============%s==============", testName));
	}

	public static void refresh(Account account) throws UnirestException {
		SpaceDogHelper.refresh(account.backendId);
	}

	public static void refresh(String index) throws UnirestException {
		String baseUrl = SpaceRequest.backendDomain.endsWith(":8080") ? SpaceRequest.backendDomain.replace(":8080", ":9200")
				: SpaceRequest.backendDomain + ":9200";
	
		if (baseUrl.startsWith("https"))
			baseUrl = baseUrl.replace("https", "http");
	
		System.out.println();
		System.out.println(String.format("Refresh index [%s] => %s", index,
				Unirest.post(baseUrl + "/{index}/_refresh").routeParam("index", index).asString().getStatusText()));
	}

}
