package io.spacedog.test;

import java.util.List;

import org.joda.time.DateTime;
import org.junit.Test;

import io.spacedog.client.LogEndpoint.LogItem;
import io.spacedog.client.LogEndpoint.LogSearchResults;
import io.spacedog.client.SpaceDog;
import io.spacedog.client.elastic.ESQueryBuilders;
import io.spacedog.client.elastic.ESSearchSourceBuilder;
import io.spacedog.client.elastic.ESSortBuilders;
import io.spacedog.client.elastic.ESSortOrder;
import io.spacedog.http.SpaceEnv;
import io.spacedog.http.SpaceHeaders;
import io.spacedog.http.SpaceRequest;
import io.spacedog.model.JsonDataObject;
import io.spacedog.model.Permission;
import io.spacedog.model.Roles;
import io.spacedog.model.Schema;
import io.spacedog.utils.Json;

public class LogServiceTest extends SpaceTest {

	@Test
	public void purgeBackendLogs() throws InterruptedException {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearRootBackend();
		SpaceDog fred = createTempDog(superadmin, "fred").login();

		fred.data().getAllRequest().go();
		fred.data().getAllRequest().go();

		// superadmin checks everything is in place
		LogSearchResults log = superadmin.logs().get(10, true);

		assertEquals(6, log.results.size());
		assertEquals("/1/data", log.results.get(0).path);
		assertEquals("/1/data", log.results.get(1).path);
		assertEquals("/1/login", log.results.get(2).path);
		assertEquals("/1/credentials", log.results.get(3).path);
		assertEquals("/1/credentials", log.results.get(4).path);
		assertEquals("/1/admin/clear", log.results.get(5).path);

		DateTime before = log.results.get(1).receivedAt;

		// superadmin deletes all logs before GET /data requests
		superadmin.logs().delete(before);

		// superadmin checks all test backend logs are deleted but ...
		log = superadmin.logs().get(10, true);

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
		superdog().logs().delete(before);

		// superadmin checks all test backend logs are deleted but ...
		log = superdog().logs().get(10, true);

		assertEquals(4, log.total);
		assertEquals("DELETE", log.results.get(0).method);
		assertEquals("/1/log", log.results.get(0).path);
		assertEquals("GET", log.results.get(1).method);
		assertEquals("/1/log", log.results.get(1).path);
		assertEquals("DELETE", log.results.get(2).method);
		assertEquals("/1/log", log.results.get(2).path);
		assertEquals("GET", log.results.get(3).method);
		assertEquals("/1/log", log.results.get(3).path);
	}

	@Test
	public void searchInLogs() {

		// prepare
		prepareTest();

		// creates test backend and user
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearRootBackend();
		guest.get("/1/data").go(200);
		guest.get("/1/data/user").go(403);
		SpaceDog vince = createTempDog(superadmin, "vince").login();
		vince.credentials().get(vince.id());

		// superadmin search for test backend logs with status 400 and higher
		ESSearchSourceBuilder query = ESSearchSourceBuilder.searchSource()//
				.query(ESQueryBuilders.rangeQuery("status").gte(400))//
				.sort(ESSortBuilders.fieldSort("receivedAt").order(ESSortOrder.DESC));
		LogSearchResults results = superadmin.logs().search(query, true);
		assertEquals(1, results.results.size());
		assertEquals("/1/data/user", results.results.get(0).path);
		assertEquals(403, results.results.get(0).status);

		// superadmin search for test backend logs
		// with credentials type equal to superadmin and lower
		query = ESSearchSourceBuilder.searchSource()//
				.query(ESQueryBuilders.termsQuery("credentials.type", "superadmin", "user", "guest"))//
				.sort(ESSortBuilders.fieldSort("receivedAt").order(ESSortOrder.DESC));
		results = superadmin.logs().search(query, true);
		assertEquals(6, results.results.size());
		assertEquals("/1/log/search", results.results.get(0).path);
		assertEquals("/1/credentials/" + vince.id(), results.results.get(1).path);
		assertEquals("/1/login", results.results.get(2).path);
		assertEquals("/1/credentials", results.results.get(3).path);
		assertEquals("/1/data/user", results.results.get(4).path);
		assertEquals("/1/data", results.results.get(5).path);

		// superadmin search for test backend log to only get user and lower logs
		query = ESSearchSourceBuilder.searchSource()//
				.query(ESQueryBuilders.termsQuery("credentials.type", "user", "guest"))//
				.sort(ESSortBuilders.fieldSort("receivedAt").order(ESSortOrder.DESC));
		results = superadmin.logs().search(query, true);
		assertEquals(4, results.results.size());
		assertEquals("/1/credentials/" + vince.id(), results.results.get(0).path);
		assertEquals("/1/login", results.results.get(1).path);
		assertEquals("/1/data/user", results.results.get(2).path);
		assertEquals("/1/data", results.results.get(3).path);

		// superadmin search for test backend log to only get guest logs
		query = ESSearchSourceBuilder.searchSource()//
				.query(ESQueryBuilders.termQuery("credentials.type", "guest"))//
				.sort(ESSortBuilders.fieldSort("receivedAt").order(ESSortOrder.DESC));
		results = superadmin.logs().search(query, true);
		assertEquals(2, results.results.size());
		assertEquals("/1/data/user", results.results.get(0).path);
		assertEquals("/1/data", results.results.get(1).path);

		// superadmin gets all test backend logs
		query = ESSearchSourceBuilder.searchSource()//
				.query(ESQueryBuilders.matchAllQuery())//
				.sort(ESSortBuilders.fieldSort("receivedAt").order(ESSortOrder.DESC))//
				.size(20);
		results = superadmin.logs().search(query, true);
		assertEquals(11, results.results.size());
		assertEquals("/1/log/search", results.results.get(0).path);
		assertEquals("/1/log/search", results.results.get(1).path);
		assertEquals("/1/log/search", results.results.get(2).path);
		assertEquals("/1/log/search", results.results.get(3).path);
		assertEquals("/1/credentials/" + vince.id(), results.results.get(4).path);
		assertEquals("/1/login", results.results.get(5).path);
		assertEquals("/1/credentials", results.results.get(6).path);
		assertEquals("/1/data/user", results.results.get(7).path);
		assertEquals("/1/data", results.results.get(8).path);
		assertEquals("/1/credentials", results.results.get(9).path);
		assertEquals("/1/admin/clear", results.results.get(10).path);
	}

	@Test
	public void pingRequestsAreNotLogged() {

		// prepare
		prepareTest();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearRootBackend();

		// guest (or load balancer) pings his backend
		guest.get("").go(200);
		guest.get("/").go(200);

		// check those pings are not logged
		LogSearchResults results = superadmin.logs().get(10, true);
		assertEquals(2, results.total);
		assertEquals("/1/credentials", results.results.get(0).path);
		assertEquals("/1/admin/clear", results.results.get(1).path);
	}

	@Test
	public void webRootRequestsAreLogged() {

		// prepare
		prepareTest();
		SpaceDog wwwGuest = SpaceDog.dog(SpaceEnv.env().wwwBackend());
		SpaceDog superadmin = clearRootBackend();

		// wwwGuest fails to loads root page since doesn't exist
		wwwGuest.get("").go(404);
		wwwGuest.get("/").go(404);

		// check those web requests are logged
		LogSearchResults results = superadmin.logs().get(10, true);
		assertEquals(4, results.total);
		assertEquals("/", results.results.get(0).path);
		assertEquals("/", results.results.get(1).path);
		assertEquals("/1/credentials", results.results.get(2).path);
		assertEquals("/1/admin/clear", results.results.get(3).path);
	}

	@Test
	public void doAFewThingsAndGetTheLogs() {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearRootBackend();

		// create message schema in test backend
		Schema schema = Schema.builder("message").text("text")//
				.acl(Roles.user, Permission.create, Permission.search).build();
		superadmin.schemas().set(schema);

		// create a user in test backend
		SpaceDog user = createTempDog(superadmin, "user").login();

		// create message in test backend
		JsonDataObject message = user.data()//
				.save("message", Json.object("text", "What's up boys?"));

		// find message by id in test backend
		user.data().fetch(message);

		// find all messages in test backend
		user.data().getAllRequest().type("message").go();

		// get all test backend logs
		// the delete request is not part of the logs
		// since log starts with the backend creation
		List<LogItem> results = superadmin.logs().get(10, true).results;
		assertEquals(8, results.size());
		assertEquals("GET", results.get(0).method);
		assertEquals("/1/data/message", results.get(0).path);
		assertEquals("GET", results.get(1).method);
		assertEquals("/1/data/message/" + message.id(), results.get(1).path);
		assertEquals("POST", results.get(2).method);
		assertEquals("/1/data/message", results.get(2).path);
		assertEquals("GET", results.get(3).method);
		assertEquals("/1/login", results.get(3).path);
		assertEquals("POST", results.get(4).method);
		assertEquals("/1/credentials", results.get(4).path);
		assertEquals("PUT", results.get(5).method);
		assertEquals("/1/schemas/message", results.get(5).path);
		assertEquals("POST", results.get(6).method);
		assertEquals("/1/credentials", results.get(6).path);
		assertEquals("POST", results.get(7).method);
		assertEquals("/1/admin/clear", results.get(7).path);
	}

	@Test
	public void checkPasswordsAreNotLogged() {

		prepareTest();
		SpaceDog superadmin = clearRootBackend();
		SpaceDog fred = createTempDog(superadmin, "fred").login();

		String passwordResetCode = superadmin.delete("/1/credentials/{id}/password")//
				.routeParam("id", fred.id()).go(200)//
				.getString("passwordResetCode");

		SpaceRequest.post("/1/credentials/{id}/password")//
				.routeParam("id", fred.id())//
				.queryParam("passwordResetCode", passwordResetCode)//
				.backend(superadmin).formField("password", "hi fred 2").go(200);

		SpaceRequest.put("/1/credentials/{id}/password").backend(superadmin)//
				.routeParam("id", fred.id()).basicAuth("fred", "hi fred 2")//
				.formField("password", "hi fred 3").go(200);

		List<LogItem> results = superadmin.logs().get(10, true).results;
		assertEquals(7, results.size());
		assertEquals("PUT", results.get(0).method);
		assertEquals("/1/credentials/" + fred.id() + "/password", results.get(0).path);
		assertEquals("********", results.get(0).getParameter(PASSWORD_FIELD));
		assertEquals("POST", results.get(1).method);
		assertEquals("/1/credentials/" + fred.id() + "/password", results.get(1).path);
		assertEquals("********", results.get(1).getParameter(PASSWORD_FIELD));
		assertEquals(passwordResetCode, //
				results.get(1).getParameter(PASSWORD_RESET_CODE_FIELD));
		assertEquals("DELETE", results.get(2).method);
		assertEquals("/1/credentials/" + fred.id() + "/password", results.get(2).path);
		assertEquals(passwordResetCode, //
				results.get(2).response.get(PASSWORD_RESET_CODE_FIELD).asText());
		assertEquals("GET", results.get(3).method);
		assertEquals("/1/login", results.get(3).path);
		assertEquals("POST", results.get(4).method);
		assertEquals("/1/credentials", results.get(4).path);
		assertEquals("fred", results.get(4).payload.get(USERNAME_FIELD).asText());
		assertEquals("********", results.get(4).payload.get(PASSWORD_FIELD).asText());
		assertEquals("POST", results.get(5).method);
		assertEquals("/1/credentials", results.get(5).path);
		assertEquals("superadmin", results.get(5).payload.get(USERNAME_FIELD).asText());
		assertEquals("********", results.get(5).payload.get(PASSWORD_FIELD).asText());
		assertEquals("POST", results.get(6).method);
		assertEquals("/1/admin/clear", results.get(6).path);
	}

	@Test
	public void testSpecialCases() {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearRootBackend();

		// fails because invalid body
		superadmin.put("/1/schemas/toto").bodyString("XXX").go(400);

		// but logs the failed request without the json content
		LogItem logItem = superadmin.logs().get(1, true).results.get(0);
		assertEquals("PUT", logItem.method);
		assertEquals("/1/schemas/toto", logItem.path);
		assertEquals(400, logItem.status);
		assertNull(logItem.payload);
		assertNull(logItem.parameters);

		// check that log response results are not logged
		superadmin.logs().get(10);
		logItem = superadmin.logs().get(1, true).results.get(0);
		assertEquals("GET", logItem.method);
		assertEquals("/1/log", logItem.path);
		assertNull(logItem.response.get("results"));

		// Headers are logged if not empty
		// and 'Authorization' header is not logged
		superadmin.get("/1/log")//
				.setHeader("x-empty", "")//
				.setHeader("x-blank", " ")//
				.setHeader("x-color", "YELLOW")//
				.addHeader("x-color-list", "RED")//
				.addHeader("x-color-list", "BLUE")//
				.addHeader("x-color-list", "GREEN")//
				.go(200);

		logItem = superadmin.logs().get(1, true).results.get(0);
		assertTrue(logItem.getHeader(SpaceHeaders.AUTHORIZATION).isEmpty());
		assertTrue(logItem.getHeader("x-empty").isEmpty());
		assertTrue(logItem.getHeader("x-blank").isEmpty());
		assertTrue(logItem.getHeader("x-color").contains("YELLOW"));
		assertTrue(logItem.getHeader("x-color-list").contains("RED"));
		assertTrue(logItem.getHeader("x-color-list").contains("BLUE"));
		assertTrue(logItem.getHeader("x-color-list").contains("GREEN"));
	}
}
