/**
 * © David Attias 2015
 */
package io.spacedog.test;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.model.Schema;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.DataEndpoint.SearchResults;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.Json7;

public class SearchResourceFrenchTest extends SpaceTest {

	private SpaceDog superadmin;

	@Test
	public void searchWithFrenchMaxAnalyser() {

		// prepare
		prepareTest();
		superadmin = resetTestBackend();
		superadmin.schema().set(Schema.builder("message").frenchMax().text("text").build());

		// écoles
		index("Les écoles enseignent");

		match("ecole");
		match("école");
		match("EcoLes");
		match("écolles");
		match("écol");

		match("enseignent");
		match("enSEIgnènt");

		noMatch("enseigne");

		// bœuf
		index("les Bœufs ruminent");

		match("BoEuf");
		match("Bœuf");
		match("boeufs");

		// :) and :(
		index("Je suis :), tu es :(");

		match("heureux");
		match("heureu");
		match("triste");
		match("tristes");
		match(":)");
		match(":(");

		noMatch(":-)");

		//
		index("1234.56");

		match("1234");
		match("1234.");
		match("1234,");
		match("1234.56");
		match("1234,56");
		match("56");
		match(".56");
		match(",56");

		// match because default operator is OR
		// and at least one token match
		match("1234.5");
		match("234.56");

		noMatch("234.5");
	}

	private void index(String text) {
		superadmin.data().create("message", Json7.object("text", text));
	}

	private void match(String text) {
		assertEquals(1, search(text, "text"));
		assertEquals(1, search(text, "_all"));
	}

	private void noMatch(String text) {
		assertEquals(0, search(text, "text"));
		assertEquals(0, search(text, "_all"));
	}

	private long search(String text, String field) {
		String source = Json7.objectBuilder().object("query")//
				.object("match").put(field, text).build().toString();
		SearchResults<ObjectNode> results = superadmin.data()//
				.search(null, source, ObjectNode.class, true);
		return results.total();
	}
}
