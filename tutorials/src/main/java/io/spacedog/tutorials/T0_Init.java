package io.spacedog.tutorials;

import java.net.URL;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.io.Resources;

import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.Roles;
import io.spacedog.client.schema.Schema;
import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;

public class T0_Init extends DemoBase {

	@Test
	public void superdogInitsBackend() {

		// superdog clears backend

		superdog().post("/1/admin/clear").go(200);

		// superdof creates backend superadmin

		superdog().credentials().create("superadmin", "hi superadmin", //
				"platform@spacedog.io", Roles.superadmin);

		// superadmin creates all backend schemas

		createCourseSchema();
		createCustomerSchema();
		createInstallationSchema();
		createDriverSchema();
	}

	public void createCourseSchema() {

		Schema schema = loadSchemaFromFile("course.schema.json");
		schema.acl(Roles.user, Permission.create, //
				Permission.updateMine, Permission.readMine);
		superadmin().schemas().set(schema);
	}

	public void createCustomerSchema() {

		Schema schema = loadSchemaFromFile("customer.schema.json");
		schema.acl(Roles.user, Permission.create, //
				Permission.updateMine, Permission.readMine);
		superadmin().schemas().set(schema);
	}

	public void createInstallationSchema() {

		superadmin().schemas().setDefault("installation");

		Schema schema = superadmin().schemas().get("installation");
		schema.acl(Roles.user, Permission.create, //
				Permission.updateMine, Permission.readMine, Permission.deleteMine);
		superadmin().schemas().set(schema);
	}

	public void createDriverSchema() {

		Schema schema = loadSchemaFromFile("driver.schema.json");

		schema.acl(Roles.admin, Permission.create, Permission.search, //
				Permission.update, Permission.delete);
		schema.acl("driver", Permission.create, //
				Permission.readMine, Permission.updateMine);

		superadmin().schemas().set(schema);
	}

	private Schema loadSchemaFromFile(String fileName) {
		URL url = Resources.getResource(getClass(), fileName);
		JsonNode node = Json.readNode(url);
		return new Schema(Utils.splitByDot(fileName)[0], //
				Json.checkObject(node));
	}

}
