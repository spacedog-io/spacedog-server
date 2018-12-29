/**
 * © David Attias 2015
 */
package io.spacedog.test.data;

import org.junit.Test;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.elastic.ESQueryBuilders;
import io.spacedog.client.elastic.ESSearchSourceBuilder;
import io.spacedog.client.schema.Schema;
import io.spacedog.test.SpaceTest;
import io.spacedog.utils.ClassResources;
import io.spacedog.utils.Json;

public class SearchRestyFrenchTest extends SpaceTest {

	private SpaceDog superadmin;

	@Test
	public void prefixSearchWithFrenchSpecificAnalyzer() {

		prepareTest(true, true);
		superadmin = clearServer();
		byte[] mapping = ClassResources.loadAsBytes(this, "message.mapping.json");
		superadmin.schemas().set(Json.toPojo(mapping, Schema.class));

		index("Google");

		matchPrefix("g");
		matchPrefix("G");
		matchPrefix("go");
		matchPrefix("Go");
		matchPrefix("goo");
		matchPrefix("GOO");
		matchPrefix("goog");
		matchPrefix("googl");
		matchPrefix("google");
		matchPrefix("goOgLe");
		noMatchPrefix("googles");

		index("L'étable de Marie");

		matchPrefix("l");
		matchPrefix("l'");
		matchPrefix("l'ét");
		matchPrefix("l'etAb");
		matchPrefix("etab");
		matchPrefix("d");
		matchPrefix("de");
		matchPrefix("maR");

		noMatchPrefix("le");
		noMatchPrefix("létable");
		noMatchPrefix("tabler");
		noMatchPrefix("marier");
		noMatchPrefix("maa");
		noMatchPrefix("tabe");

		index("j' zi");

		matchPrefix("j");
		matchPrefix("j'z");
		matchPrefix("j z");

		noMatchPrefix("jz");

		index("17F11");

		matchPrefix("1");
		matchPrefix("17");
		matchPrefix("17f");
		matchPrefix("17f11");

		// noMatchPrefix("11");
		// noMatchPrefix("F");
		// noMatchPrefix("F11");
	}

	@Test
	public void searchWithFrenchMaxAnalyser() {

		prepareTest();
		superadmin = clearServer();
		superadmin.schemas().set(Schema.builder("message")//
				.text("text").frenchMax().build());

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
		index("1234.567");

		match("1234");
		match("1234.");
		match("1234,");
		match("1234.567");
		match("1234,567");
		match("1234567");
		match("567");
		match(".567");
		match(",567");

		// match because default operator is OR
		// and at least one token match
		match("1234.56");
		match("234.567");

		noMatch("234.56");
	}

	private void index(String text) {
		superadmin.data().save("message", Json.object("text", text));
	}

	private void match(String text) {
		assertEquals(1, search(text, "text"));
	}

	private void matchPrefix(String text) {
		assertEquals(1, searchPrefix(text, "text"));
	}

	private void noMatch(String text) {
		assertEquals(0, search(text, "text"));
	}

	private void noMatchPrefix(String text) {
		assertEquals(0, searchPrefix(text, "text"));
	}

	private long search(String text, String field) {
		ESSearchSourceBuilder source = ESSearchSourceBuilder.searchSource()//
				.query(ESQueryBuilders.matchQuery(field, text));
		return superadmin.data().prepareSearch().refresh(true).source(source.toString()).go().total;
	}

	private long searchPrefix(String text, String field) {
		ESSearchSourceBuilder source = ESSearchSourceBuilder.searchSource()//
				.query(ESQueryBuilders.matchPhrasePrefixQuery(field, text));
		return superadmin.data().prepareSearch().refresh(true).source(source.toString()).go().total;
	}
}
