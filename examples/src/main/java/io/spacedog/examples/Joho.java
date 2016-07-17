/**
 * © David Attias 2015
 */
package io.spacedog.examples;

import java.net.URL;
import java.util.Iterator;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.Resources;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.SchemaBuilder2;

public class Joho extends SpaceClient {

	public final static Backend JOHO2 = new Backend("joho2", "joho", "hi joho", "david@spacedog.io");
	public final static Backend JOHORECETTE = new Backend("johorecette", "johorecette", "hi johorecette",
			"david@spacedog.io");

	private static Backend backend = JOHO2;

	static ObjectNode buildDiscussionSchema() {
		return SchemaBuilder2.builder("discussion") //
				.textProperty("title", "french", true) //
				.textProperty("description", "french", true) //
				.startObjectProperty("theme", true)//
				.textProperty("name", "french", true)//
				.textProperty("description", "french", true)//
				.stringProperty("code", true)//
				.endObjectProperty()//
				.startObjectProperty("category", true)//
				.textProperty("name", "french", true)//
				.textProperty("description", "french", true)//
				.stringProperty("code", true)//
				.endObjectProperty()//
				.startObjectProperty("author", true)//
				.textProperty("firstname", "french", true)//
				.textProperty("lastname", "french", true)//
				.stringProperty("avatar", true)//
				.stringProperty("job", true)//
				.endObjectProperty()//
				.startObjectProperty("lastMessage", true)//
				.textProperty("text", "french", true)//
				.startObjectProperty("author", true)//
				.textProperty("firstname", "french", true)//
				.textProperty("lastname", "french", true)//
				.stringProperty("avatar", true)//
				.stringProperty("job", true)//
				.build();
	}

	static ObjectNode buildMessageSchema() {
		return SchemaBuilder2.builder("message") //
				.textProperty("text", "french", true)//
				.stringProperty("discussionId", true)//
				.startObjectProperty("author", true)//
				.textProperty("firstname", "french", true)//
				.textProperty("lastname", "french", true)//
				.stringProperty("avatar", true)//
				.stringProperty("job", true)//
				.endObjectProperty()//
				.startObjectProperty("category", true)//
				.textProperty("name", "french", true)//
				.textProperty("description", "french", true)//
				.stringProperty("code", true)//
				.endObjectProperty()//
				.startObjectProperty("responses", true, true)//
				.textProperty("text", "french", true)//
				.startObjectProperty("author", true)//
				.textProperty("firstname", "french", true)//
				.textProperty("lastname", "french", true)//
				.stringProperty("avatar", true)//
				.stringProperty("job", true)//
				.endObjectProperty()//
				.endObjectProperty()//
				.build();
	}

	static ObjectNode buildCustomUserSchema() {
		return SchemaBuilder2.builder("user", "username")//
				.stringProperty("username", true)//
				.stringProperty("email", true)//
				.textProperty("firstname", "french", true)//
				.textProperty("lastname", "french", true)//
				.simpleProperty("job", "enum", true, false)//
				.simpleProperty("service", "enum", true, false)//
				.startObjectProperty("site", true)//
				.textProperty("name", "french", true)//
				.stringProperty("address1", true)//
				.stringProperty("address2", false)//
				.stringProperty("town", true)//
				.stringProperty("zipcode", true)//
				.simpleProperty("where", "geopoint", true)//
				.stringProperty("code", true)//
				.endObjectProperty()//
				.stringProperty("mobile", true)//
				.stringProperty("fixed", true)//
				.stringProperty("avatar", true)//
				.build();
	}

	static ObjectNode buildThemesSchema() {
		return SchemaBuilder2.builder("themes")//
				.startObjectProperty("themes", true, true)//
				.textProperty("name", "french", true)//
				.textProperty("description", "french", true)//
				.stringProperty("code", true)//
				.startObjectProperty("categories", true, true)//
				.textProperty("name", "french", true)//
				.textProperty("description", "french", true)//
				.stringProperty("code", true)//
				.endObjectProperty()//
				.endObjectProperty()//
				.build();
	}

	static ObjectNode buildSitesSchema() {
		return SchemaBuilder2.builder("sites")//
				.startObjectProperty("sites", true, true)//
				.textProperty("name", "french", true)//
				.stringProperty("address1", true)//
				.stringProperty("address2", false)//
				.stringProperty("town", true)//
				.stringProperty("zipcode", true)//
				.simpleProperty("where", "geopoint", true)//
				.stringProperty("code", true)//
				.build();
	}

	@Test
	public void createJoho2InstallationIndex() throws Exception {
		SpaceRequest.delete("/1/schema/installation").adminAuth(backend).go(200, 404);
		SpaceRequest.put("/1/schema/installation").adminAuth(backend).go(201);
	}

	@Test
	public void initAndFillJohoBackend() throws Exception {

		// resetAccount(johoAccount);

		setSchema(buildDiscussionSchema(), backend);
		setSchema(buildMessageSchema(), backend);
		setSchema(buildCustomUserSchema(), backend);
		setSchema(buildThemesSchema(), backend);
		setSchema(buildSitesSchema(), backend);

		createThemes();
		createSites();
	}

	public void createResponse(String messageId, String text, User user) throws Exception {
		JsonBuilder<ObjectNode> message = Json.objectBuilder().object("responses").put("text", text)//
				.object("author").put("firstname", user.username).put("lastname", user.email);
		SpaceRequest.put("/1/data/message/" + messageId).userAuth(user).body(message).go(200);
	}

	public User createUser(Backend backend, String username, String password, String email, String firstname,
			String lastname, String job, String town, String serviceName, String serviceCode, double lat, double lon,
			String mobile, String fixed, String avatarUrl) throws Exception {

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

	private void createThemes() throws Exception {
		URL url = Resources.getResource("io/spacedog/examples/joho.themes.json");
		JsonNode themes = Json.readNode(url);
		SpaceRequest.post("/1/data/themes").adminAuth(backend).body(themes).go(201);
	}

	private void createSites() throws Exception {
		URL url = Resources.getResource("io/spacedog/examples/joho.sites.json");
		JsonNode sites = Json.readNode(url);
		SpaceRequest.post("/1/data/sites").adminAuth(backend).body(sites).go(201);
	}

	public String createDiscussion(String title, String categoryCode, User user) throws Exception {

		JsonBuilder<ObjectNode> discussion = Json.objectBuilder().put("title", title)//
				.put("description", title).object("category").put("code", categoryCode);
		return SpaceRequest.post("/1/data/discussion").userAuth(user).body(discussion).go(201)//
				.objectNode().get("id").asText();
	}

	public String createMessage(String discussionId, String text, User user) throws Exception {
		return SpaceRequest.post("/1/data/message").userAuth(user)//
				.body("text", text, "discussionId", discussionId)//
				.go(201).objectNode().get("id").asText();
	}

	public Iterator<JsonNode> showWall() throws Exception {

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
}
