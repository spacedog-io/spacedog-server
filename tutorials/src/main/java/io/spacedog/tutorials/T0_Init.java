package io.spacedog.tutorials;

import java.net.URL;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.io.Resources;

import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.Roles;
import io.spacedog.client.data.DataSettings;
import io.spacedog.client.push.Installation;
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
		superadmin().schemas().set(schema);

		DataSettings dataSettings = new DataSettings();
		dataSettings.acl().put("course", Roles.user, Permission.create, //
				Permission.updateMine, Permission.readMine);
		superadmin().settings().save(dataSettings);
	}

	public void createCustomerSchema() {

		Schema schema = loadSchemaFromFile("customer.schema.json");
		superadmin().schemas().set(schema);

		DataSettings dataSettings = new DataSettings();
		dataSettings.acl().put("customer", Roles.user, Permission.create, //
				Permission.updateMine, Permission.readMine);
		superadmin().settings().save(dataSettings);
	}

	public void createInstallationSchema() {

		superadmin().schemas().setDefault(Installation.TYPE);

		DataSettings dataSettings = new DataSettings();
		dataSettings.acl().put(Installation.TYPE, Roles.user, Permission.create, //
				Permission.updateMine, Permission.readMine, Permission.deleteMine);
		superadmin().settings().save(dataSettings);
	}

	public void createDriverSchema() {

		Schema schema = loadSchemaFromFile("driver.schema.json");
		superadmin().schemas().set(schema);

		DataSettings dataSettings = new DataSettings();
		dataSettings.acl().put("driver", Roles.admin, Permission.create, Permission.search, //
				Permission.update, Permission.delete);
		dataSettings.acl().put("driver", "driver", Permission.create, //
				Permission.readMine, Permission.updateMine);
		superadmin().settings().save(dataSettings);
	}

	private Schema loadSchemaFromFile(String fileName) {
		URL url = Resources.getResource(getClass(), fileName);
		JsonNode node = Json.readNode(url);
		return new Schema(Utils.splitByDot(fileName)[0], //
				Json.checkObject(node));
	}

}
