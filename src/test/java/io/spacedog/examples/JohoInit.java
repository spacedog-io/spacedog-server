/**
 * © David Attias 2015
 */
package io.spacedog.examples;

import java.net.URL;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.Resources;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceDogHelper.User;
import io.spacedog.client.SpaceRequest;
import io.spacedog.services.Json;
import io.spacedog.services.JsonBuilder;
import io.spacedog.services.SchemaBuilder2;
import io.spacedog.services.UserResource;

public class JohoInit extends Assert {

	private static final String BACKEND_ID = "joho2";

	private static final String ADMIN_PASSWORD = "hi joho";

	private static final String ADMIN_USERNAME = "joho";

	private static SpaceDogHelper.Account johoAccount;

	private static SpaceDogHelper.User fred;
	private static SpaceDogHelper.User maelle;
	private static SpaceDogHelper.User vincent;

	static ObjectNode buildDiscussionSchema() {
		return SchemaBuilder2.builder("discussion") //
				.textProperty("title", "french", true) //
				.textProperty("description", "french", false) //
				.startObjectProperty("theme", false)//
				.textProperty("name", "french", false)//
				.textProperty("description", "french", false)//
				.stringProperty("code", false)//
				.endObjectProperty()//
				.startObjectProperty("category", true)//
				.textProperty("name", "french", false)//
				.textProperty("description", "french", false)//
				.stringProperty("code", true)//
				.endObjectProperty()//
				.startObjectProperty("author", false)//
				.textProperty("firsname", "french", false)//
				.textProperty("lastsname", "french", false)//
				.stringProperty("username", false)//
				.stringProperty("avatar", false)//
				.stringProperty("job", false)//
				.endObjectProperty()//
				.build();
	}

	static ObjectNode buildMessageSchema() {
		return SchemaBuilder2.builder("message") //
				.textProperty("text", "french", true)//
				.stringProperty("discussionId", true)//
				.build();
	}

	static ObjectNode buildCustomUserSchema() {
		return UserResource.getDefaultUserSchemaBuilder() //
				.textProperty("firstname", "french", true)//
				.textProperty("lastname", "french", true)//
				.simpleProperty("job", "enum", true, false)//
				.stringProperty("town", true)//
				.startObjectProperty("service", true)//
				.textProperty("name", "french", true)//
				.stringProperty("code", true)//
				.simpleProperty("where", "geopoint", true)//
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

	static ObjectNode buildServicesSchema() {
		return SchemaBuilder2.builder("services")//
				.startObjectProperty("services", true, true)//
				.textProperty("name", "french", true)//
				.stringProperty("code", true)//
				.simpleProperty("where", "geopoint", true)//
				.build();
	}

	@Test
	public void initAndFillJohoBackend() throws Exception {

		// johoAccount = SpaceDogHelper.resetAccount(BACKEND_ID, ADMIN_USERNAME,
		// ADMIN_PASSWORD, "david@spacedog.io");
		johoAccount = SpaceDogHelper.getAccount(BACKEND_ID, ADMIN_USERNAME, ADMIN_PASSWORD);

		SpaceDogHelper.resetSchema("discussion", buildDiscussionSchema(), johoAccount);
		SpaceDogHelper.resetSchema("message", buildMessageSchema(), johoAccount);
		SpaceDogHelper.resetSchema("user", buildCustomUserSchema(), johoAccount);
		SpaceDogHelper.resetSchema("themes", buildThemesSchema(), johoAccount);
		SpaceDogHelper.resetSchema("services", buildServicesSchema(), johoAccount);

		SpaceDogHelper.deleteUser("fred", johoAccount);
		fred = createUser(johoAccount.backendKey, "fred", "hi fred", "frederic.falliere@in-tact.fr", "Frédéric",
				"Fallière", "Lead développeur", "Paris", "in-tact", "INTACT", 44.9, 2.4, "06 67 68 69 70",
				"01 22 33 44 55", "http://offbeat.topix.com/pximg/KJUP13O61TTML7P3.jpg");
		SpaceDogHelper.deleteUser("maelle", johoAccount);
		maelle = createUser(johoAccount.backendKey, "maelle", "hi maelle", "maelle.lepape@in-tact.fr", "Maëlle",
				"Le Pape", "Développeur", "Paris", "in-tact", "INTACT", 44.9, 2.4, "06 67 68 69 70", "01 22 33 44 55",
				"http://static.lexpress.fr/medias_10179/w_640,h_358,c_fill,g_center/v1423758015/le-pape-francois-le-12-fevrier-2015-a-l-ouverture-d-un-consistoire-sur-la-reforme-de-la-curie_5212121.jpg");
		SpaceDogHelper.deleteUser("vincent", johoAccount);
		vincent = createUser(johoAccount.backendKey, "vincent", "hi vincent", "vincent.miramond@in-tact.fr", "Vincent",
				"Miramond", "Directeur", "Paris", "in-tact", "INTACT", 44.9, 2.4, "06 67 68 69 70", "01 22 33 44 55",
				"http://www.t83.fr/infos/wp-content/uploads/2015/08/Fred-01-gros-nez-620x658.jpg");

		SpaceRequest.refresh(johoAccount);

		createThemes();
		createServices();

		String threadId = createDiscussion("je suis partie en mission en argentine", "RH", fred);
		createMessage(threadId, "tu connais ?", fred);
		createMessage(threadId, "hein, heu nan je ne parle pas espagnol", maelle);
		createMessage(threadId, "un pays génial, à la base, j'étais juste partie qq mois pour bosser", fred);
		createMessage(threadId, "et puis finalement je suis resté un an", fred);
		createMessage(threadId, "ha ouais.", maelle);

		threadId = createDiscussion("ALORS ??? ton RDV TINDER ??", "RH", vincent);
		createMessage(threadId, "il tourne autour du pot", maelle);
		createMessage(threadId, "CHAUFFE LE !!!!!!!!!!", vincent);

		threadId = createDiscussion("j'ai traversé la pampa", "RH", fred);
		createMessage(threadId, "j'ai même été boire du café en Colombie, hein", fred);
		createMessage(threadId, "mais que du café ;-)", fred);
		createMessage(threadId, "hein hein", maelle);
		createMessage(threadId, "j'ai vu de la végétation, des arbres immenses...", fred);

		threadId = createDiscussion("CHAUFFE LE !!!!!!", "RH", vincent);

		threadId = createDiscussion("et, euh, le plus fort", "RH", fred);
		createMessage(threadId, "t'as des dauphins roses", fred);
		createMessage(threadId, "nan", maelle);

		showWall();
	}

	private User createUser(String backendKey, String username, String password, String email, String firstname,
			String lastname, String job, String town, String serviceName, String serviceCode, double lat, double lon,
			String mobile, String fixed, String avatarUrl) throws Exception {

		ObjectNode user = Json.startObject().put("username", username)//
				.put("password", password)//
				.put("email", email)//
				.put("firstname", firstname)//
				.put("lastname", lastname)//
				.put("town", town)//
				.put("job", job)//
				.put("mobile", mobile)//
				.put("fixed", fixed)//
				.put("avatar", avatarUrl)//
				.startObject("service")//
				.put("name", serviceName)//
				.put("code", serviceCode)//
				.startObject("where")//
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

	private void createServices() throws Exception {
		URL url = Resources.getResource("io/spacedog/examples/joho.services.json");
		JsonNode services = Json.getMapper().readTree(url);
		SpaceRequest.post("/v1/data/services").basicAuth(johoAccount).body(services).go(201);
	}

	public String createDiscussion(String title, String categoryCode, SpaceDogHelper.User user) throws Exception {

		JsonBuilder<ObjectNode> discussion = Json.startObject().put("title", title)//
				.put("description", title).startObject("category").put("code", categoryCode);
		return SpaceRequest.post("/v1/data/discussion").backendKey(johoAccount).basicAuth(user).body(discussion).go(201)
				.objectNode().get("id").asText();
	}

	public void createMessage(String discussionId, String text, SpaceDogHelper.User user) throws Exception {

		JsonBuilder<ObjectNode> message = Json.startObject().put("text", text).put("discussionId", discussionId);
		SpaceRequest.post("/v1/data/message").backendKey(johoAccount).basicAuth(user).body(message).go(201);
	}

	public Iterator<JsonNode> showWall() throws Exception {

		SpaceRequest.refresh(johoAccount);

		JsonBuilder<ObjectNode> discussionQuery = Json.startObject()//
				.put("from", 0)//
				.put("size", 10)//
				.startArray("sort")//
				.startObject()//
				.startObject("meta.updatedAt")//
				.put("order", "asc")//
				.end()//
				.end()//
				.end()//
				.startObject("query")//
				.startObject("match_all");

		JsonNode subjectResults = SpaceRequest.post("/v1/data/discussion/search").backendKey(johoAccount)
				.body(discussionQuery).go(200).jsonNode();

		Iterator<JsonNode> discussions = subjectResults.get("results").elements();

		while (discussions.hasNext()) {

			JsonBuilder<ObjectNode> messagesQuery = Json.startObject()//
					.put("from", 0)//
					.put("size", 10)//
					.startArray("sort")//
					.startObject()//
					.startObject("meta.updatedAt")//
					.put("order", "asc")//
					.end()//
					.end()//
					.end()//
					.startObject("query")//
					.startObject("filtered")//
					.startObject("query")//
					.startObject("match_all")//
					.end()//
					.end()//
					.startObject("filter")//
					.startObject("term")//
					.put("discussionId", discussions.next().get("meta").get("id").asText());

			SpaceRequest.post("/v1/data/message/search").backendKey(johoAccount).body(messagesQuery).go(200);
		}

		return discussions;
	}
}
