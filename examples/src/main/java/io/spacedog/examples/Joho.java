/**
 * © David Attias 2015
 */
package io.spacedog.examples;

import java.net.URL;
import java.util.Iterator;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.DataPermission;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.Schema;
import io.spacedog.utils.Schema.SchemaAclSettings;
import io.spacedog.utils.SchemaSettings;

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
				.add(buildSitesSchema());

		SchemaAclSettings schemaAcl = new SchemaAclSettings();
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

	void createResponse(String messageId, String text, User user) {
		JsonBuilder<ObjectNode> message = Json.objectBuilder().object("responses").put("text", text)//
				.object("author").put("firstname", user.username).put("lastname", user.email);
		SpaceRequest.put("/1/data/message/" + messageId).userAuth(user).body(message).go(200);
	}

	User createUser(Backend backend, String username, String password, String email, String firstname, String lastname,
			String job, String town, String serviceName, String serviceCode, double lat, double lon, String mobile,
			String fixed, String avatarUrl) {

		ObjectNode user = Json.objectBuilder().put("username", username)//
				.put("password", password)//
				.put("email", email)//
				.put("firstname", firstname)//
				.put("lastname", lastname)//
				.put("job", job)//
				.put("mobile", mobile)//
				.put("fixed", fixed)//
				.put("avatar", avatarUrl)//
				.object("site")//
				.put("name", serviceName)//
				.put("town", town)//
				.put("code", serviceCode)//
				.object("where")//
				.put("lat", lat)//
				.put("lon", lon)//
				.build();

		String id = SpaceRequest.post("/1/user/").backend(backend).body(user).go(201).objectNode().get("id").asText();

		return new User(backend.backendId, id, username, password, email);
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

	String createDiscussion(String title, String categoryCode, User user) {

		JsonBuilder<ObjectNode> discussion = Json.objectBuilder().put("title", title)//
				.put("description", title).object("category").put("code", categoryCode);
		return SpaceRequest.post("/1/data/discussion").userAuth(user).body(discussion).go(201)//
				.objectNode().get("id").asText();
	}

	String createMessage(String discussionId, String text, User user) {
		return SpaceRequest.post("/1/data/message").userAuth(user)//
				.body("text", text, "discussionId", discussionId)//
				.go(201).objectNode().get("id").asText();
	}

	Iterator<JsonNode> showWall() {

		JsonBuilder<ObjectNode> discussionQuery = Json.objectBuilder()//
				.put("from", 0)//
				.put("size", 10)//
				.array("sort")//
				.object()//
				.object("meta.updatedAt")//
				.put("order", "asc")//
				.end()//
				.end()//
				.end()//
				.object("query")//
				.object("match_all");

		JsonNode subjectResults = SpaceRequest.post("/1/search/discussion").refresh().backend(backend)
				.body(discussionQuery).go(200).jsonNode();

		Iterator<JsonNode> discussions = subjectResults.get("results").elements();

		while (discussions.hasNext()) {

			JsonBuilder<ObjectNode> messagesQuery = Json.objectBuilder()//
					.put("from", 0)//
					.put("size", 10)//
					.array("sort")//
					.object()//
					.object("meta.updatedAt")//
					.put("order", "asc")//
					.end()//
					.end()//
					.end()//
					.object("query")//
					.object("filtered")//
					.object("query")//
					.object("match_all")//
					.end()//
					.end()//
					.object("filter")//
					.object("term")//
					.put("discussionId", discussions.next().get("meta").get("id").asText());

			SpaceRequest.post("/1/search/message").backend(backend).body(messagesQuery).go(200);
		}

		return discussions;
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
}
