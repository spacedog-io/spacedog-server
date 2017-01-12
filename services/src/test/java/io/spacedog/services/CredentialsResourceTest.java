/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.junit.Assert;
import org.junit.Test;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceClient.Backend;
import io.spacedog.client.SpaceClient.User;
import io.spacedog.client.SpaceEnv;
import io.spacedog.client.SpaceRequest;

public class CredentialsResourceTest extends Assert {

	@Test
	public void deleteSuperAdminCredentials() {

		// prepare
		SpaceClient.prepareTest();
		Backend test = SpaceClient.resetTestBackend();
		Backend test2 = SpaceClient.resetTest2Backend();

		test.adminUser = SpaceClient.login(test.adminUser);
		test2.adminUser = SpaceClient.login(test2.adminUser);
		User superdog = SpaceClient.login("api", //
				SpaceRequest.env().get("spacedog.superdog.username"), //
				SpaceRequest.env().get("spacedog.superdog.password"));

		// forbidden to delete superadmin if last superadmin of backend
		SpaceRequest.delete("/1/credentials/" + test.adminUser.id).adminAuth(test).go(403);
		SpaceRequest.delete("/1/credentials/" + test.adminUser.id).superdogAuth().go(403);

		// superadmin test can create another superadmin (test1)
		SpaceRequest.post("/1/credentials").adminAuth(test)//
				.body("username", "test1", "password", "hi test1", //
						"email", "test1@test.com", "level", "SUPER_ADMIN")//
				.go(201);

		User test1 = SpaceClient.login("test", "test1", "hi test1");

		// superadmin test can delete superadmin test1
		SpaceRequest.delete("/1/credentials/" + test1.id).adminAuth(test).go(200);

		// test1 can no longer login
		SpaceClient.login("test", "test1", "hi test1", 401);

		// superadmin test fails to delete superdog david
		SpaceRequest.delete("/1/credentials/" + superdog.id).adminAuth(test).go(403);

		// superadmin test fails to delete superadmin of another backend
		SpaceRequest.delete("/1/credentials/" + test2.adminUser.id).adminAuth(test).go(403);

		// superadmin test2 fails to delete superadmin of another backend
		SpaceRequest.delete("/1/credentials/" + test.adminUser.id).adminAuth(test2).go(403);
	}

	@Test
	public void superdogCanLoginAndAccessAllBackends() {

		// prepare
		SpaceClient.prepareTest();
		SpaceClient.resetTestBackend();
		SpaceEnv configuration = SpaceRequest.env();
		User superdog = new User("api", //
				configuration.get("spacedog.superdog.username"), configuration.get("spacedog.superdog.password"));

		// superdog logs in with the root backend
		SpaceClient.login(superdog);
		String apiToken = superdog.accessToken;

		// superdog can access anything in any backend
		SpaceRequest.get("/1/backend").backendId("api").bearerAuth(apiToken).go(200);
		SpaceRequest.get("/1/backend").backendId("test").bearerAuth(apiToken).go(200);
		SpaceRequest.get("/1/data").backendId("api").bearerAuth(apiToken).go(200);
		SpaceRequest.get("/1/data").backendId("test").bearerAuth(apiToken).go(200);

		// superdog logs with the "test" backend
		superdog.backendId = "test";
		SpaceClient.login(superdog);
		String testToken = superdog.accessToken;
		assertNotEquals(testToken, apiToken);

		// superdog credentials backendId is not changed
		// by login into "test" backend
		SpaceRequest.get("/1/credentials/" + superdog.id)//
				.superdogAuth().go(200)//
				.assertEquals("api", "backendId");

		// superdog can still access anything in any backend with the old token
		SpaceRequest.get("/1/backend").backendId("api").bearerAuth(apiToken).go(200);
		SpaceRequest.get("/1/backend").backendId("test").bearerAuth(apiToken).go(200);
		SpaceRequest.get("/1/data").backendId("api").bearerAuth(apiToken).go(200);
		SpaceRequest.get("/1/data").backendId("test").bearerAuth(apiToken).go(200);

		// superdog can also access anything in any backend with the new token
		SpaceRequest.get("/1/backend").backendId("api").bearerAuth(testToken).go(200);
		SpaceRequest.get("/1/backend").backendId("test").bearerAuth(testToken).go(200);
		SpaceRequest.get("/1/data").backendId("api").bearerAuth(testToken).go(200);
		SpaceRequest.get("/1/data").backendId("test").bearerAuth(testToken).go(200);

		// superdog logout from his "test" session
		SpaceClient.logout("api", testToken);

		// superdog can not access anything from his "test" token
		SpaceRequest.get("/1/data").backendId("api").bearerAuth(testToken).go(401);
		SpaceRequest.get("/1/backend").backendId("api").bearerAuth(testToken).go(401);

		// superdog can still access anything from his "api" token
		SpaceRequest.get("/1/data").backendId("api").bearerAuth(apiToken).go(200);
		SpaceRequest.get("/1/backend").backendId("api").bearerAuth(apiToken).go(200);

		// superdog logout from his "api" session
		// and can not access anything this token
		SpaceClient.logout("api", apiToken);
		SpaceRequest.get("/1/data").backendId("api").bearerAuth(apiToken).go(401);
		SpaceRequest.get("/1/backend").backendId("api").bearerAuth(apiToken).go(401);
	}
}