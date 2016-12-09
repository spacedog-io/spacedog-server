/**
 * © David Attias 2015
 */
package io.spacedog.examples;

import java.util.Collections;
import java.util.Optional;
import java.util.Random;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceTarget;
import io.spacedog.utils.DataPermission;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonGenerator;
import io.spacedog.utils.MailTemplate;
import io.spacedog.utils.Schema;
import io.spacedog.utils.StripeSettings;

public class Caremen extends SpaceClient {

	static final Backend DEV = new Backend(//
			"caredev", "caredev", "hi caredev", "david@spacedog.io");

	private Backend backend;
	private JsonGenerator generator = new JsonGenerator();
	private Random random = new Random();

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
		// initAppCustomerSettings();

		// setSchema(buildCourseSchema(), backend);
		// setSchema(buildDriverSchema(), backend);
		// setSchema(buildCustomerSchema(), backend);
		// setSchema(buildCourseLogSchema(), backend);
		// setSchema(buildCustomerCompanySchema(), backend);
		setSchema(buildCompanySchema(), backend);

		// createDrivers();
		// createOperators();
	}

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
		SpaceClient.saveSettings(backend, settings);
	}

	void initFareSettings() {
		ObjectNode classic = Json.object("base", 3, "minimum", 8, "km", 2, "min", 0.45);
		ObjectNode premium = Json.object("base", 5, "minimum", 15, "km", 2, "min", 0.45);
		ObjectNode green = Json.object("base", 5, "minimum", 35, "km", 2, "min", 0.45);
		ObjectNode breakk = Json.object("base", 5, "minimum", 8, "km", 2, "min", 0.45);
		ObjectNode van = Json.object("base", 5, "minimum", 20, "km", 2, "min", 0.45);
		ObjectNode settings = Json.object("classic", classic, "premium", premium, //
				"green", green, "break", breakk, "van", van);
		SpaceRequest.put("/1/settings/fare").adminAuth(backend).body(settings).go(200, 201);
	}

	void initAppCustomerSettings() {
		ObjectNode settings = Json.object("driverAverageSpeed", 15);
		SpaceRequest.put("/1/settings/appcustomer").adminAuth(backend).body(settings).go(200, 201);
	}

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
				.text("address").french().examples("8 rue Titon 75011 Paris") //
				.geopoint("geopoint")//
				.close()//

				.object("to")//
				.text("address").french().examples("8 rue Pierre Dupont 75010 Paris") //
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

	void createOperators() {

		createOperator("nico", "nicola.lonzi@gmail.com");
		createOperator("dimitri", "dimitri.valax@in-tact.fr");
		createOperator("david", "david@spacedog.io");
		createOperator("philippe", "philippe.rolland@in-tact.fr");
		createOperator("flav", "flavien.dibello@in-tact.fr");
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

	void createDrivers() {
		Schema schema = buildDriverSchema();

		createDriver("marcel", schema);
		createDriver("gerard", schema);
		createDriver("robert", schema);
		createDriver("suzanne", schema);
	}

	void createDriver(String username, Schema schema) {
		String password = "hi " + username;
		String email = (username.startsWith("driver") ? "driver" : username) + "@caremen.com";

		Optional<User> optional = SpaceClient.login(backend.backendId, username, password, 200, 401);

		User credentials = optional.isPresent() ? optional.get() //
				: SpaceClient.signUp(backend, username, password, email);

		SpaceRequest.put("/1/credentials/" + credentials.id + "/roles/driver").adminAuth(backend).go(200);

		ObjectNode driver = generator.gen(schema, 0);
		driver.put("status", "working");
		driver.put("credentialsId", credentials.id);
		driver.put("firstname", credentials.username);
		driver.put("lastname", credentials.username.charAt(0) + ".");
		JsonNode where = Json.object("lat", 48.844 + (random.nextFloat() / 10), //
				"lon", 2.282 + (random.nextFloat() / 10));
		Json.set(driver, "lastLocation.where", where);

		SpaceRequest.post("/1/data/driver").adminAuth(backend).body(driver).go(201);
	}

	static Schema buildDriverSchema() {
		return Schema.builder("driver") //

				.acl("user", DataPermission.search)//
				.acl("driver", DataPermission.search, DataPermission.update_all)//
				.acl("operator", DataPermission.create, DataPermission.search, //
						DataPermission.update_all)//
				.acl("admin", DataPermission.create, DataPermission.search, DataPermission.update_all,
						DataPermission.delete_all)//

				.string("credentialsId").examples("khljgGFJHfvlkHMhjh")//
				.string("status").examples("working")//
				.string("firstname").examples("Robert")//
				.string("lastname").examples("Morgan")//
				.text("homeAddress").french().examples("52 rue Michel Ange 75016 Paris")//
				.string("phone").examples("+ 33 6 42 01 67 56")//
				.string("photo")
				.examples("http://s3-eu-west-1.amazonaws.com/spacedog-artefact/SpaceDog-Logo-Transp-130px.png")//

				.object("lastLocation")//
				.geopoint("where")//
				.timestamp("when")//
				.close()

				.object("vehicule")//
				.string("type").examples("classic")//
				.string("brand").examples("Peugeot", "Renault")//
				.string("model").examples("508", "Laguna", "Talisman")//
				.string("color").examples("black", "white", "pink")//
				.string("licencePlate").examples("BM-500-RF")//
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
				.acl("admin", DataPermission.search, DataPermission.delete_all)//

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
				.text("name")//
				.text("address")//
				.string("phone")//
				.string("email")//
				.bool("active")//

				.object("contact")//
				.text("firstname")//
				.text("lastname")//
				.text("role")//
				.string("phone")//
				.string("email")//

				.close()//
				.build();
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

		SpaceRequest.put("/1/settings/vehiculeTypes")//
				.adminAuth(backend).body(node).go(201);
	}
}
