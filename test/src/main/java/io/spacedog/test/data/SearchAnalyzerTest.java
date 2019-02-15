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

public class SearchAnalyzerTest extends SpaceTest {

	private SpaceDog superadmin;

	@Test
	public void prefixSearchWithFrenchCustomAnalyzer() {

		prepareTest(true, true);
		superadmin = clearServer();
		byte[] json = ClassResources.loadAsBytes(this, "message.mapping.json");
		superadmin.schemas().set(Json.toPojo(json, Schema.class));

		indexMessage("Google");

		matchPrefixMessage("g");
		matchPrefixMessage("G");
		matchPrefixMessage("go");
		matchPrefixMessage("Go");
		matchPrefixMessage("goo");
		matchPrefixMessage("GOO");
		matchPrefixMessage("goog");
		matchPrefixMessage("googl");
		matchPrefixMessage("google");
		matchPrefixMessage("goOgLe");
		noMatchPrefixMessage("googles");

		indexMessage("L'étable de Marie");

		matchPrefixMessage("l");
		matchPrefixMessage("l'");
		matchPrefixMessage("l'ét");
		matchPrefixMessage("l'etAb");
		matchPrefixMessage("etab");
		matchPrefixMessage("d");
		matchPrefixMessage("de");
		matchPrefixMessage("maR");

		noMatchPrefixMessage("le");
		noMatchPrefixMessage("létable");
		noMatchPrefixMessage("tabler");
		noMatchPrefixMessage("marier");
		noMatchPrefixMessage("maa");
		noMatchPrefixMessage("tabe");

		indexMessage("j' zi");

		matchPrefixMessage("j");
		matchPrefixMessage("j'z");
		matchPrefixMessage("j z");

		noMatchPrefixMessage("jz");

		indexMessage("17F11");

		matchPrefixMessage("1");
		matchPrefixMessage("17");
		matchPrefixMessage("17f");
		matchPrefixMessage("17f11");

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
		indexMessage("Les écoles enseignent");

		matchMessage("ecole");
		matchMessage("école");
		matchMessage("EcoLes");
		matchMessage("écolles");
		matchMessage("écol");

		matchMessage("enseignent");
		matchMessage("enSEIgnènt");

		noMatchMessage("enseigne");

		///////////////
		indexMessage("les Bœufs ruminent");

		matchMessage("BoEuf");
		matchMessage("Bœuf");
		matchMessage("boeufs");

		///////////////
		indexMessage("Je suis :), tu es :(");

		matchMessage("heureux");
		matchMessage("heureu");
		matchMessage("triste");
		matchMessage("tristes");
		matchMessage(":)");
		matchMessage(":(");

		noMatchMessage(":-)");

		///////////////
		indexMessage("1234.567");

		matchMessage("1234");
		matchMessage("1234.");
		matchMessage("1234,");
		matchMessage("1234.567");
		matchMessage("1234,567");
		matchMessage("1234567");
		matchMessage("567");
		matchMessage(".567");
		matchMessage(",567");

		// match because default operator is OR
		// and at least one token match
		matchMessage("1234.56");
		matchMessage("234.567");

		noMatchMessage("234.56");
	}

	private void indexMessage(String text) {
		superadmin.data().save("message", Json.object("text", text));
	}

	private void matchMessage(String text) {
		assertEquals(1, search(text, "text"));
	}

	private void matchPrefixMessage(String text) {
		assertEquals(1, searchPrefix(text, "text"));
	}

	private void noMatchMessage(String text) {
		assertEquals(0, search(text, "text"));
	}

	private void noMatchPrefixMessage(String text) {
		assertEquals(0, searchPrefix(text, "text"));
	}

	@Test
	public void searchAmountWithCustomAnalyzer() {

		prepareTest(true, true);
		superadmin = clearServer();
		byte[] json = ClassResources.loadAsBytes(this, "amount.mapping.json");
		superadmin.schemas().set(Json.toPojo(json, Schema.class));

		indexAmount(1234l);

		matchAmount("1234");
		matchAmount("12.34");
		matchAmount("12,34");
		matchAmount("-12,34");
		matchAmount("12");
		noMatchAmount("34");
	}

	private void indexAmount(long amount) {
		superadmin.data().save("amount", Json.object("value", amount));
	}

	private void matchAmount(String amount) {
		assertEquals(1, search(amount, "value.text"));
	}

	private void noMatchAmount(String amount) {
		assertEquals(0, search(amount, "value.text"));
	}

	//
	// Implementation
	//

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
