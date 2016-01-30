package io.spacedog.watchdog;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceDogHelper.Account;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Json;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class AggregationTest {

	@Test
	public void searchForAggregations() throws Exception {

		SpaceDogHelper.prepareTest();
		Account testAccount = SpaceDogHelper.resetTestAccount();

		SpaceDogHelper.createUser(testAccount, "riri", "hi riri", "hello@disney.com");
		SpaceDogHelper.createUser(testAccount, "fifi", "hi fifi", "hello@disney.com");
		SpaceDogHelper.createUser(testAccount, "loulou", "hi loulou", "hello@disney.com");
		SpaceDogHelper.createUser(testAccount, "donald", "hi donald", "donald@disney.com");
		SpaceDogHelper.createUser(testAccount, "mickey", "hi mickey", "mickey@disney.com");

		ObjectNode query = Json.objectBuilder()//
				.put("size", 0)//
				.object("aggs")//
				.object("objectCount")//
				.object("cardinality")//
				.put("field", "email")//
				.build();

		SpaceRequest.post("/v1/search?refresh=true").backendKey(testAccount).body(query).go(200)//
				.assertEquals(3, "aggregations.objectCount.value");

	}

}
