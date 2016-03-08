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

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceRequest;
import io.spacedog.services.UserResource;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.SchemaBuilder2;

public class Joho extends SpaceDogHelper {

	private static final String BACKEND_ID = "joho2";
	private static final String ADMIN_PASSWORD = "hi joho";
	private static final String ADMIN_USERNAME = "joho";

	// private static final String BACKEND_ID = "johorecette";
	// private static final String ADMIN_PASSWORD = "hi johorecette";
	// private static final String ADMIN_USERNAME = "johorecette";

	private static Account johoAccount;

	private static User fred;
	private static User maelle;
	private static User vincent;

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
		return UserResource.getDefaultUserSchemaBuilder() //
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

		SpaceRequest.delete("/v1/schema/installation")//
				.basicAuth(ADMIN_USERNAME, ADMIN_PASSWORD)//
				.go(200, 404);

		ObjectNode installationSchema = SchemaBuilder2.builder("installation")//
				.stringProperty("appId", true)//
				.stringProperty("deviceToken", true)//
				.stringProperty("providerId", true)//
				.stringProperty("userId", true)//
				.startObjectProperty("tags", false)//
				.stringProperty("key", true)//
				.stringProperty("value", true)//
				.endObjectProperty()//
				.build();

		SpaceRequest.put("/v1/schema/installation")//
				.basicAuth(ADMIN_USERNAME, ADMIN_PASSWORD)//
				.body(installationSchema)//
				.go(201);
	}

	@Test
	public void initAndFillJohoBackend() throws Exception {

		// johoAccount = resetAccount(BACKEND_ID, ADMIN_USERNAME,
		// ADMIN_PASSWORD, "david@spacedog.io");
		johoAccount = getOrCreateAccount(BACKEND_ID, ADMIN_USERNAME, ADMIN_PASSWORD, "david@spacedog.io", false);

		setSchema(buildDiscussionSchema(), johoAccount);
		setSchema(buildMessageSchema(), johoAccount);
		setSchema(buildCustomUserSchema(), johoAccount);
		setSchema(buildThemesSchema(), johoAccount);
		setSchema(buildSitesSchema(), johoAccount);

		// deleteUser("fred", johoAccount);
		// fred = createUser(johoAccount.backendKey, "fred", "hi fred",
		// "frederic.falliere@in-tact.fr", "Frédéric",
		// "Fallière", "Lead développeur", "Paris", "in-tact", "INTACT", 44.9,
		// 2.4, "06 67 68 69 70",
		// "01 22 33 44 55",
		// "http://offbeat.topix.com/pximg/KJUP13O61TTML7P3.jpg");
		// deleteUser("maelle", johoAccount);
		// maelle = createUser(johoAccount.backendKey, "maelle", "hi maelle",
		// "maelle.lepape@in-tact.fr", "Maëlle",
		// "Le Pape", "Développeur", "Paris", "in-tact", "INTACT", 44.9, 2.4,
		// "06 67 68 69 70", "01 22 33 44 55",
		// "http://static.lexpress.fr/medias_10179/w_640,h_358,c_fill,g_center/v1423758015/le-pape-francois-le-12-fevrier-2015-a-l-ouverture-d-un-consistoire-sur-la-reforme-de-la-curie_5212121.jpg");
		// deleteUser("vincent", johoAccount);
		// vincent = createUser(johoAccount.backendKey, "vincent", "hi vincent",
		// "vincent.miramond@in-tact.fr", "Vincent",
		// "Miramond", "Directeur", "Paris", "in-tact", "INTACT", 44.9, 2.4, "06
		// 67 68 69 70", "01 22 33 44 55",
		// "http://www.t83.fr/infos/wp-content/uploads/2015/08/Fred-01-gros-nez-620x658.jpg");

		createThemes();
		createSites();

		// String threadId = createDiscussion("je suis partie en mission en
		// argentine", "RH", fred);
		// createMessage(threadId, "tu connais ?", fred);
		// createMessage(threadId, "hein, heu nan je ne parle pas espagnol",
		// maelle);
		// createMessage(threadId, "un pays génial, à la base, j'étais juste
		// partie qq mois pour bosser", fred);
		// createMessage(threadId, "et puis finalement je suis resté un an",
		// fred);
		// createMessage(threadId, "ha ouais.", maelle);
		//
		// threadId = createDiscussion("ALORS ??? ton RDV TINDER ??", "RH",
		// vincent);
		// createMessage(threadId, "il tourne autour du pot", maelle);
		// createMessage(threadId, "CHAUFFE LE !!!!!!!!!!", vincent);
		//
		// threadId = createDiscussion("j'ai traversé la pampa", "RH", fred);
		// createMessage(threadId, "j'ai même été boire du café en Colombie,
		// hein", fred);
		// createMessage(threadId, "mais que du café ;-)", fred);
		// createMessage(threadId, "hein hein", maelle);
		// createMessage(threadId, "j'ai vu de la végétation, des arbres
		// immenses...", fred);
		//
		// threadId = createDiscussion("CHAUFFE LE !!!!!!", "RH", vincent);
		//
		// threadId = createDiscussion("et, euh, le plus fort", "RH", fred);
		// createMessage(threadId, "t'as des dauphins roses", fred);
		// String messageId = createMessage(threadId, "nan", maelle);
		//
		// createResponse(messageId, "c'est quoi ça \"nan\" ?", vincent);
		// createResponse(messageId, "c'est français ?", vincent);
		//
		// showWall();
	}

	private void createResponse(String messageId, String text, User user) throws Exception {
		JsonBuilder<ObjectNode> message = Json.objectBuilder().object("responses").put("text", text)//
				.object("author").put("firstname", user.username).put("lastname", user.email);
		SpaceRequest.put("/v1/data/message/{messageId}").routeParam("messageId", messageId).backendKey(johoAccount)
				.basicAuth(user).body(message).go(200);
	}

	private User createUser(String backendKey, String username, String password, String email, String firstname,
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

		String id = SpaceRequest.post("/v1/user/").backendKey(backendKey).body(user).go(201).objectNode().get("id")
				.asText();

		return new User(id, username, password, email);
	}

	private void createThemes() throws Exception {
		URL url = Resources.getResource("io/spacedog/examples/joho.themes.json");
		JsonNode themes = Json.getMapper().readTree(url);
		SpaceRequest.post("/v1/data/themes").basicAuth(johoAccount).body(themes).go(201);
	}

	private void createSites() throws Exception {
		URL url = Resources.getResource("io/spacedog/examples/joho.sites.json");
		JsonNode sites = Json.getMapper().readTree(url);
		SpaceRequest.post("/v1/data/sites").basicAuth(johoAccount).body(sites).go(201);
	}

	public String createDiscussion(String title, String categoryCode, User user) throws Exception {

		JsonBuilder<ObjectNode> discussion = Json.objectBuilder().put("title", title)//
				.put("description", title).object("category").put("code", categoryCode);
		return SpaceRequest.post("/v1/data/discussion").backendKey(johoAccount).basicAuth(user).body(discussion).go(201)
				.objectNode().get("id").asText();
	}

	public String createMessage(String discussionId, String text, User user) throws Exception {

		JsonBuilder<ObjectNode> message = Json.objectBuilder().put("text", text).put("discussionId", discussionId);
		return SpaceRequest.post("/v1/data/message").backendKey(johoAccount).basicAuth(user).body(message).go(201)
				.objectNode().get("id").asText();
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

		JsonNode subjectResults = SpaceRequest.post("/v1/search/discussion?refresh=true").backendKey(johoAccount)
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

			SpaceRequest.post("/v1/search/message").backendKey(johoAccount).body(messagesQuery).go(200);
		}

		return discussions;
	}
}
