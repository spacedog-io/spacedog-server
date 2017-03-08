/**
 * © David Attias 2015
 */
package io.spacedog.examples;

import java.util.Iterator;

import org.junit.Assert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.rest.SpaceRequest;
import io.spacedog.utils.Utils;

public class SuperDogStatus extends Assert {

	public static void main(String[] args) throws Exception {

		SpaceRequest.setLogDebug(false);

		ObjectNode backends = SpaceRequest.get("/1/backend").size(100)//
				.superdogAuth().go(200).objectNode();

		Utils.info("[%s] backends:", backends.get("total").asLong());

		Iterator<JsonNode> elements = backends.get("results").elements();
		while (elements.hasNext())
			Utils.info("\t%s", elements.next().get("backendId").asText());

		elements = backends.get("results").elements();
		while (elements.hasNext()) {
			JsonNode account = elements.next();
			String backendId = account.get("backendId").asText();
			// String backendKey = getBackendKey(account);

			Utils.info();
			Utils.info("**** %s ****", backendId);

			// TODO count backend objects when superdogs
			// can access /1/data like a regular backend admin

			// long total = SpaceRequest.get("/1/data")//
			// .queryString("size", "0")//
			// .superdogAuth()//
			// .go(200)//
			// .getFromJson("total")//
			// .asLong();
			//
			// log("Total number of objects = %s", total);

			ObjectNode log = SpaceRequest.get("/1/log?logType=ADMIN")//
					.size(1)//
					.superdogAuth(backendId)//
					.go(200)//
					.objectNode();

			Utils.info("Last user request:");
			Utils.info("results", log.get("results").get(0));
		}
	}
}
