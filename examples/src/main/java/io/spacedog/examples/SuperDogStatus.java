/**
 * © David Attias 2015
 */
package io.spacedog.examples;

import java.util.Iterator;

import org.junit.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Json;

public class SuperDogStatus extends Assert {

	public static void main(String[] args) throws Exception {

		SpaceRequest.setLogDebug(false);

		ObjectNode backends = SpaceRequest.get("/1/backend").queryString("size", "100")//
				.superdogAuth().go(200).objectNode();

		log("[%s] backends:", backends.get("total").asLong());

		Iterator<JsonNode> elements = backends.get("results").elements();
		while (elements.hasNext())
			log("\t%s", elements.next().get("backendId").asText());

		elements = backends.get("results").elements();
		while (elements.hasNext()) {
			JsonNode account = elements.next();
			String backendId = account.get("backendId").asText();
			// String backendKey = getBackendKey(account);

			log();
			log("**** %s ****", backendId);

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

			ObjectNode log = SpaceRequest.get("/1/log?size=1&logType=ADMIN")//
					.superdogAuth(backendId)//
					.go(200)//
					.objectNode();

			log("Last user request:");
			log(log.get("results").get(0));
		}
	}

	private static void log() {
		System.out.println();
	}

	private static void log(JsonNode node) throws JsonProcessingException {
		log(Json.getMapper().writerWithDefaultPrettyPrinter().writeValueAsString(node));
	}

	private static void log(String string, Object... args) {
		System.out.println(String.format(string, args));
	}
}
