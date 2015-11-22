/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.services.AdminResourceTest.ClientAccount;
import io.spacedog.services.UserResourceTest.ClientUser;

public class JohoTest extends Assert {

	private static ClientAccount johoAccount;

	private static ClientUser fred;
	private static ClientUser maelle;
	private static ClientUser vincent;

	static ObjectNode buildDiscussionSchema() {
		return SchemaBuilder.builder("discussion") //
				.property("title", "text").language("french").required().end() //
				.property("description", "text").language("french").required().end() //
				.build();
	}

	static ObjectNode buildMessageSchema() {
		return SchemaBuilder.builder("message") //
				.property("text", "text").language("french").required().end() //
				.property("discussionId", "string").end()//
				.build();
	}

	static ObjectNode buildCustomUserSchema() {
		return SchemaBuilder.builder("customuser") //
				.property("firstname", "text").language("french").required().end() //
				.property("lastname", "text").language("french").required().end() //
				.property("job", "enum").required().end() //
				.property("town", "text").language("french").required().end() //
				.property("serviceId", "string").required().end() //
				.property("mobile", "string").required().end() //
				.property("fixed", "string").required().end() //
				.property("avatar", "string").required().end() //
				.build();
	}

	@Test
	public void initAndFillJohoBackend() throws Exception {

		johoAccount = AdminResourceTest.resetAccount("joho", "joho", "hi joho", "david@spacedog.io");

		SchemaResourceTest.resetSchema("discussion", buildDiscussionSchema().toString(), "joho", "hi joho");
		SchemaResourceTest.resetSchema("message", buildMessageSchema().toString(), "joho", "hi joho");
		SchemaResourceTest.resetSchema("customuser", buildCustomUserSchema().toString(), "joho", "hi joho");

		fred = UserResourceTest.createUser(johoAccount.backendKey, "fred", "hi fred", "hello@spacedog.io");
		maelle = UserResourceTest.createUser(johoAccount.backendKey, "maelle", "hi maelle", "hello@spacedog.io");
		vincent = UserResourceTest.createUser(johoAccount.backendKey, "vincent", "hi vincent", "hello@spacedog.io");

		SpaceRequest.refresh(johoAccount);

		String threadId = createDiscussion("je suis partie en mission en argentine", fred);
		createMessage(threadId, "tu connais ?", fred);
		createMessage(threadId, "hein, heu nan je ne parle pas espagnol", maelle);
		createMessage(threadId, "un pays génial, à la base, j'étais juste partie qq mois pour bosser", fred);
		createMessage(threadId, "et puis finalement je suis resté un an", fred);
		createMessage(threadId, "ha ouais.", maelle);

		threadId = createDiscussion("ALORS ??? ton RDV TINDER ??", vincent);
		createMessage(threadId, "il tourne autour du pot", maelle);
		createMessage(threadId, "CHAUFFE LE !!!!!!!!!!", vincent);

		threadId = createDiscussion("j'ai traversé la pampa", fred);
		createMessage(threadId, "j'ai même été boire du café en Colombie, hein", fred);
		createMessage(threadId, "mais que du café ;-)", fred);
		createMessage(threadId, "hein hein", maelle);
		createMessage(threadId, "j'ai vu de la végétation, des arbres immenses...", fred);

		threadId = createDiscussion("CHAUFFE LE !!!!!!", vincent);

		threadId = createDiscussion("et, euh, le plus fort", fred);
		createMessage(threadId, "t'as des dauphins roses", fred);
		createMessage(threadId, "nan", maelle);

		showWall();
	}

	public String createDiscussion(String title, ClientUser user) throws Exception {

		JsonBuilder<ObjectNode> discussion = Json.startObject().put("title", title).put("description", title);

		return SpaceRequest.post("/v1/data/discussion").backendKey(johoAccount).basicAuth(user).body(discussion).go(201)
				.objectNode().get("id").asText();
	}

	public void createMessage(String discussionId, String text, ClientUser user) throws Exception {

		JsonBuilder<ObjectNode> message = Json.startObject().put("text", text)//
				.put("discussionId", discussionId);

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
