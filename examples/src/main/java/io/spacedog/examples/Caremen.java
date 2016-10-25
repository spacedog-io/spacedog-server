/**
 * © David Attias 2015
 */
package io.spacedog.examples;

import java.util.Optional;
import java.util.Random;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceTarget;
import io.spacedog.utils.DataPermission;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonGenerator;
import io.spacedog.utils.Schema;

public class Caremen extends SpaceClient {

	static final Backend DEV = new Backend(//
			"caredev", "caredev", "hi caredev", "david@spacedog.io");

	private Backend backend;
	private JsonGenerator generator = new JsonGenerator();
	private Random random = new Random();

	@Test
	public void initCaremenBackend() {

		backend = DEV;
		SpaceRequest.configuration().target(SpaceTarget.production);

		// resetBackend(backend);
		// initInstallations();
		// initVehiculeTypes();

		// setSchema(buildCourseSchema(), backend);
		// setSchema(buildDriverSchema(), backend);
		// setSchema(buildCustomerSchema(), backend);
		// setSchema(buildDriverLogSchema(), backend);

		// createCustomers();
		// createDrivers();
	}

	void createCustomers() {
		Schema schema = buildCustomerSchema();

		createCustomer("flavien", schema);
		createCustomer("aurelien", schema);
		createCustomer("david", schema);
		createCustomer("philippe", schema);
	}

	User createCustomer(String username, Schema schema) {
		String password = "hi " + username;
		String email = "david@spacedog.io";

		Optional<User> optional = SpaceClient.login(backend.backendId, username, password, 200, 401);

		User credentials = optional.isPresent() ? optional.get()
				: SpaceClient.newCredentials(backend, username, password, email);

		SpaceRequest.put("/1/credentials/" + credentials.id + "/roles/customer")//
				.adminAuth(backend).go(200);

		ObjectNode customer = generator.gen(schema, 0);
		customer.put("credentialsId", credentials.id);
		customer.put("firstname", credentials.username);
		customer.put("lastname", credentials.username.charAt(0) + ".");
		customer.put("phone", "0033662627520");
		SpaceRequest.post("/1/data/customer").userAuth(credentials).body(customer).go(201);

		return credentials;
	}

	static Schema buildCustomerSchema() {
		return Schema.builder("customer") //

				.acl("customer", DataPermission.create, DataPermission.search, //
						DataPermission.update)//
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

				.acl("customer", DataPermission.create, DataPermission.read, //
						DataPermission.search, DataPermission.update)//
				.acl("driver", DataPermission.search, DataPermission.update_all)//
				.acl("admin", DataPermission.create, DataPermission.search, //
						DataPermission.update_all, DataPermission.delete_all)//

				.string("status").examples("requested") //
				.string("customerId").examples("david") //
				.string("requestedVehiculeType").examples("classic") //
				.timestamp("requestedPickupTimestamp").examples("2016-07-12T14:00:00.000Z") //
				.timestamp("pickupTimestamp").examples("2016-07-12T14:00:00.000Z") //
				.timestamp("dropoffTimestamp").examples("2016-07-12T14:00:00.000Z") //
				.floatt("fare").examples(23.82)// in euros
				.longg("time").examples(1234567)// in millis
				.integer("distance").examples(12345)// in meters

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
				.close()//

				.close()//
				.build();
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
				: SpaceClient.newCredentials(backend, username, password, email);

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

				.acl("customer", DataPermission.search)//
				.acl("driver", DataPermission.search, DataPermission.update_all)//
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

	static Schema buildDriverLogSchema() {
		return Schema.builder("driverlog") //

				.acl("driver", DataPermission.create)//
				.acl("admin", DataPermission.create, DataPermission.search, //
						DataPermission.delete_all)//

				.string("driverId").examples("robert")//
				.geopoint("where")//

				.object("course")//
				.string("id").examples("robert")//
				.string("status").examples("accepted", "waiting")//
				.string("statusUpdatedTo").examples("accepted", "waiting")//
				.string("statusUpdatedFrom").examples("requested", "accepted")//
				.close()//

				.close()//
				.build();
	}

	void initVehiculeTypes() {

		ArrayNode node = Json.arrayBuilder()//
				.object()//
				.put("type", "classic")//
				.put("name", "Berline Classic")//
				.put("description", "Standard")//
				.put("minimumPrice", 10)//
				.put("passengers", 4)//
				.end()//

				.object()//
				.put("type", "premium")//
				.put("name", "Berline Premium")//
				.put("description", "Haut de gamme")//
				.put("minimumPrice", 15)//
				.put("passengers", 4)//
				.end()//

				.object()//
				.put("type", "green")//
				.put("name", "GREEN BERLINE")//
				.put("description", "Electric cars")//
				.put("minimumPrice", 15)//
				.put("passengers", 4)//
				.end()//

				.object()//
				.put("type", "break")//
				.put("name", "BREAK")//
				.put("description", "Grand coffre")//
				.put("minimumPrice", 15)//
				.put("passengers", 4)//
				.end()//

				.object()//
				.put("type", "van")//
				.put("name", "VAN")//
				.put("description", "Mini bus")//
				.put("minimumPrice", 15)//
				.put("passengers", 6)//
				.end()//

				.build();

		SpaceRequest.put("/1/settings/carTypes")//
				.adminAuth(backend).body(node).go(201);
	}
}
