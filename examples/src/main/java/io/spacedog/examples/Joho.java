/**
 * © David Attias 2015
 */
package io.spacedog.examples;

import java.net.URL;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.DataPermission;
import io.spacedog.utils.Json;
import io.spacedog.utils.Schema;
import io.spacedog.utils.SchemaSettings;
import io.spacedog.utils.SchemaSettings.SchemaAcl;

public class Joho extends SpaceClient {

	final static Backend JOHO2 = new Backend("joho2", "joho", "hi joho", "david@spacedog.io");
	final static Backend JOHORECETTE = new Backend("johorecette", "johorecette", "hi johorecette", "david@spacedog.io");

	private Backend backend;

	@Test
	public void updateJohoBackend() {

		backend = JOHO2;

		// SpaceRequest.configuration().target(SpaceTarget.production);

		// setSchemaSettings();

		// setSchema(buildDiscussionSchema(), backend);
		// setSchema(buildMessageSchema(), backend);
		// setSchema(buildCustomUserSchema(), backend);
		// setSchema(buildThemesSchema(), backend);
		// setSchema(buildSitesSchema(), backend);
		// setSchema(buildSondageSchema(), backend);

		// createInstallationSchema();

		// createThemes();
		// createSites();
	}

	void setSchemaSettings() {

		SchemaSettings settings = new SchemaSettings();

		settings.add(buildDiscussionSchema())//
				.add(buildMessageSchema())//
				.add(buildCustomUserSchema())//
				.add(buildThemesSchema())//
				.add(buildSondageSchema())//
				.add(buildSitesSchema());

		SchemaAcl schemaAcl = new SchemaAcl();
		schemaAcl.put("user", Sets.newHashSet(DataPermission.create, //
				DataPermission.read, DataPermission.update, DataPermission.delete));
		schemaAcl.put("admin", Sets.newHashSet(DataPermission.search, //
				DataPermission.update_all, DataPermission.delete_all));
		settings.acl.put("installation", schemaAcl);

		SpaceClient.saveSettings(backend, settings);
	}

	void createInstallationSchema() {
		SpaceRequest.delete("/1/schema/installation").adminAuth(backend).go(200, 404);
		SpaceRequest.put("/1/schema/installation").adminAuth(backend).go(201);
	}

	void createThemes() {
		URL url = Resources.getResource("io/spacedog/examples/joho.themes.json");
		JsonNode themes = Json.readNode(url);
		SpaceRequest.post("/1/data/themes").adminAuth(backend).body(themes).go(201);
	}

	void createSites() {
		URL url = Resources.getResource("io/spacedog/examples/joho.sites.json");
		JsonNode sites = Json.readNode(url);
		SpaceRequest.post("/1/data/sites").adminAuth(backend).body(sites).go(201);
	}

	static Schema buildDiscussionSchema() {
		return Schema.builder("discussion") //

				.acl("user", DataPermission.create, DataPermission.search, //
						DataPermission.update_all, DataPermission.delete)//
				.acl("admin", DataPermission.create, DataPermission.search, //
						DataPermission.update_all, DataPermission.delete_all)//

				.text("title").french() //
				.text("description").french() //

				.object("theme")//
				.text("name").french()//
				.text("description").french()//
				.string("code")//
				.close()//

				.object("category")//
				.text("name").french()//
				.text("description").french()//
				.string("code")//
				.close()//

				.object("author")//
				.text("firstname").french()//
				.text("lastname").french()//
				.string("avatar")//
				.string("job")//
				.close()//

				.object("lastMessage")//
				.text("text").french()//
				.object("author")//
				.text("firstname").french()//
				.text("lastname").french()//
				.string("avatar")//
				.string("job")//
				.build();
	}

	static Schema buildMessageSchema() {
		return Schema.builder("message") //

				.acl("user", DataPermission.create, DataPermission.search, //
						DataPermission.update_all, DataPermission.delete)//
				.acl("admin", DataPermission.create, DataPermission.search, //
						DataPermission.update_all, DataPermission.delete_all)//

				.text("text").french()//
				.string("discussionId")//

				.object("author")//
				.text("firstname").french()//
				.text("lastname").french()//
				.string("avatar")//
				.string("job")//
				.close()//

				.object("category")//
				.text("name").french()//
				.text("description").french()//
				.string("code")//
				.close()//

				.object("responses").array()//
				.text("text").french()//
				.object("author")//
				.text("firstname").french()//
				.text("lastname").french()//
				.string("avatar")//
				.string("job")//
				.close()//
				.close()

				.build();
	}

	static Schema buildCustomUserSchema() {
		return Schema.builder("user")//
				.id("username")//

				.acl("user", DataPermission.create, DataPermission.search, //
						DataPermission.update_all, DataPermission.delete)//
				.acl("admin", DataPermission.create, DataPermission.search, //
						DataPermission.update_all, DataPermission.delete_all)//

				.string("username")//
				.string("email")//
				.text("firstname").french()//
				.text("lastname").french()//
				.enumm("job")//
				.enumm("service")//
				.string("mobile")//
				.string("fixed")//
				.string("avatar")//

				.object("site")//
				.text("name").french()//
				.string("address1")//
				.string("address2")//
				.string("town")//
				.string("zipcode")//
				.geopoint("where")//
				.string("code")//
				.close()//

				.build();
	}

	static Schema buildThemesSchema() {
		return Schema.builder("themes")//

				.acl("user", DataPermission.search)//
				.acl("admin", DataPermission.create, DataPermission.search, //
						DataPermission.update_all, DataPermission.delete_all)//

				.object("themes").array()//
				.text("name").french()//
				.text("description").french()//
				.string("code")//

				.object("categories").array()//
				.text("name").french()//
				.text("description").french()//
				.string("code")//
				.close()//

				.build();
	}

	static Schema buildSitesSchema() {
		return Schema.builder("sites")//

				.acl("user", DataPermission.search)//
				.acl("admin", DataPermission.create, DataPermission.search, //
						DataPermission.update_all, DataPermission.delete_all)//

				.object("sites").array()//
				.text("name").french()//
				.string("address1")//
				.string("address2")//
				.string("town")//
				.string("zipcode")//
				.geopoint("where")//
				.string("code")//
				.build();
	}

	static Schema buildSondageSchema() {
		return Schema.builder("sondage")//

				.acl("user", DataPermission.search, DataPermission.update_all)//
				.acl("admin", DataPermission.create, DataPermission.search, //
						DataPermission.update_all, DataPermission.delete_all)//

				.text("question").french()//
				.string("status")//

				.object("choices").array()//
				.text("title").french()//

				.object("answers").array()//
				.string("userId").array()//
				.close()

				.close()//
				.build();
	}

}
