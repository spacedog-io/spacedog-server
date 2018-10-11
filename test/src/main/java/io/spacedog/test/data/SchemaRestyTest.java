/**
 * © David Attias 2015
 */
package io.spacedog.test.data;

import java.util.Map;

import org.junit.Test;

import com.google.common.collect.Maps;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.Roles;
import io.spacedog.client.data.DataSettings;
import io.spacedog.client.schema.Schema;
import io.spacedog.test.SpaceTest;
import io.spacedog.utils.Json;

public class SchemaRestyTest extends SpaceTest {

	@Test
	public void deletePutAndGetSchemas() {

		// prepare
		prepareTest();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearServer();
		SpaceDog bob = createTempDog(superadmin, "bob");

		// anonymous gets all backend schema
		// if no schema returns empty object
		guest.schemas().getAll().isEmpty();

		// admin creates car, home and sale schemas
		superadmin.schemas().set(buildCarSchema());
		superadmin.schemas().set(buildHomeSchema());
		superadmin.schemas().set(buildSaleSchema());

		// anonymous gets car, home and sale schemas
		assertEquals(buildCarSchema().enhance(), guest.schemas().get("car"));
		assertEquals(buildHomeSchema().enhance(), guest.schemas().get("home"));
		assertEquals(buildSaleSchema().enhance(), guest.schemas().get("sale"));

		// anonymous gets all schemas
		Map<String, Schema> schemas = Maps.newHashMap();
		schemas.put("car", buildCarSchema().enhance());
		schemas.put("home", buildHomeSchema().enhance());
		schemas.put("sale", buildSaleSchema().enhance());
		assertEquals(schemas, guest.schemas().getAll());

		// anonymous is not allowed to delete schema
		assertHttpError(401, () -> guest.schemas().delete("sale"));

		// user is not allowed to delete schema
		assertHttpError(403, () -> bob.schemas().delete("sale"));

		// superadmin faisl to delete a non existing schema
		assertHttpError(404, () -> superadmin.schemas().delete("XXX"));

		// admin deletes a schema and all its objects
		superadmin.delete("sale");

		// admin fails to create an invalid schema
		assertHttpError(400, () -> superadmin.schemas().set(//
				Schema.builder("toto").property("content", "XXX").build()));

		// superadmin fails to update car schema color field
		// since field type change is not allowed
		Schema schema = buildCarSchema();
		schema.mapping().with("car").with("properties")//
				.with("color").put("type", "date");
		assertHttpError(400, () -> superadmin.schemas().set(schema));

		// superadmin fails to remove car schema color field
		// since removing fields is not allowed
		// carSchema.mapping().with("car").with("properties").remove("color");
		// assertHttpError(400, () -> superadmin.schemas().set(carSchema));
	}

	private static Schema buildHomeSchema() {
		return Schema.builder("home") //
				.keyword("type")//
				.keyword("phone")//
				.geopoint("location")//

				.object("address") //
				.integer("number")//
				.text("street")//
				.keyword("city")//
				.keyword("country")//
				.closeObject()//
				.build();

	}

	public static Schema buildSaleSchema() {
		return Schema.builder("sale") //
				.keyword("number") //
				.timestamp("when") //
				.geopoint("where") //
				.bool("online")//
				.date("deliveryDate") //
				.time("deliveryTime")//

				.object("items") //
				.keyword("ref")//
				.text("description").english()//
				.integer("quantity")//
				.keyword("type")//
				.closeObject()//
				.build();
	}

	public static Schema buildCarSchema() {
		return Schema.builder("car") //
				.keyword("serialNumber")//
				.date("buyDate")//
				.time("buyTime")//
				.timestamp("buyTimestamp") //
				.keyword("color")//
				.bool("techChecked") //
				.geopoint("location") //

				.object("model")//
				.text("description").french()//
				.integer("fiscalPower")//
				.floatt("size")//
				.closeObject()//
				.build();
	}

	// @Test
	// public void saveMetaDataInSchema() {
	//
	// // prepare
	// prepareTest();
	// SpaceDog superadmin = clearRootBackend();
	//
	// Schema schemaClient = Schema.builder("home") //
	// .extra("global-scope", true)//
	// .enumm("type").required().extra("global-scope", false)//
	// .object("address").required().extra("global-scope", false)//
	// .text("street").required().extra("global-scope", false) //
	// .keyword("owner").required().refType("user")//
	// .build();
	//
	// superadmin.schemas().set(schemaClient);
	//
	// // superadmin gets the schema from backend
	// // and check it is unchanged
	// Schema schemaServer = superadmin.schemas().get(schemaClient.name());
	// assertEquals(schemaClient, schemaServer);
	// }

	@Test
	public void testStashField() {

		// prepare
		prepareTest();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearServer();

		// superadmin creates document schema
		Schema schema = Schema.builder("document").stash("data").build();
		superadmin.schemas().set(schema);

		// superadmin sets acl of document schema
		DataSettings settings = new DataSettings();
		settings.acl().put("document", Roles.all, Permission.create);
		superadmin.data().settings(settings);

		// guest saves a first document with data as an object
		guest.data().save("document", //
				Json.object("data", Json.object("a", "aaa", "b", Json.object("b", "bbb"))));

		// guest saves a second document with data as an array
		guest.data().save("document", Json.object("data", Json.array("a", Json.array(1, 2, 3))));

		// guest saves a thirst document with data as a boolean
		guest.data().save("document", Json.object("data", true));

		// document schema contains no fields but "data" stash field
		guest.post("/2/data/document").bodyJson("a", "aaa").go(400);
	}
}
