/**
 * © David Attias 2015
 */
package io.spacedog.examples;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceTarget;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonGenerator;
import io.spacedog.utils.SchemaBuilder3;

public class Colibee extends SpaceClient {

	private ObjectNode schemaConsultant;
	private ObjectNode schemaOpportunite;
	private ObjectNode schemaGroupe;
	private ObjectNode schemaRdv;
	private ObjectNode schemaDiscussion;
	private ObjectNode schemaMessage;

	private User william;
	private Backend backend;
	private JsonGenerator generator;

	public Colibee() throws Exception {
		backend = new Backend("colibee", "colibee", "hi colibee", "david@spacedog.io");
		william = new User("colibee", "william", "william", "hi william", "william@me.com");
		generator = new JsonGenerator();
	}

	@Test
	public void initColibeeBackend() throws Exception {

		SpaceRequest.configuration().target(SpaceTarget.production);

		// resetBackend();

		initGenerator();
		initReferences();
		initConsultantWilliam();
		initOpportunites();
		initGroupeFinance();
		initRdvWilliam();
		initDiscussionBale3();
	}

	@SuppressWarnings("unused")
	private void resetBackend() throws Exception {
		resetBackend(backend);
		initUserDefaultSchema(backend);
		william = createUser(william);
	}

	private void initGenerator() throws Exception {

		// photo

		byte[] bytes = Resources.toByteArray(//
				Resources.getResource("io/spacedog/examples/ma-tronche.png"));

		String url = SpaceRequest.put("/1/share/ma-tronche.png").userAuth(william)//
				.body(bytes).go(200).getFromJson("s3").asText();

		generator.regPath("identite.photo", url);

		// cv

		bytes = Resources.toByteArray(//
				Resources.getResource("io/spacedog/examples/mon-cv.pdf"));

		url = SpaceRequest.put("/1/share/mon-cv.pdf").userAuth(william)//
				.body(bytes).go(200).getFromJson("s3").asText();

		generator.regPath("cv.url", url);
	}

	private void initConsultantWilliam() throws Exception {
		schemaConsultant = buildConsultantSchema();
		resetSchema(schemaConsultant, backend);

		ObjectNode consultantWilliam = generator.gen(schemaConsultant);
		SpaceRequest.post("/1/data/consultant?id=william")//
				.userAuth(william).body(consultantWilliam).go(201);
	}

	private void initOpportunites() throws Exception {
		schemaOpportunite = buildOpportuniteSchema();
		resetSchema(schemaOpportunite, backend);

		SpaceRequest.post("/1/data/opportunite?id=airbus")//
				.userAuth(william).body(generator.gen(schemaOpportunite, 0)).go(201);

		SpaceRequest.post("/1/data/opportunite?id=total")//
				.userAuth(william).body(generator.gen(schemaOpportunite, 1)).go(201);

		SpaceRequest.post("/1/data/opportunite?id=edf")//
				.userAuth(william).body(generator.gen(schemaOpportunite, 2)).go(201);

		SpaceRequest.post("/1/data/opportunite?id=suez")//
				.userAuth(william).body(generator.gen(schemaOpportunite, 3)).go(201);
	}

	private void initGroupeFinance() throws Exception {
		schemaGroupe = buildGroupeSchema();
		resetSchema(schemaGroupe, backend);
		ObjectNode groupeFinance = generator.gen(schemaGroupe);
		SpaceRequest.post("/1/data/groupe?id=finance")//
				.userAuth(william).body(groupeFinance).go(201);
	}

	private void initRdvWilliam() throws Exception {
		schemaRdv = buildRdvSchema();
		resetSchema(schemaRdv, backend);
		ObjectNode rdvWilliam = generator.gen(schemaRdv);
		SpaceRequest.post("/1/data/rdv?id=william")//
				.userAuth(william).body(rdvWilliam).go(201);
	}

	private void initDiscussionBale3() throws Exception {
		schemaDiscussion = buildDiscussionSchema();
		resetSchema(schemaDiscussion, backend);
		ObjectNode bale3 = generator.gen(schemaDiscussion);
		String discussionId = SpaceRequest.post("/1/data/discussion?id=bale3")//
				.userAuth(william).body(bale3).go(201).getFromJson("id").asText();

		schemaMessage = buildMessageSchema();
		resetSchema(schemaMessage, backend);
		ObjectNode message = generator.gen(schemaMessage);
		message.put("discussionId", discussionId);
		SpaceRequest.post("/1/data/message")//
				.userAuth(william).body(message).go(201);
	}

	private void initReferences() throws Exception {
		ObjectNode references = Json.object();

		references.set("secteur", Json.object("asset-management", "Asset Management", //
				"assurance-vie", "Assurance vie", "automobile", "Automobile"));

		references.set("methode", Json.object("agile-scrum", "Agile / SCRUM", "audit", //
				"Audit", "big-data", "Big Data", "cadrage", "Cadrage"));

		references.set("outil", Json.object("ariba", "Ariba", "essbase", "Essbase", //
				"hfm", "HFM", "hp", "HP"));

		references.set("fonction", Json.object("achat-bien-service", "Achats | biens & services", //
				"fsi-acturiat", "FSI | Acturiat", "fsi-derivative", "FSI | Derivatives"));

		references.set("langue", Json.object("fr", "Français", "en", "Anglais", "de", "Allemand"));

		references.set("niveauLangue", Json.object("excellent", "Excellent", "bon", "Bon", "moyen", "Moyen"));

		references.set("nationalite", Json.object("fr", "Française", "en", "Anglaise", "de", "Allemande"));

		references.set("typePrestation",
				Json.object("management-transition", "Management de transition", //
						"conseil-strategie", "Conseil en stratégie", //
						"conseil-management", "Conseil en management", //
						"conseil-système-information", "Conseil en systèmes d'information", //
						"assistance-maitrise-oeuvre", "Assistance à maîtrise d'œuvre"));

		references.set("rythmeMission", Json.object("temps-plein", "Temps plein", //
				"temps-partiel", "Temps partiel"));

		references.set("freqDeplacement", Json.object("aucun", "Aucun", "ponctuel", "Ponctuels", //
				"regulier", "Réguliers"));

		references.set("statutOpportunite", Json.object("identifie", "Identifiée", "ouverte", "Ouverte", //
				"ferme", "Fermée"));

		references.set("resultatOpportunite", Json.object("en-cours", "En cours", "gagnee", "Gagnée", //
				"perdue", "Perdue", "gelee", "Gelée", "annulee", "Annulée"));

		references.set("motifPerdue", Json.object("prix", "Prix", "pas-de-profil", "Pas de profil", //
				"profil-non-retenu", "Profil non retenu"));

		references.set("typeCV", Json.object("chronologique", "CV chronologique", "theme", "CV par thème", //
				"snapshot", "CV snapshot", "autre", "Autre"));

		references.set("zoneMobilite", Json.object("local", "Local", "france", "France", "etranger", "Etranger"));

		references.set("statutCandidature", Json.object("en-cours", "En cours", "ok", "Acceptée", "ko", "Refusée"));

		SpaceRequest.put("/1/stash").adminAuth(backend).go(200);
		SpaceRequest.put("/1/stash/references").adminAuth(backend).body(references).go(200, 201);

		registerReferencesInGenerator(references);
	}

	private void registerReferencesInGenerator(ObjectNode references) {
		Iterator<String> types = references.fieldNames();
		while (types.hasNext()) {
			String type = types.next();
			List<String> values = Lists.newArrayList(references.get(type).fieldNames());
			generator.regType(type, values);
		}
	}

	static ObjectNode buildConsultantSchema() {
		return SchemaBuilder3.builder("consultant") //
				.bool("membreColibee")//
				.string("membreNumero").examples("01234567").labels("fr", "N° de membre")//
				.text("resume")//
				.enumm("typesPrestation").array().enumType("typePrestation")//
				.enumm("secteurs").array().enumType("secteur")//
				.enumm("methodes").array().enumType("methode")//
				.enumm("fonctions").array().enumType("fonction")//
				.enumm("outils").array().enumType("outil")//

				.object("langues").array()//
				.string("langue").enumType("langue")//
				.enumm("niveau").enumType("niveauLangue")//
				.close()//

				.object("identite")//
				.string("prenom").examples("William") //
				.string("nom").examples("Lopez", "Miramond", "Attias") //
				.text("titre").french().examples("Chef de projet")//
				.string("photo")//
				.date("indepDepuisLe")//
				.date("dateDeNaissance") //
				.enumm("nationalite").enumType("nationalite")//
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
				.enumm("zoneMobilite").enumType("zoneMobilite")//
				.enumm("freqDeplacement").enumType("freqDeplacement")//
				.enumm("rythmeTravail").enumType("rythmeMission")//
				.integer("tarifCible").examples(1200)//
				.integer("tarifMin").examples(800)//
				.text("note").examples("J'accepte le temps partiel si nécessaire.")//
				.close()//

				.object("cv").array()//
				.text("titre").french().examples("Directeur de projet", "Auditeur")//
				.date("date").examples("2012-05-12", "2016-05-13")//
				.enumm("type").enumType("typeCV")//
				.text("note").french().examples("CV pour la proposition BNP", "CV pour Total")//
				.string("url")//
				.bool("archive")//
				.close()//

				.object("experiences").array()//
				.text("fonction").examples("Chef de projet", "Chef de produit")//
				.text("raisonSociale").french().examples("Orange SA", "Bull SA")//
				.date("du").examples("2011-01-23", "2013-07-02")//
				.date("au").examples("2013-03-07", "2015-01-23")//
				.text("localisation").examples("Paris", "Puteaux")//
				.text("description")//
				.close()//

				.object("formations").array()//
				.text("titre").french().examples("Diplôme HEC", "Master 2 Pro")//
				.text("ecole").french().examples("HEC", "Université Paris 1 Panthéon-Sorbonne")//
				.text("region").french().examples("Région de Paris, France")//
				.date("du").examples("2005-09-01", "2007-09-01")//
				.date("au").examples("2007-06-30", "2009-06-30")//
				.text("description")//
				.close()//

				.object("dispos").array()//
				.date("debut").examples("2016-07-01", "2016-07-12")//
				.date("fin").examples("2016-07-04", "2016-07-18")//
				.bool("probablement")//
				.text("note").french().examples("En vacances.")//
				.close()//

				.build();
	}

	private ObjectNode buildOpportuniteSchema() {
		return SchemaBuilder3.builder("opportunite") //
				.text("titre").french().examples("Audit financier")//
				.text("contexte").french()//
				.text("objectif").french()//
				.text("contribution").french()//
				.text("commentaires").french()//
				.bool("public")//
				.string("statut").enumType("statutOpportunite")//
				.string("resultat").enumType("resultatOpportunite")//
				.string("typePrestation").array().enumType("typePrestation")//
				.string("secteurActivite").enumType("secteur")//
				.string("fonctions").array().enumType("fonction")//
				.string("methodes").array().enumType("methode")//
				.string("outils").array().enumType("outil")//
				.string("langues").array().enumType("langue")//

				.object("lieu")//
				.text("ville").french().examples("Paris")//
				.string("pays").examples("FR")//
				.geopoint("geopoint")//
				.close()//

				.date("demarreLe").examples("2016-09-10")//
				.date("termineLe").examples("2016-12-24")//
				.string("missionEffectueePar").examples("william")//
				.integer("charge").examples(25)//
				.integer("rythme").examples(3)//
				.string("freqDeplacement").enumType("freqDeplacement")//

				.object("client")//
				.text("raisonSociale").french().examples("Airbus")//
				.text("nom").french().examples("Jérôme")//
				.text("prenom").french().examples("Dupont")//
				.string("tel").examples("+33 6 62 78 34 56")//
				.string("email").examples("jdupont@airbus.com")//
				.close()//

				.object("candidatures").array()//
				.string("par").examples("william", "david", "vincent")//
				.timestamp("le").examples("2016-07-01T14:00:00.000Z", "2016-07-03T14:00:00.000Z")//
				.string("statut").enumType("statutCandidature")//
				.close()//

				.timestamp("identifieeLe").examples("2016-07-01T14:00:00.000Z")//
				.timestamp("ouverteLe").examples("2016-07-01T14:00:00.000Z")//
				.timestamp("fermeeLe").examples("2016-07-01T14:00:00.000Z")//
				.string("motifPerdue").enumType("motifPerdue")//

				.build();
	}

	private ObjectNode buildGroupeSchema() {
		return SchemaBuilder3.builder("groupe") //
				.text("titre").french()//
				.text("description").french()//
				.bool("membreOnly")//
				.string("animateur")//
				.string("consultants").array()//
				.build();

	}

	private ObjectNode buildRdvSchema() {
		return SchemaBuilder3.builder("rdv") //
				.text("titre").french()//
				.string("categorie").examples("Présentation client")//
				.timestamp("debut")//
				.timestamp("fin")//
				.text("lieu")//
				.string("participants").array()//
				.text("commentaires")//
				// animateur = owner of object
				.build();
	}

	private ObjectNode buildDiscussionSchema() {
		return SchemaBuilder3.builder("discussion") //
				.text("titre").french()//
				.string("statut").examples("open", "close")//
				.timestamp("fermeeLe")//
				.string("fermeePar")//
				.build();
	}

	private ObjectNode buildMessageSchema() {
		return SchemaBuilder3.builder("message") //
				.text("texte").french()//
				.string("discussionId")//
				.object("reponses").array()//
				.text("texte").french()//
				.timestamp("ecritLe")//
				.string("ecritPar")//
				.close()//
				.build();
	}
}
