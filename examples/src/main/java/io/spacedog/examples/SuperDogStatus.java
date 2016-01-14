/**
 * © David Attias 2015
 */
package io.spacedog.examples;

import java.util.Iterator;

import org.junit.Assert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceRequest;

public class SuperDogStatus extends Assert {

	public static void main(String[] args) throws Exception {
		ObjectNode accounts = SpaceRequest.get("/v1/admin/account").queryString("size", "100")//
				.superdogAuth().go(200).objectNode();

		Iterator<JsonNode> elements = accounts.get("results").elements();
		while (elements.hasNext()) {
			String backendId = elements.next().get("backendId").asText();
			SpaceRequest.get("/v1/admin/log/" + backendId)//
					.superdogAuth()//
					.go(200);
		}
	}

}
