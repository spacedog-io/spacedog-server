package io.spacedog.test;

import java.util.Iterator;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.LogEndpoint.LogSearchResults;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.Backends;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Credentials.Level;
import io.spacedog.utils.Json7;
import io.spacedog.utils.Optional7;
import io.spacedog.utils.Passwords;

public class LogResourceTest extends SpaceTest {

	@Test
	public void purgeBackendLogs() throws InterruptedException {

		// prepare
		prepareTest();
		SpaceDog test2 = resetTest2Backend();
		SpaceDog test = resetTestBackend();
		SpaceDog fred = signUp(test, "fred", "hi fred");

		fred.data().getAll().get();
		fred.data().getAll().get();

		// superadmin gets test2 backend total log count to check
		// later that they aren't affected by test backend log purge
		long test2TotalLogs = test2.log().get(0, true).total;

		// superadmin checks everything is in place
		LogSearchResults log = test.log().get(6, true);

		assertEquals("/1/data", log.results.get(0).path);
		assertEquals("/1/data", log.results.get(1).path);
		assertEquals("/1/login", log.results.get(2).path);
		assertEquals("/1/credentials", log.results.get(3).path);
		assertEquals("/1/backend", log.results.get(4).path);
		assertEquals("/1/backend", log.results.get(5).path);

		DateTime before = log.results.get(1).receivedAt;

		// superadmin deletes all logs before GET /data requests
		test.log().delete(before);

		// superadmin checks all test backend logs are deleted but ...
		log = test.log().get(10, true);

		assertEquals(4, log.total);
		assertEquals("DELETE", log.results.get(0).method);
		assertEquals("/1/log", log.results.get(0).path);
		assertEquals("GET", log.results.get(1).method);
		assertEquals("/1/log", log.results.get(1).path);
		assertEquals("GET", log.results.get(2).method);
		assertEquals("/1/data", log.results.get(2).path);
		assertEquals("GET", log.results.get(3).method);
		assertEquals("/1/data", log.results.get(3).path);

		before = log.results.get(1).receivedAt;

		// superdog deletes all logs before GET /log requests
		superdog(test).log().delete(before);

		// superadmin checks all test backend logs are deleted but ...
		log = superdog(test).log().get(10, true);

		assertEquals(4, log.total);
		assertEquals("DELETE", log.results.get(0).method);
		assertEquals("/1/log", log.results.get(0).path);
		assertEquals("GET", log.results.get(1).method);
		assertEquals("/1/log", log.results.get(1).path);
		assertEquals("DELETE", log.results.get(2).method);
		assertEquals("/1/log", log.results.get(2).path);
		assertEquals("GET", log.results.get(3).method);
		assertEquals("/1/log", log.results.get(3).path);

		// count = last time checked + 1, because of the first check.
		// It demonstrates purge of specific backend doesn't affect other
		// backends
		log = test2.log().get(0, true);

		assertEquals(test2TotalLogs + 1, log.total);
	}

	@Test
	public void purgeAllBackendLogs() throws InterruptedException {

		// prepare
		prepareTest();

		// superdog creates purge user in root backend
		superdogDeletesCredentials("api", "purgealltest");
		SpaceDog purgeUser = createTempUser("api", "purgealltest");

		// purge user fails to delete any logs
		// since it doesn't have the 'purgeall' role
		SpaceRequest.delete("/1/log").auth(purgeUser).go(403);

		// superdog assigns 'purgeall' role to purge user
		superdog().put("/1/credentials/" + purgeUser.id() + "/roles/purgeall")//
				.go(200);

		// create test and test2 backends and users
		SpaceDog test = resetTestBackend();
		SpaceDog test2 = resetTest2Backend();
		SpaceDog fred = signUp(test, "fred", "hi fred");
		SpaceDog vince = signUp(test2, "vince", "hi vince");

		// data requests for logs
		SpaceRequest.get("/1/data").auth(fred).go(200);
		SpaceRequest.get("/1/data").auth(vince).go(200);
		SpaceRequest.get("/1/data").auth(fred).go(200);
		SpaceRequest.get("/1/data").auth(vince).go(200);

		// check everything is in place
		String before = superdog().get("/1/log").refresh().go(200)//
				.assertEquals("/1/data", "results.0.path")//
				.assertEquals("test2", "results.0.credentials.backendId")//
				.assertEquals("/1/data", "results.1.path")//
				.assertEquals("test", "results.1.credentials.backendId")//
				.assertEquals("/1/data", "results.2.path")//
				.assertEquals("test2", "results.2.credentials.backendId")//
				.assertEquals("/1/data", "results.3.path")//
				.assertEquals("test", "results.3.credentials.backendId")//
				.assertEquals("/1/login", "results.4.path")//
				.assertEquals("test2", "results.4.credentials.backendId")//
				.assertEquals("/1/credentials", "results.5.path")//
				.assertEquals("test2", "results.5.credentials.backendId")//
				.assertEquals("/1/login", "results.6.path")//
				.assertEquals("test", "results.6.credentials.backendId")//
				.assertEquals("/1/credentials", "results.7.path")//
				.assertEquals("test", "results.7.credentials.backendId")//
				.getString("results.0.receivedAt");

		// purge user fails to get logs
		SpaceRequest.get("/1/log").auth(purgeUser).go(403);

		// purge user deletes all backend logs before the last data request
		SpaceRequest.delete("/1/log").queryParam("before", before).auth(purgeUser).go(200);

		// superdog checks all backend logs are deleted but ...
		before = superdog().get("/1/log").refresh().go(200)//
				.assertEquals(4, "total")//
				.assertEquals("DELETE", "results.0.method")//
				.assertEquals("/1/log", "results.0.path")//
				.assertEquals("GET", "results.1.method")//
				.assertEquals("/1/log", "results.1.path")//
				.assertEquals("GET", "results.2.method")//
				.assertEquals("/1/log", "results.2.path")//
				.assertEquals("GET", "results.3.method")//
				.assertEquals("/1/data", "results.3.path")//
				.getString("results.0.receivedAt");

		// superdog deletes all backend logs before the delete request
		superdog().delete("/1/log").queryParam("before", before).go(200);

		// superdog checks all backend logs are deleted but ...
		superdog().get("/1/log").refresh().go(200)//
				.assertEquals(3, "total")//
				.assertEquals("DELETE", "results.0.method")//
				.assertEquals("/1/log", "results.0.path")//
				.assertEquals("GET", "results.1.method")//
				.assertEquals("/1/log", "results.1.path")//
				.assertEquals("DELETE", "results.2.method")//
				.assertEquals("/1/log", "results.2.path");
	}

	@Test
	public void superdogsCanBrowseAllBackendLogs() {

		prepareTest();

		// create test backends and users
		SpaceDog test = resetTestBackend();
		SpaceDog test2 = resetTest2Backend();

		signUp(test, "vince", "hi vince");
		signUp(test2, "fred", "hi fred");

		// superdog gets all backends logs
		superdog().get("/1/log").refresh().go(200)//
				.assertEquals("/1/login", "results.0.path")//
				.assertEquals("test2", "results.0.credentials.backendId")//
				.assertEquals("/1/credentials", "results.1.path")//
				.assertEquals("test2", "results.1.credentials.backendId")//
				.assertEquals("/1/login", "results.2.path")//
				.assertEquals("test", "results.2.credentials.backendId")//
				.assertEquals("/1/credentials", "results.3.path")//
				.assertEquals("test", "results.3.credentials.backendId")//
				.assertEquals("/1/backend", "results.4.path")//
				.assertEquals("test2", "results.4.credentials.backendId")//
				.assertEquals("/1/backend", "results.5.path")//
				.assertEquals("test2", "results.5.credentials.backendId")//
				.assertEquals("/1/backend", "results.6.path")//
				.assertEquals("test", "results.6.credentials.backendId")//
				.assertEquals("/1/backend", "results.7.path")//
				.assertEquals("test", "results.7.credentials.backendId");

		// after backend deletion, logs are still accessible to superdogs
		test.admin().deleteBackend(test.backendId());
		test2.admin().deleteBackend(test2.backendId());

		superdog().get("/1/log").refresh().go(200)//
				.assertEquals("DELETE", "results.0.method")//
				.assertEquals("/1/backend", "results.0.path")//
				.assertEquals("test2", "results.0.credentials.backendId")//
				.assertEquals("DELETE", "results.1.method")//
				.assertEquals("/1/backend", "results.1.path")//
				.assertEquals("test", "results.1.credentials.backendId")//
				.assertEquals("GET", "results.2.method")//
				.assertEquals("/1/log", "results.2.path")//
				.assertEquals("api", "results.2.credentials.backendId")//
				.assertEquals("GET", "results.3.method")//
				.assertEquals("/1/login", "results.3.path")//
				.assertEquals("test2", "results.3.credentials.backendId")//
				.assertEquals("POST", "results.4.method")//
				.assertEquals("/1/credentials", "results.4.path")//
				.assertEquals("test2", "results.4.credentials.backendId")//
				.assertEquals("GET", "results.5.method")//
				.assertEquals("/1/login", "results.5.path")//
				.assertEquals("test", "results.5.credentials.backendId")//
				.assertEquals("POST", "results.6.method")//
				.assertEquals("/1/credentials", "results.6.path")//
				.assertEquals("test", "results.6.credentials.backendId");

	}

	@Test
	public void onlySuperdogsCanBrowseAllBackends() {

		// prepare
		prepareTest();
		SpaceDog superdog = superdog();
		String nathPassword = Passwords.random();
		SpaceDog nath = SpaceDog.backend("api").username("nath")//
				.email("nath@dog.com").password(nathPassword);

		// superdog deletes nath if she exists for fresh start
		Optional7<Credentials> credentials = superdog.credentials()//
				.getByUsername("nath");
		if (credentials.isPresent())
			superdog.credentials().delete(credentials.get().id());

		// superdog creates superadmin named nath in root 'api' backend
		superdog.credentials().create(nath.username(), //
				nathPassword, nath.email().get(), Level.SUPER_ADMIN);

		// anonymous gets data from test backend
		SpaceDog.backend("test").data().getAll().get();

		// superdog gets all backend logs and is able to review
		// previous test and root 'api' backend requests
		superdog.get("/1/log").refresh().size(3).go(200)//
				.assertEquals("/1/data", "results.0.path")//
				.assertEquals("/1/credentials", "results.1.path");

		// but nath only gets logs from her root 'api' backend
		// since she's only superadmin and not superdog
		nath.get("/1/log").refresh().size(2).go(200)//
				.assertEquals("/1/log", "results.0.path")//
				.assertEquals("/1/credentials", "results.1.path");

		// nath deletes herself to leave root backend clean
		nath.credentials().delete();
	}

	@Test
	public void searchInLogs() {

		// prepare
		prepareTest();

		// creates test backend and user
		SpaceDog test = resetTestBackend();
		SpaceRequest.get("/1/data").backend(test).go(200);
		SpaceRequest.get("/1/data/user").backend(test).go(403);
		SpaceDog vince = signUp(test, "vince", "hi vince");
		SpaceRequest.get("/1/credentials/" + vince.id()).auth(vince).go(200);

		// superdog search for test backend logs with status 400 and higher
		superdog(test).post("/1/log/search").refresh().size(1)//
				.bodyJson("range", Json7.object("status", Json7.object("gte", "400")))//
				.go(200)//
				.assertEquals("/1/data/user", "results.0.path")//
				.assertEquals(403, "results.0.status");

		// superdog search for test backend logs
		// with credentials level equal to SUPER_ADMIN and lower
		superdog(test).post("/1/log/search").size(7)//
				.bodyJson("terms", Json7.object("credentials.type", Json7.array("SUPER_ADMIN", "USER", "KEY")))//
				.go(200)//
				.assertEquals("/1/credentials/" + vince.id(), "results.0.path")//
				.assertEquals("/1/login", "results.1.path")//
				.assertEquals("/1/credentials", "results.2.path")//
				.assertEquals("/1/data/user", "results.3.path")//
				.assertEquals("/1/data", "results.4.path")//
				.assertEquals("/1/backend", "results.5.path");

		// superdog search for test backend log to only get USER and lower logs
		superdog(test).post("/1/log/search").size(7)//
				.bodyJson("terms", Json7.object("credentials.type", Json7.array("USER", "KEY")))//
				.go(200)//
				.assertEquals("/1/credentials/" + vince.id(), "results.0.path")//
				.assertEquals("/1/login", "results.1.path")//
				.assertEquals("/1/credentials", "results.2.path")//
				.assertEquals("/1/data/user", "results.3.path")//
				.assertEquals("/1/data", "results.4.path");

		// superdog search for test backend log to only get KEY logs
		superdog(test).post("/1/log/search").size(3)//
				.bodyJson("term", Json7.object("credentials.type", "KEY"))//
				.go(200)//
				.assertEquals("/1/credentials", "results.0.path")//
				.assertEquals("/1/data/user", "results.1.path")//
				.assertEquals("/1/data", "results.2.path");

		// superdog gets all test backend logs
		superdog(test).post("/1/log/search").refresh().size(15)//
				.bodyJson("match_all", Json7.object())//
				.go(200)//
				.assertEquals("/1/log/search", "results.0.path")//
				.assertEquals("/1/log/search", "results.1.path")//
				.assertEquals("/1/log/search", "results.2.path")//
				.assertEquals("/1/log/search", "results.3.path")//
				.assertEquals("/1/credentials/" + vince.id(), "results.4.path")//
				.assertEquals("/1/login", "results.5.path")//
				.assertEquals("/1/credentials", "results.6.path")//
				.assertEquals("/1/data/user", "results.7.path")//
				.assertEquals("/1/data", "results.8.path")//
				.assertEquals("/1/backend", "results.9.path")//
				.assertEquals("/1/backend", "results.10.path");
	}

	@Test
	public void pingRequestAreNotLogged() {

		prepareTest();

		// load balancer pings a SpaceDog instance

		SpaceRequest.get("").go(200);

		// this ping should not be present in logs

		JsonNode results = superdog().get("/1/log")//
				.size(5).go(200).get("results");

		Iterator<JsonNode> elements = results.elements();
		while (elements.hasNext()) {
			JsonNode element = elements.next();
			if (element.get("path").asText().equals("/")
					&& element.get("credentials").get("backendId").asText().equals(Backends.rootApi()))
				Assert.fail();
		}
	}

	@Test
	public void test() {

		prepareTest();

		// since from + size invalid since bigger than authorized window
		// request should return http error 400
		superdog().post("/1/log/search")//
				.bodyJson(Json7.object()).size(11000).go(400);
	}
}
