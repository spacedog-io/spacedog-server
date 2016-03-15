package io.spacedog.watchdog;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceDogHelper.Backend;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Json;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class AggregationTest {

	@Test
	public void searchForAggregations() throws Exception {

		SpaceDogHelper.prepareTest();
		Backend testBackend = SpaceDogHelper.resetTestBackend();

		SpaceDogHelper.createUser(testBackend, "riri", "hi riri", "hello@disney.com");
		SpaceDogHelper.createUser(testBackend, "fifi", "hi fifi", "hello@disney.com");
		SpaceDogHelper.createUser(testBackend, "loulou", "hi loulou", "hello@disney.com");
		SpaceDogHelper.createUser(testBackend, "donald", "hi donald", "donald@disney.com");
		SpaceDogHelper.createUser(testBackend, "mickey", "hi mickey", "mickey@disney.com");

		ObjectNode query = Json.objectBuilder()//
				.put("size", 0)//
				.object("aggs")//
				.object("objectCount")//
				.object("cardinality")//
				.put("field", "email")//
				.build();

		// 4 => 3 distinct disney user emails (hello, donald, mickey)
		// and 1 admin email
		SpaceRequest.post("/1/search?refresh=true").backend(testBackend).body(query).go(200)//
				.assertEquals(4, "aggregations.objectCount.value");

	}

}
