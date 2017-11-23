/**
 * © David Attias 2015
 */
package io.spacedog.test;

import org.junit.Test;

import io.spacedog.client.DataEndpoint.SearchRequest;
import io.spacedog.client.SpaceDog;
import io.spacedog.client.elastic.ESQueryBuilders;
import io.spacedog.client.elastic.ESSearchSourceBuilder;
import io.spacedog.model.Schema;
import io.spacedog.utils.Json;

public class SearchServiceFrenchTest extends SpaceTest {

	private SpaceDog superadmin;

	@Test
	public void searchWithFrenchMaxAnalyser() {

		prepareTest();
		superadmin = resetTestBackend();
		superadmin.schemas().set(Schema.builder("message").frenchMax().text("text").build());

		///////////////
		index("Les écoles enseignent");

		match("ecole");
		match("école");
		match("EcoLes");
		match("écolles");
		match("écol");

		match("enseignent");
		match("enSEIgnènt");

		noMatch("enseigne");

		///////////////
		index("les Bœufs ruminent");

		match("BoEuf");
		match("Bœuf");
		match("boeufs");

		///////////////
		index("Je suis :), tu es :(");

		match("heureux");
		match("heureu");
		match("triste");
		match("tristes");
		match(":)");
		match(":(");

		noMatch(":-)");

		///////////////
		index("1234.56");

		match("1234");
		match("1234.");
		match("1234,");
		match("1234.56");
		match("1234,56");
		match("56");
		match(".56");
		match(",56");
		match("234.56");
		match("1234.5");

		noMatch("234.5");
		noMatch("1");
		noMatch("2");
		noMatch("3");
		noMatch("4");

	}

	private void index(String text) {
		superadmin.data().save("message", Json.object("text", text));
	}

	private void match(String text) {
		assertEquals(1, search(text, "text"));
		// assertEquals(1, search(text, "_all"));
	}

	private void noMatch(String text) {
		assertEquals(0, search(text, "text"));
		// assertEquals(0, search(text, "_all"));
	}

	private long search(String text, String field) {
		ESSearchSourceBuilder source = ESSearchSourceBuilder.searchSource()//
				.query(ESQueryBuilders.matchQuery(field, text));
		SearchRequest searchRequest = superadmin.data().searchRequest().refresh().source(source);
		return searchRequest.go().total;
	}
}
