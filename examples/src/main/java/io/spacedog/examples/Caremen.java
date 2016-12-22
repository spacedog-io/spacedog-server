/**
 * © David Attias 2015
 */
package io.spacedog.examples;

import java.util.Collections;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceTarget;
import io.spacedog.utils.DataPermission;
import io.spacedog.utils.Json;
import io.spacedog.utils.MailTemplate;
import io.spacedog.utils.Schema;
import io.spacedog.utils.SettingsSettings;
import io.spacedog.utils.SettingsSettings.SettingsAcl;
import io.spacedog.utils.StripeSettings;

public class Caremen extends SpaceClient {

	static final Backend DEV = new Backend(//
			"caredev", "caredev", "hi caredev", "david@spacedog.io");

	static final Backend RECETTE = new Backend(//
			"carerec", "carerec", "hi carerec", "david@spacedog.io");

	static final Backend PRODUCTION = new Backend(//
			"caremen", "caremen", "hi caremen", "david@spacedog.io");

	private Backend backend;

	@Test
	public void initCaremenBackend() throws JsonProcessingException {

		backend = DEV;
		SpaceRequest.configuration().target(SpaceTarget.production);

		// resetBackend(backend);
		// initInstallations();
		// initVehiculeTypes();
		// initStripeSettings();
		// initMailTemplates();
		// initFareSettings();
		// initAppConfigurationSettings();
		// initReferences();
		// initSettingsSettings();

		// setSchema(buildCourseSchema(), backend);
		// setSchema(buildDriverSchema(), backend);
		// setSchema(buildCustomerSchema(), backend);
		// setSchema(buildCourseLogSchema(), backend);
		// setSchema(buildCustomerCompanySchema(), backend);
		// setSchema(buildCompanySchema(), backend);

		// createOperators();
		// createCashier();
		// createRobots();
	}

	//
	// Settings
	//

	void initMailTemplates() throws JsonProcessingException {
		MailTemplate template = new MailTemplate();
		template.to = Lists.newArrayList("{{to}}");
		template.subject = "Vous pouvez maintenant payer vos courses Caremen sur le compte de {{company.name}}";
		template.text = "coucou";
		template.model = Maps.newHashMap();
		template.model.put("to", "string");
		template.model.put("company", "company");
		template.roles = Collections.singleton("operator");

		SpaceRequest.put("/1/mail/template/notif_customer_company").adminAuth(backend)//
				.body(Json.mapper().writeValueAsString(template)).go(200);
	}

	void initStripeSettings() {
		StripeSettings settings = new StripeSettings();
		settings.secretKey = SpaceRequest.configuration().testStripeSecretKey();
		settings.rolesAllowedToCharge = Sets.newHashSet("cashier");
		SpaceClient.saveSettings(backend, settings);
	}

	void initFareSettings() {
		ObjectNode classic = Json.object("base", 3, "minimum", 8, "km", 2, "min", 0.45);
		ObjectNode premium = Json.object("base", 5, "minimum", 15, "km", 2, "min", 0.45);
		ObjectNode green = Json.object("base", 5, "minimum", 35, "km", 2, "min", 0.45);
		ObjectNode breakk = Json.object("base", 5, "minimum", 8, "km", 2, "min", 0.45);
		ObjectNode van = Json.object("base", 5, "minimum", 20, "km", 2, "min", 0.45);
		ObjectNode settings = Json.object("classic", classic, "premium", premium, //
				"green", green, "break", breakk, "van", van, "driverShare", 0.82f);
		SpaceRequest.put("/1/settings/fare").adminAuth(backend).body(settings).go(200, 201);
	}

	void initAppConfigurationSettings() {
		ObjectNode settings = Json.object("driverAverageSpeed", 15, //
				"driverAverageSpeedKmPerHour", 15, //
				"courseLogInterval", 20, //
				"courseLogIntervalMeters", 100, //
				"customerWaitingForDriverMaxDurationMinutes", 2, //
				"operatorRefreshTimeout", 30, //
				"operatorRefreshTimeoutSeconds", 30);
		SpaceRequest.put("/1/settings/appcustomer").adminAuth(backend).body(settings).go(200, 201);
		SpaceRequest.put("/1/settings/appconfiguration").adminAuth(backend).body(settings).go(200, 201);
	}

	void initSettingsSettings() {
		SettingsSettings settings = new SettingsSettings();

		// appcustomer settings
		SettingsAcl acl = new SettingsAcl();
		acl.read("key", "user");
		settings.put("appcustomer", acl);

		// appconfiguration settings
		acl = new SettingsAcl();
		acl.read("key", "user", "admin");
		settings.put("appconfiguration", acl);

		// fare settings
		acl = new SettingsAcl();
		acl.read("operator", "cashier", "user");
		acl.update("operator");
		settings.put("fare", acl);

		// references settings
		acl = new SettingsAcl();
		acl.read("key", "user", "operator");
		settings.put("references", acl);

		SpaceClient.saveSettings(backend, settings);
	}

	void initVehiculeTypes() {

		ObjectNode node = Json.objectBuilder()//
				.object("classic")//
				.put("type", "classic")//
				.put("name", "Berline Classic")//
				.put("description", "Standard")//
				.put("minimumPrice", 10)//
				.put("passengers", 4)//
				.end()//

				.object("premium")//
				.put("type", "premium")//
				.put("name", "Berline Premium")//
				.put("description", "Haut de gamme")//
				.put("minimumPrice", 15)//
				.put("passengers", 4)//
				.end()//

				.object("green")//
				.put("type", "green")//
				.put("name", "GREEN BERLINE")//
				.put("description", "Electric cars")//
				.put("minimumPrice", 15)//
				.put("passengers", 4)//
				.end()//

				.object("break")//
				.put("type", "break")//
				.put("name", "BREAK")//
				.put("description", "Grand coffre")//
				.put("minimumPrice", 15)//
				.put("passengers", 4)//
				.end()//

				.object("van")//
				.put("type", "van")//
				.put("name", "VAN")//
				.put("description", "Mini bus")//
				.put("minimumPrice", 15)//
				.put("passengers", 6)//
				.end()//

				.build();

		SpaceRequest.put("/1/settings/vehiculetypes")//
				.adminAuth(backend).body(node).go(201, 200);
	}

	void initReferences() {

		ObjectNode node = Json.objectBuilder()//

				.node("courseStatuses", //
						Json.array("new-immediate", "driver-is-coming", "ready-to-load", "in-progress", "completed",
								"cancelled", "new-scheduled", "scheduled-assigned", "billed", "unbilled"))//

				.node("driverStatuses", //
						Json.array("working", "not-working", "disabled"))//

				.node("roles", //
						Json.array("key", "user", "customer", "driver", "operator", "admin"))//

				.node("vehiculeTypes", //
						Json.object("classic", "Classic Berline", //
								"premium", "Premium Berline", "green", "Green Berline", //
								"break", "Breack", "van", "Van"))//

				.build();

		SpaceRequest.put("/1/settings/references")//
				.adminAuth(backend).body(node).go(201, 200);
	}

	//
	// Schemas
	//

	static Schema buildCustomerSchema() {
		return Schema.builder("customer") //

				.acl("user", DataPermission.create, DataPermission.search, //
						DataPermission.update)//
				.acl("operator", DataPermission.search)//
				.acl("admin", DataPermission.search, DataPermission.update_all, //
						DataPermission.delete_all)//

				.string("credentialsId").examples("khljgGFJHfvlkHMhjh")//
				.string("firstname").examples("Robert")//
				.string("lastname").examples("Morgan")//
				.string("phone").examples("+ 33 6 42 01 67 56")//

				.object("billing")//
				.text("name").french().examples("In-tact SARL")//
				.text("street").french().examples("9 rue Titon")//
				.string("zipcode").examples("75011")//
				.text("town").examples("Paris")//
				.close()//

				.close()//
				.build();
	}

	static Schema buildCustomerCompanySchema() {
		return Schema.builder("customercompany") //

				.acl("user", DataPermission.read_all)//
				.acl("operator", DataPermission.create, DataPermission.delete_all, //
						DataPermission.search)//
				.acl("admin", DataPermission.create, DataPermission.update_all, //
						DataPermission.delete_all, DataPermission.search)//

				.string("companyId")//
				.string("companyName")//
				.build();
	}

	void initInstallations() {
		SpaceRequest.delete("/1/schema/installation").adminAuth(backend).go(200, 404);
		SpaceRequest.put("/1/schema/installation").adminAuth(backend).go(201);
		Schema schema = SpaceClient.getSchema("installation", backend);
		schema.acl("key", DataPermission.create, DataPermission.read, DataPermission.update, DataPermission.delete);
		schema.acl("user", DataPermission.create, DataPermission.read, DataPermission.update, DataPermission.delete);
		schema.acl("admin", DataPermission.search, DataPermission.update_all, DataPermission.delete_all);
		SpaceClient.setSchema(schema, backend);
	}

	static Schema buildCourseSchema() {
		return Schema.builder("course") //

				.acl("user", DataPermission.create, DataPermission.read, //
						DataPermission.search, DataPermission.update)//
				.acl("cashier", DataPermission.search, DataPermission.update_all)//
				.acl("driver", DataPermission.search, DataPermission.update_all)//
				.acl("operator", DataPermission.search, DataPermission.update_all)//
				.acl("admin", DataPermission.create, DataPermission.search, //
						DataPermission.update_all, DataPermission.delete_all)//

				.string("status") //
				.string("requestedVehiculeType").examples("classic") //
				.timestamp("requestedPickupTimestamp").examples("2016-07-12T14:00:00.000Z") //
				.timestamp("pickupTimestamp").examples("2016-07-12T14:00:00.000Z") //
				.timestamp("dropoffTimestamp").examples("2016-07-12T14:00:00.000Z") //
				.text("noteForDriver")//
				.floatt("fare").examples(23.82)// in euros
				.longg("time").examples(1234567)// in millis
				.integer("distance").examples(12345)// in meters

				.string("customerId")//
				.object("customer")//
				.string("id")//
				.string("credentialsId")//
				.string("firstname").examples("Robert")//
				.string("lastname").examples("Morgan")//
				.string("phone").examples("+ 33 6 42 01 67 56")//
				.close()

				.object("payment")//
				.string("companyId")//
				.string("companyName")//

				.object("stripe")//
				.string("customerId")//
				.string("cardId")//
				.string("paymentId")//
				.close()//

				.close()//

				.object("from")//
				.text("address").french()//
				.geopoint("geopoint")//
				.close()//

				.object("to")//
				.text("address").french()//
				.geopoint("geopoint")//
				.close()//

				.object("driver")//
				.string("driverId").examples("robert")//
				.string("credentialsId")//
				.floatt("gain").examples("10.23")//
				.string("firstname").examples("Robert")//
				.string("lastname").examples("Morgan")//
				.string("phone").examples("+ 33 6 42 01 67 56")//
				.string("photo")
				.examples("http://s3-eu-west-1.amazonaws.com/spacedog-artefact/SpaceDog-Logo-Transp-130px.png")//

				.object("vehicule")//
				.string("type").examples("classic")//
				.string("brand").examples("Peugeot", "Renault")//
				.string("model").examples("508", "Laguna", "Talisman")//
				.string("color").examples("black", "white", "pink")//
				.string("licencePlate").examples("BM-500-FG")//
				.close()//

				.close()//
				.build();
	}

	static Schema buildDriverSchema() {
		return Schema.builder("driver") //

				.acl("user", DataPermission.search)//
				.acl("driver", DataPermission.search, DataPermission.update_all)//
				.acl("operator", DataPermission.create, DataPermission.search, //
						DataPermission.update_all)//
				.acl("admin", DataPermission.create, DataPermission.search, DataPermission.update_all,
						DataPermission.delete_all)//

				.string("credentialsId")//
				.string("status")//
				.text("firstname").french()//
				.text("lastname").french()//
				.text("homeAddress").french()//
				.string("phone")//
				.string("photo")//

				.object("lastLocation")//
				.geopoint("where")//
				.timestamp("when")//
				.close()

				.object("vehicule")//
				.string("type")//
				.text("brand").french()//
				.text("model").french()//
				.text("color").french()//
				.string("licencePlate")//
				.close()//

				.object("RIB")//
				.text("bankName").french().examples("Société Générale")//
				.string("bankCode").examples("SOGEFRPP")//
				.string("accountIBAN").examples("FR568768757657657689")//
				.close()//

				.close()//
				.build();
	}

	static Schema buildCourseLogSchema() {
		return Schema.builder("courselog") //

				.acl("driver", DataPermission.create)//
				.acl("cashier", DataPermission.search)//
				.acl("admin", DataPermission.create, DataPermission.search, //
						DataPermission.delete_all)//

				.string("courseId")//
				.string("driverId")//
				.string("status")//
				.geopoint("where")//

				.close()//
				.build();
	}

	Schema buildCompanySchema() {
		return Schema.builder("company") //

				.acl("operator", DataPermission.create, DataPermission.search, //
						DataPermission.update_all)//
				.acl("admin", DataPermission.create, DataPermission.search, //
						DataPermission.update_all, DataPermission.delete_all)//

				.string("status")//
				.string("vatId")//
				.text("name").french()//
				.text("address").french()//
				.string("phone")//
				.string("email")//
				.bool("active")//

				.object("contact")//
				.text("firstname").french()//
				.text("lastname").french()//
				.text("role").french()//
				.string("phone")//
				.string("email")//

				.close()//
				.build();
	}

	//
	// Special users
	//

	void createOperators() {

		createOperator("nico", "nicola.lonzi@gmail.com");
		createOperator("dimitri", "dimitri.valax@in-tact.fr");
		createOperator("dave", "david@spacedog.io");
		createOperator("philou", "philippe.rolland@in-tact.fr");
		createOperator("flav", "flavien.dibello@in-tact.fr");

		if (backend == RECETTE) {
			createOperator("manu", "emmanuel@walkthisway.fr");
			createOperator("xav", "xdorange@gmail.com");
			createOperator("auguste", "lcdlocation@wanadoo.fr");
		}
	}

	void createOperator(String username, String email) {
		String password = "hi " + username;

		JsonNode node = SpaceRequest.get("/1/credentials")//
				.adminAuth(backend).queryParam("username", username).go(200)//
				.get("results.0.id");

		if (node == null) {
			User credentials = SpaceClient.createAdminCredentials(//
					backend, username, password, email);
			SpaceRequest.put("/1/credentials/" + credentials.id + "/roles/operator")//
					.adminAuth(backend).go(200);
		}
	}

	private void createRobots() {
		if (backend == PRODUCTION)
			return;

		for (int i = 0; i < 3; i++) {

			String username = "robot-" + i;
			String password = "hi " + username;

			JsonNode node = SpaceRequest.get("/1/credentials")//
					.adminAuth(backend).queryParam("username", username).go(200)//
					.get("results.0.id");

			if (node == null) {
				User robot = SpaceClient.signUp(backend.backendId, username, password);

				SpaceRequest.put("/1/credentials/" + robot.id + "/roles/driver")//
						.adminAuth(backend).go(200);

				SpaceRequest.post("/1/data/driver").adminAuth(backend)//
						.body("status", "not-working", "firstname", "Robot", //
								"lastname", Integer.toString(i), "phone", "0606060606", //
								"homeAddress", "9 rue Titon 75011 Paris", //
								"credentialsId", robot.id, "vehicule", //
								Json.object("brand", "Faucon", "model", "Millenium", //
										"type", "classic", "color", "Métal", //
										"licencePlate", "DA-KISS-ME"))//
						.go(201);
			}
		}
	}

	void createCashier() {
		User cashier = SpaceClient.createCredentials(backend.backendId, //
				"cashier", "hi cashier", "plateform@spacedog.io");

		SpaceRequest.put("/1/credentials/" + cashier.id + "/roles/cashier")//
				.adminAuth(backend).go(200);
	}

}
