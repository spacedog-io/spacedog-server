/**
 * © David Attias 2015
 */
package io.spacedog.examples;

import org.junit.Test;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceTarget;
import io.spacedog.utils.DataPermission;
import io.spacedog.utils.JsonGenerator;
import io.spacedog.utils.Schema;

public class Caremen extends SpaceClient {

	static final Backend DEV = new Backend(//
			"caredev", "caredev", "hi caredev", "david@spacedog.io");

	private Backend backend;
	private JsonGenerator generator;

	public Caremen() {
		generator = new JsonGenerator();
	}

	@Test
	public void initCaremenBackend() {

		backend = DEV;
		SpaceRequest.configuration().target(SpaceTarget.production);

		// resetBackend(backend);
		// initInstallations();
		// initCourses();
		// initDrivers();
		// initUsers();
	}

	void initUsers() {
		SpaceClient.newCredentials(backend, "flavien", "hi flavien", "flavien.dibello@in-tact.fr");
		User marcel = SpaceClient.newCredentials(backend, "marcel", "hi marcel", "marcel@caremen.com");
		SpaceRequest.put("/1/credentials/" + marcel.id + "/roles/driver").adminAuth(backend).go(200);
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

		SpaceRequest.post("/1/data/driver?id=robert")//
				.adminAuth(backend).body(generator.gen(schema, 0)).go(201);
	}

	static Schema buildDriverSchema() {
		return Schema.builder("driver") //

				.acl("key", DataPermission.read_all)//
				.acl("driver", DataPermission.create, DataPermission.read, DataPermission.update)//
				.acl("admin", DataPermission.create, DataPermission.search, DataPermission.update_all,
						DataPermission.delete_all)//

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
}
