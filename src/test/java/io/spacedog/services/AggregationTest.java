package io.spacedog.services;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceDogHelper.Account;
import io.spacedog.client.SpaceRequest;

public class AggregationTest {

	@Test
	public void shouldSearchForAggregations() throws Exception {

		Account testAccount = SpaceDogHelper.resetTestAccount();

		SpaceDogHelper.createUser(testAccount, "riri", "hi riri", "hello@disney.com");
		SpaceDogHelper.createUser(testAccount, "fifi", "hi fifi", "hello@disney.com");
		SpaceDogHelper.createUser(testAccount, "loulou", "hi loulou", "hello@disney.com");
		SpaceDogHelper.createUser(testAccount, "donald", "hi donald", "donald@disney.com");
		SpaceDogHelper.createUser(testAccount, "mickey", "hi mickey", "mickey@disney.com");

		SpaceDogHelper.refresh(testAccount);

		ObjectNode query = Json.startObject()//
				.put("size", 0)//
				.startObject("aggs")//
				.startObject("objectCount")//
				.startObject("cardinality")//
				.put("field", "email")//
				.build();

		SpaceRequest.post("/v1/data/search").backendKey(testAccount).body(query).go(200).assertEquals(3,
				"aggregations.objectCount.value");

	}

}
