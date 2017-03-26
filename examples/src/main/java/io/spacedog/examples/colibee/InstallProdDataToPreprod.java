/**
 * © David Attias 2015
 */
package io.spacedog.examples.colibee;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceTest;
import io.spacedog.utils.Json7;
import io.spacedog.utils.Utils;

public class InstallProdDataToPreprod extends SpaceTest {

	@Test
	public void run() {

		// upgradeAliases();
		upgradeCredentials();

	}

	void upgradeAliases() {

		SpaceRequest
				.post("http://connectapipreproduction.colibee.com:9200"//
						+ "/_aliases")//
				.bodyResource(this.getClass(), "aliases.json")//
				.go(200);
	}

	void upgradeCredentials() {

		ObjectNode query = Json7.objectBuilder()//
				.put("size", 400)//
				.object("query")//
				.object("term")//
				.put("backendId", "connectapi")//
				.build();

		JsonNode node = SpaceRequest
				.post("http://connectapipreproduction.colibee.com:9200"//
						+ "/spacedog-credentials/credentials/_search")//
				.body(query).go(200).get("hits.hits");

		for (JsonNode credentials : node) {
			String id = credentials.get("_id").asText();
			Utils.info("Updating credentials [%s] ...", id);

			SpaceRequest
					.post("http://connectapipreproduction.colibee.com:9200"//
							+ "/spacedog-credentials/credentials/" //
							+ id + "/_update")//
					.body("doc", Json7.object("backendId", "connectapipreproduction"))//
					.go(200);
		}
	}
}
