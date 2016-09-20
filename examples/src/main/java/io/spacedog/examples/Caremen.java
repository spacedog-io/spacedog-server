/**
 * © David Attias 2015
 */
package io.spacedog.examples;

import java.util.Optional;

import org.elasticsearch.action.support.QuerySourceBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Test;

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

	@Test
	public void initCaremenBackend() {

		backend = DEV;
		SpaceRequest.configuration().target(SpaceTarget.production);

		// resetBackend(backend);
		// initInstallations();
		// initCourses();
		// initDrivers();
		// initPassengers();
		// initCarTypes();

		moveCourseDrivers();
	}

	void initPassengers() {
		SpaceClient.newCredentials(backend, "flavien", "hi flavien", "flavien.dibello@in-tact.fr");
		SpaceClient.newCredentials(backend, "aurelien", "hi aurelien", "aurelien.gustan@in-tact.fr");
		SpaceClient.newCredentials(backend, "david", "hi david", "david.attias@in-tact.fr");
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

	void initCourses() {
		Schema schema = buildCourseSchema();
		resetSchema(schema, backend);

		SpaceRequest.post("/1/data/course?id=home")//
				.adminAuth(backend).body(generator.gen(schema, 0)).go(201);
	}

	static Schema buildCourseSchema() {
		return Schema.builder("course") //

				.acl("key", DataPermission.search)//
				.acl("user", DataPermission.create, DataPermission.read, DataPermission.update)//
				.acl("driver", DataPermission.search, DataPermission.update_all)//
				.acl("admin", DataPermission.create, DataPermission.search, DataPermission.update_all,
						DataPermission.delete_all)//

				.string("status").examples("requested", "accepted") //
				.string("carType").examples("berline", "van", "break")//

				.object("from")//
				.text("address").french().examples("8 rue Titon 75011 Paris") //
				.geopoint("geopoint")//
				.close()//

				.object("to")//
				.text("address").french().examples("8 rue Pierre Dupont 75010 Paris") //
				.geopoint("geopoint")//
				.close()//

				.object("history").array()//
				.string("event").examples("requested", "accepted")//
				.string("by").examples("robert", "marcel")//
				.timestamp("when").examples("2016-07-12T14:00:00.000Z", "2016-08-02T14:00:00.000Z")//
				.close()//

				.object("driver")//
				.string("id").examples("robert")//
				.string("firstname").examples("Robert")//
				.string("lastname").examples("Morgan")//
				.string("phone").examples("+ 33 6 42 01 67 56")//
				.string("photo")
				.examples("http://s3-eu-west-1.amazonaws.com/spacedog-artefact/SpaceDog-Logo-Transp-130px.png")//

				.object("lastLocation")//
				.geopoint("where")//
				.timestamp("when")//
				.close()

				.object("car")//
				.string("type").examples("berline", "van", "break")//
				.string("brand").examples("Peugeot", "Renault")//
				.string("model").examples("508", "Laguna", "Talisman")//
				.string("color").examples("black", "white", "pink")//
				.close()//

				.close()//
				.build();
	}

	void initDrivers() {
		Schema schema = buildDriverSchema();
		resetSchema(schema, backend);

		createDriver("marcel", schema);
		createDriver("gerard", schema);
		createDriver("robert", schema);
	}

	void createDriver(String username, Schema schema) {
		String password = "hi " + username;
		String email = username + "@caremen.com";

		Optional<User> optional = SpaceClient.login(backend.backendId, username, password, 200, 401);

		User credentials = optional.isPresent() ? optional.get() //
				: SpaceClient.newCredentials(backend, username, password, email);

		SpaceRequest.put("/1/credentials/" + credentials.id + "/roles/driver").adminAuth(backend).go(200);

		ObjectNode driver = generator.gen(schema, 0);
		driver.put("credentialsId", credentials.id);
		driver.put("firstname", credentials.username);
		driver.put("lastname", credentials.username.charAt(0) + ".");
		SpaceRequest.post("/1/data/driver").adminAuth(backend).body(driver).go(201);
	}

	static Schema buildDriverSchema() {
		return Schema.builder("driver") //

				.acl("key", DataPermission.read_all)//
				.acl("user", DataPermission.search)//
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

				.object("car")//
				.string("type").examples("berline", "van", "break")//
				.string("brand").examples("Peugeot", "Renault")//
				.string("model").examples("508", "Laguna", "Talisman")//
				.string("color").examples("black", "white", "pink")//
				.close()//

				.object("RIB")//
				.text("bankName").french().examples("Société Générale")//
				.string("bankCode").examples("SOGEFRPP")//
				.string("accountIBAN").examples("FR568768757657657689")//
				.close()//

				.close()//
				.build();
	}

	void initCarTypes() {

		ArrayNode node = Json.arrayBuilder()//
				.object()//
				.put("type", "berline")//
				.put("name", "Berline")//
				.put("description", "Standard")//
				.put("minimumPrice", 10)//
				.put("passengers", 4)//
				.end()//

				.object()//
				.put("type", "premium")//
				.put("name", "BERLINE PREMIUM")//
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

	public void moveCourseDrivers() {

		QuerySourceBuilder source = new QuerySourceBuilder();
		source.setQuery(QueryBuilders.termQuery("status", "accepted"));

		SpaceRequest.post("/1/search/course").adminAuth(backend)//
				.body(source.toString()).size(100).go(200);

		ObjectNode courseUpdate = Json.objectBuilder().object("driver")//
				.object("lastLocation")//
				.object("where")//
				.put("lat", 23.78)//
				.put("lon", 45.89)//
				.end()//
				.end()//
				.end()//
				.build();

		SpaceRequest.put("/1/data/course/kjhgkjhgkjhvhkv").adminAuth(backend)//
				.body(courseUpdate).go(200);
	}

}
