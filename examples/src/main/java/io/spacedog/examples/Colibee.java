/**
 * © David Attias 2015
 */
package io.spacedog.examples;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.Resources;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonGenerator;
import io.spacedog.utils.SchemaBuilder3;

public class Colibee extends SpaceClient {

	private ObjectNode schemaConsultant;
	private ObjectNode schemaExpertise;
	private ObjectNode schemaOpportunite;

	private User william;
	private Backend backend;
	private JsonGenerator generator;

	public Colibee() throws Exception {
		schemaConsultant = buildConsultantSchema();
		schemaExpertise = buildExpertiseSchema();
		schemaOpportunite = buildOpportuniteSchema();
		backend = new Backend("colibee", "colibee", "hi colibee", "david@spacedog.io");
		generator = new JsonGenerator();
	}

	@Test
	public void initColibeeBackend() throws Exception {

		// SpaceRequest.configuration().target(SpaceTarget.production);

		resetBackend(backend);
		initUserDefaultSchema(backend);
		william = createUser(backend, "william", "hi willian");
		setSchema(schemaConsultant, backend);
		setSchema(schemaExpertise, backend);

		initGenerator();
		initConsultantWilliam();
		initExpertises();
	}

	private void initGenerator() throws Exception {

		generator.reg("identite.prenom", "William");
		generator.reg("identite.nom", "Miramond", "Lopez");
		generator.reg("identite.titre", "Chef de projet", "Directeur des achats");

		byte[] bytes = Resources.toByteArray(//
				Resources.getResource("io/spacedog/examples/consultant.png"));

		String url = SpaceRequest.put("/1/share/ma-tronche.png").userAuth(william)//
				.body(bytes).go(200).getFromJson("s3").asText();

		generator.reg("identite.photo", url);
	}

	private void initConsultantWilliam() throws Exception {

		ObjectNode consultant = generator.gen(schemaConsultant);

		SpaceRequest.post("/1/data/consultant?id=william")//
				.userAuth(william).body(consultant).go(201);
	}

	private void initExpertises() throws Exception {

		ObjectNode expertise = Json.object();

		expertise.set("secteurs", Json.object("asset-management", "Asset Management", //
				"assurance-vie", "Assurance vie", "automobile", "Automobile"));

		expertise.set("methodes", Json.object("agile-scrum", "Agile / SCRUM", "audit", //
				"Audit", "big-data", "Big Data", "cadrage", "Cadrage"));

		expertise.set("outils", Json.object("ariba", "Ariba", "essbase", "Essbase", //
				"hfm", "HFM", "hp", "HP"));

		expertise.set("fonctions", Json.object("achats", "Achats", "acturiat", "Acturiat", //
				"derivatives", "Derivatives"));

		SpaceRequest.post("/1/data/expertise?id=all").adminAuth(backend).body(expertise).go(201);
	}

	static ObjectNode buildConsultantSchema() {
		return SchemaBuilder3.builder("consultant") //
				.bool("membreColibee")//
				.string("membreNumero").examples("01234567").labels("fr", "N° de membre")//
				.text("resume")//
				.enumm("typePrestations").array().examples("William")//
				.enumm("secteurActivites").array().examples("Automobile")//
				.enumm("savoirFaires").array().examples("Big data")//
				.enumm("fonctions").array().examples("FSI | Risque client")//
				.enumm("outils").array().examples("Clickview")//

				.object("langues").array()//
				.string("langue").examples("Français")//
				.enumm("niveau").examples("Excellent")//
				.close()//

				.object("identite")//
				.string("prenom").examples("William") //
				.string("nom").examples("Lopez", "Miramond", "Attias") //
				.text("titre").french().examples("Chef de projet")//
				.string("photo")//
				.date("indepDepuisLe")//
				.date("dateDeNaissance") //
				.enumm("nationalite").examples("Française") //
				.string("emailColibee").examples("william@colibee.net")//
				.string("emailSecondaire").examples("william@gmail.com")//
				.string("telMobile").examples("+ 33 6 62 01 67 56")//
				.string("telAutre").examples("+ 33 1 42 01 67 56")//

				.object("domicile")//
				.text("adresse").french().examples("18 rue Yves Toudic, 75010 Paris")//
				.string("ville").examples("Paris") //
				.geopoint("geopoint")//
				.close()//

				.close()//

				.object("contrat")//
				.text("raisonSociale").french().examples("William Consulting")//
				.enumm("formeJuridique").examples("SARL")//
				.text("adresseSiege").examples("18 rue Yves Toudic, 75010 Paris")//
				.string("siren").examples("923-567-78")//
				.string("tva").examples("FR392356778")//
				.string("urssaf").examples("UR78T54453")//
				.string("siretSiege").examples("923-567-78-00014")//
				.close()//

				.object("prefMission")//
				.enumm("zoneMobilite").examples("Local")//
				.enumm("freqDeplacement").examples("Mensuel")//
				.enumm("rythmeTravail").examples("Temps complet")//
				.integer("tarifCible").examples(1200)//
				.integer("tarifMin").examples(800)//
				.text("note").examples("J'accepte le temps partiel si nécessaire.")//
				.close()//

				.object("cv").array()//
				.text("titre").french().examples("Directeur de projet")//
				.date("date").examples("2016-05-12")//
				.enumm("type").examples("Chronologique")//
				.text("note").french().examples("CV pour la proposition BNP")//
				.string("url")//
				.bool("archive")//
				.close()//

				.object("experiences").array()//
				.text("fonction").examples("Chef de projet")//
				.text("raisonSociale").french().examples("Orange SA")//
				.date("du").examples("2016-01-23")//
				.date("au").examples("2016-03-07")//
				.text("localisation").examples("Paris")//
				.text("description")//
				.close()//

				.object("indispos").array()//
				.timestamp("debut").examples("2016-07-01T14:00:00.000Z")//
				.timestamp("fin").examples("2016-07-18T23:00:00.000Z")//
				.bool("probablement")//
				.text("note").french().examples("En vacances.")//
				.close()//

				.build();
	}

	static ObjectNode buildExpertiseSchema() {
		return SchemaBuilder3.builder("expertise") //
				.stash("fonctions")//
				.stash("outils")//
				.stash("secteurs")//
				.stash("methodes")//
				.build();
	}

	private ObjectNode buildOpportuniteSchema() {
		return SchemaBuilder3.builder("opportunite") //
				.build();
	}
}
