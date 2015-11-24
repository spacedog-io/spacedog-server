/**
 * © David Attias 2015
 */
package io.spacedog;

import java.net.URL;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.Resources;

import io.spacedog.services.AdminResourceTest;
import io.spacedog.services.AdminResourceTest.ClientAccount;
import io.spacedog.services.Json;
import io.spacedog.services.JsonBuilder;
import io.spacedog.services.SchemaBuilder2;
import io.spacedog.services.SchemaResourceTest;
import io.spacedog.services.SpaceRequest;
import io.spacedog.services.UserResourceTest;
import io.spacedog.services.UserResourceTest.ClientUser;

public class JohoInit extends Assert {

	private static final String BACKEND_ID = "joho2";

	private static final String ADMIN_PASSWORD = "hi joho";

	private static final String ADMIN_USERNAME = "joho";

	private static ClientAccount johoAccount;

	private static ClientUser fred;
	private static ClientUser maelle;
	private static ClientUser vincent;

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
		return SchemaBuilder2.builder("customuser") //
				.textProperty("firstname", "french", true)//
				.textProperty("lastname", "french", true)//
				.simpleProperty("job", "enum", true, false)//
				.textProperty("town", "french", true)//
				.stringProperty("service", true)//
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

	@Test
	public void initAndFillJohoBackend() throws Exception {

		johoAccount = AdminResourceTest.resetAccount(BACKEND_ID, ADMIN_USERNAME, ADMIN_PASSWORD, "david@spacedog.io");
		// johoAccount = AdminResourceTest.getAccount(BACKEND_ID,
		// ADMIN_USERNAME, ADMIN_PASSWORD);

		SchemaResourceTest.resetSchema("discussion", buildDiscussionSchema(), johoAccount);
		SchemaResourceTest.resetSchema("message", buildMessageSchema(), johoAccount);
		SchemaResourceTest.resetSchema("customuser", buildCustomUserSchema(), johoAccount);
		SchemaResourceTest.resetSchema("themes", buildThemesSchema(), johoAccount);

		UserResourceTest.deleteUser("fred", johoAccount);
		fred = UserResourceTest.createUser(johoAccount.backendKey, "fred", "hi fred", "hello@spacedog.io");
		UserResourceTest.deleteUser("maelle", johoAccount);
		maelle = UserResourceTest.createUser(johoAccount.backendKey, "maelle", "hi maelle", "hello@spacedog.io");
		UserResourceTest.deleteUser("vincent", johoAccount);
		vincent = UserResourceTest.createUser(johoAccount.backendKey, "vincent", "hi vincent", "hello@spacedog.io");

		SpaceRequest.refresh(johoAccount);

		createThemes();

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

	private void createThemes() throws Exception {
		URL themesUrl = Resources.getResource("io/spacedog/services/joho.themes.json");
		JsonNode themes = Json.getMapper().readTree(themesUrl);
		SpaceRequest.post("/v1/data/themes").basicAuth(johoAccount).body(themes).go(201);
	}

	// public void createTheme(String name, String code) throws Exception {
	// JsonBuilder<ObjectNode> theme = Json.startObject().put("name",
	// name).put("code", code);
	// SpaceRequest.post("/v1/data/theme").basicAuth(johoAccount).body(theme).go(201);
	// }
	//
	// public void createCategory(String name, String code, String theme) throws
	// Exception {
	// JsonBuilder<ObjectNode> category = Json.startObject().put("name",
	// name).put("code", code).put("theme",
	// "/theme/" + theme);
	// SpaceRequest.post("/v1/data/category").basicAuth(johoAccount).body(category).go(201);
	// }

	public String createDiscussion(String title, String categoryCode, ClientUser user) throws Exception {

		JsonBuilder<ObjectNode> discussion = Json.startObject().put("title", title)//
				.put("description", title).startObject("category").put("code", categoryCode);
		return SpaceRequest.post("/v1/data/discussion").backendKey(johoAccount).basicAuth(user).body(discussion).go(201)
				.objectNode().get("id").asText();
	}

	public void createMessage(String discussionId, String text, ClientUser user) throws Exception {

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
