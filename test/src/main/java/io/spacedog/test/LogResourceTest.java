package io.spacedog.test;

import java.util.Iterator;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.LogEndpoint.LogSearchResults;
import io.spacedog.client.elastic.ESQueryBuilders;
import io.spacedog.client.elastic.ESSearchSourceBuilder;
import io.spacedog.client.elastic.ESSortBuilders;
import io.spacedog.client.elastic.ESSortOrder;
import io.spacedog.http.SpaceBackend;
import io.spacedog.http.SpaceRequest;
import io.spacedog.http.SpaceTest;

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

		assertEquals(5, log.results.size());
		assertEquals("/1/data", log.results.get(0).path);
		assertEquals("/1/data", log.results.get(1).path);
		assertEquals("/1/login", log.results.get(2).path);
		assertEquals("/1/credentials", log.results.get(3).path);
		assertEquals("/1/backend", log.results.get(4).path);

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
	public void searchInLogs() {

		// prepare
		prepareTest();

		// creates test backend and user
		SpaceDog superadmin = resetTestBackend();
		SpaceDog guest = SpaceDog.backend(superadmin);
		guest.get("/1/data").go(200);
		guest.get("/1/data/user").go(403);
		SpaceDog vince = signUp(superadmin, "vince", "hi vince");
		vince.get("/1/credentials/" + vince.id()).go(200);

		// superadmin search for test backend logs with status 400 and higher
		ESSearchSourceBuilder query = ESSearchSourceBuilder.searchSource()//
				.query(ESQueryBuilders.rangeQuery("status").gte(400))//
				.sort(ESSortBuilders.fieldSort("receivedAt").order(ESSortOrder.DESC));
		LogSearchResults results = superadmin.log().search(query, true);
		assertEquals(1, results.results.size());
		assertEquals("/1/data/user", results.results.get(0).path);
		assertEquals(403, results.results.get(0).status);

		// superadmin search for test backend logs
		// with credentials type equal to superadmin and lower
		query = ESSearchSourceBuilder.searchSource()//
				.query(ESQueryBuilders.termsQuery("credentials.type", "superadmin", "user", "guest"))//
				.sort(ESSortBuilders.fieldSort("receivedAt").order(ESSortOrder.DESC));
		results = superadmin.log().search(query, true);
		assertEquals(7, results.results.size());
		assertEquals("/1/log/search", results.results.get(0).path);
		assertEquals("/1/credentials/" + vince.id(), results.results.get(1).path);
		assertEquals("/1/login", results.results.get(2).path);
		assertEquals("/1/credentials", results.results.get(3).path);
		assertEquals("/1/data/user", results.results.get(4).path);
		assertEquals("/1/data", results.results.get(5).path);
		assertEquals("/1/backend", results.results.get(6).path);

		// superadmin search for test backend log to only get user and lower logs
		query = ESSearchSourceBuilder.searchSource()//
				.query(ESQueryBuilders.termsQuery("credentials.type", "user", "guest"))//
				.sort(ESSortBuilders.fieldSort("receivedAt").order(ESSortOrder.DESC));
		results = superadmin.log().search(query, true);
		assertEquals(6, results.results.size());
		assertEquals("/1/credentials/" + vince.id(), results.results.get(0).path);
		assertEquals("/1/login", results.results.get(1).path);
		assertEquals("/1/credentials", results.results.get(2).path);
		assertEquals("/1/data/user", results.results.get(3).path);
		assertEquals("/1/data", results.results.get(4).path);
		assertEquals("/1/backend", results.results.get(5).path);

		// superadmin search for test backend log to only get guest logs
		query = ESSearchSourceBuilder.searchSource()//
				.query(ESQueryBuilders.termQuery("credentials.type", "guest"))//
				.sort(ESSortBuilders.fieldSort("receivedAt").order(ESSortOrder.DESC));
		results = superadmin.log().search(query, true);
		assertEquals(4, results.results.size());
		assertEquals("/1/credentials", results.results.get(0).path);
		assertEquals("/1/data/user", results.results.get(1).path);
		assertEquals("/1/data", results.results.get(2).path);
		assertEquals("/1/backend", results.results.get(3).path);

		// superdog gets all test backend logs
		query = ESSearchSourceBuilder.searchSource()//
				.query(ESQueryBuilders.matchAllQuery())//
				.sort(ESSortBuilders.fieldSort("receivedAt").order(ESSortOrder.DESC));
		results = superadmin.log().search(query, true);
		assertEquals(10, results.results.size());
		assertEquals("/1/log/search", results.results.get(0).path);
		assertEquals("/1/log/search", results.results.get(1).path);
		assertEquals("/1/log/search", results.results.get(2).path);
		assertEquals("/1/log/search", results.results.get(3).path);
		assertEquals("/1/credentials/" + vince.id(), results.results.get(4).path);
		assertEquals("/1/login", results.results.get(5).path);
		assertEquals("/1/credentials", results.results.get(6).path);
		assertEquals("/1/data/user", results.results.get(7).path);
		assertEquals("/1/data", results.results.get(8).path);
		assertEquals("/1/backend", results.results.get(9).path);
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
}
