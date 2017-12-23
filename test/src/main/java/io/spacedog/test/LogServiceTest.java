package io.spacedog.test;

import java.util.Iterator;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.spacedog.client.LogEndpoint.LogItem;
import io.spacedog.client.LogEndpoint.LogSearchResults;
import io.spacedog.client.SpaceDog;
import io.spacedog.client.elastic.ESQueryBuilders;
import io.spacedog.client.elastic.ESSearchSourceBuilder;
import io.spacedog.client.elastic.ESSortBuilders;
import io.spacedog.client.elastic.ESSortOrder;
import io.spacedog.http.SpaceBackend;
import io.spacedog.http.SpaceRequest;
import io.spacedog.model.JsonDataObject;
import io.spacedog.model.Permission;
import io.spacedog.model.Schema;
import io.spacedog.utils.Json;
import io.spacedog.utils.Roles;
import io.spacedog.utils.SpaceHeaders;

public class LogServiceTest extends SpaceTest {

	@Test
	public void purgeBackendLogs() throws InterruptedException {

		// prepare
		prepareTest();
		SpaceDog test2 = resetTest2Backend();
		SpaceDog test = resetTestBackend();
		SpaceDog fred = createTempDog(test, "fred").login();

		fred.data().getAllRequest().go();
		fred.data().getAllRequest().go();

		// superadmin gets test2 backend total log count to check
		// later that they aren't affected by test backend log purge
		long test2TotalLogs = test2.logs().get(0, true).total;

		// superadmin checks everything is in place
		LogSearchResults log = test.logs().get(6, true);

		assertEquals(4, log.results.size());
		assertEquals("/1/data", log.results.get(0).path);
		assertEquals("/1/data", log.results.get(1).path);
		assertEquals("/1/login", log.results.get(2).path);
		assertEquals("/1/credentials", log.results.get(3).path);

		DateTime before = log.results.get(1).receivedAt;

		// superadmin deletes all logs before GET /data requests
		test.logs().delete(before);

		// superadmin checks all test backend logs are deleted but ...
		log = test.logs().get(10, true);

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
		superdog(test).logs().delete(before);

		// superadmin checks all test backend logs are deleted but ...
		log = superdog(test).logs().get(10, true);

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
		log = test2.logs().get(0, true);

		assertEquals(test2TotalLogs + 1, log.total);
	}

	@Test
	public void searchInLogs() {

		// prepare
		prepareTest();

		// creates test backend and user
		SpaceDog superadmin = resetTestBackend();
		SpaceDog guest = SpaceDog.backend(superadmin.backend());
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
				.sort(ESSortBuilders.fieldSort("receivedAt").order(ESSortOrder.DESC));
		results = superadmin.logs().search(query, true);
		assertEquals(9, results.results.size());
		assertEquals("/1/log/search", results.results.get(0).path);
		assertEquals("/1/log/search", results.results.get(1).path);
		assertEquals("/1/log/search", results.results.get(2).path);
		assertEquals("/1/log/search", results.results.get(3).path);
		assertEquals("/1/credentials/" + vince.id(), results.results.get(4).path);
		assertEquals("/1/login", results.results.get(5).path);
		assertEquals("/1/credentials", results.results.get(6).path);
		assertEquals("/1/data/user", results.results.get(7).path);
		assertEquals("/1/data", results.results.get(8).path);
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
					&& element.get("credentials").get("backendId").asText().equals(SpaceBackend.defaultBackendId()))
				Assert.fail();
		}
	}

	@Test
	public void doAFewThingsAndGetTheLogs() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		SpaceDog superadmin2 = resetTest2Backend();

		// create message schema in test backend
		Schema schema = Schema.builder("message").text("text")//
				.acl(Roles.user, Permission.create, Permission.search).build();
		superadmin.schemas().set(schema);

		// create a user in test backend
		SpaceDog user = createTempDog(superadmin, "user").login();

		// create a user in test2 backend
		createTempDog(superadmin2, "user2").login();

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
		assertEquals(6, results.size());
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

		// get all test2 backend logs
		results = superadmin2.logs().get(4, true).results;
		assertEquals(2, results.size());
		assertEquals("GET", results.get(0).method);
		assertEquals("/1/login", results.get(0).path);
		assertEquals("POST", results.get(1).method);
		assertEquals("/1/credentials", results.get(1).path);
		assertEquals("********", results.get(1).payload.get(PASSWORD_FIELD).asText());

		// after backend deletion, logs are not accessible to backend
		superadmin.admin().deleteBackend(superadmin.backendId());
		superadmin.get("/1/log").go(401);

		superadmin2.admin().deleteBackend(superadmin2.backendId());
		superadmin2.get("/1/log").go(401);
	}

	@Test
	public void checkPasswordsAreNotLogged() {

		prepareTest();
		SpaceDog superadmin = resetTestBackend();
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

		List<LogItem> results = superadmin.logs().get(7, true).results;
		assertEquals(5, results.size());
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
	}

	@Test
	public void testSpecialCases() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();

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

	@Test
	public void defaultLogShouldContainDeleteAndCreateBackendLog() {

		// prepare
		prepareTest();
		resetTestBackend();

		// superdog gets default backend (api) log
		List<LogItem> results = superdog().logs().get(2, true).results;
		assertEquals("POST", results.get(0).method);
		assertEquals("/1/backends", results.get(0).path);
		assertEquals("test", results.get(0).payload.get(BACKEND_ID_FIELD).asText());
		assertEquals("********", Json.get(results.get(0).payload, "superadmin.password").asText());
		assertEquals("DELETE", results.get(1).method);
		assertEquals("/1/backends/test", results.get(1).path);
	}

}
