/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Iterator;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.services.AdminResourceTest.ClientAccount;
import io.spacedog.services.UserResourceTest.ClientUser;

public class ChauffeLeTest extends Assert {

	private static ClientAccount adminAccount;
	private static ClientUser lui;
	private static ClientUser elle;
	private static ClientUser laCopine;

	@BeforeClass
	public static void resetBackend() throws Exception {

		adminAccount = AdminResourceTest.resetAccount("chauffele", "admin", "hi admin", "david@spacedog.io");

		SchemaResourceTest.resetSchema("bigpost", buildBigPostSchema().toString(), "admin", "hi admin");
		SchemaResourceTest.resetSchema("smallpost", buildSmallPostSchema().toString(), "admin", "hi admin");

		lui = UserResourceTest.createUser(adminAccount.backendKey, "lui", "hi lui", "lui@chauffe.le");
		elle = UserResourceTest.createUser(adminAccount.backendKey, "elle", "hi elle", "elle@chauffe.le");
		laCopine = UserResourceTest.createUser(adminAccount.backendKey, "la copine", "hi la copine",
				"lacopine@chauffe.le");

		SpaceRequest.refresh(adminAccount);
	}

	static ObjectNode buildBigPostSchema() {
		return SchemaBuilder.builder("bigpost") //
				.property("title", "text").language("french").required().end() //
				.objectProperty("responses").array() //
				.property("title", "text").language("french").required().end() //
				.property("author", "string").required().end() //
				.end() //
				.build();
	}

	static ObjectNode buildSmallPostSchema() {
		return SchemaBuilder.builder("smallpost") //
				.property("title", "text").language("french").required().end() //
				.property("parent", "string").end()//
				.build();
	}

	@Test
	public void shouldSucceedTestingBigPostChauffeLe() throws Exception {
		chauffeLe(new BigPost());
	}

	@Test
	public void shouldSucceedTestingSmallPostChauffeLe() throws Exception {
		chauffeLe(new SmallPost());
	}

	public void chauffeLe(ChauffeLeEngine impl) throws Exception {

		String threadId = impl.createSubject("je suis partie en mission en argentine", lui);
		impl.addComment(threadId, "tu connais ?", lui);
		impl.addComment(threadId, "hein, heu nan je ne parle pas espagnol", elle);
		impl.addComment(threadId, "un pays génial, à la base, j'étais juste partie qq mois pour bosser", lui);
		impl.addComment(threadId, "et puis finalement je suis resté un an", lui);
		impl.addComment(threadId, "ha ouais.", elle);

		threadId = impl.createSubject("ALORS ??? ton RDV TINDER ??", laCopine);
		impl.addComment(threadId, "il tourne autour du pot", elle);
		impl.addComment(threadId, "CHAUFFE LE !!!!!!!!!!", laCopine);

		threadId = impl.createSubject("j'ai traversé la pampa", lui);
		impl.addComment(threadId, "j'ai même été boire du café en Colombie, hein", lui);
		impl.addComment(threadId, "mais que du café ;-)", lui);
		impl.addComment(threadId, "hein hein", elle);
		impl.addComment(threadId, "j'ai vu de la végétation, des arbres immenses...", lui);

		threadId = impl.createSubject("CHAUFFE LE !!!!!!", laCopine);

		threadId = impl.createSubject("et, euh, le plus fort", lui);
		impl.addComment(threadId, "t'as des dauphins roses", lui);
		impl.addComment(threadId, "nan", elle);

		impl.showWall();
	}

	public interface ChauffeLeEngine {
		String createSubject(String subject, ClientUser user) throws Exception;

		void addComment(String threadId, String comment, ClientUser user) throws Exception;

		Iterator<JsonNode> showWall() throws Exception;
	}

	public static class BigPost implements ChauffeLeEngine {

		@Override
		public String createSubject(String subject, ClientUser user) throws Exception {

			String bigPost = Json.startObject().put("title", subject) //
					.startArray("responses") //
					.end()//
					.build().toString();

			return SpaceRequest.post("/v1/data/bigpost").backendKey(adminAccount).basicAuth(user).body(bigPost).go(201)
					.objectNode().get("id").asText();
		}

		@Override
		public void addComment(String postId, String comment, ClientUser user) throws Exception {

			ObjectNode bigPost = SpaceRequest.get("/v1/data/bigpost/{id}").backendKey(adminAccount)
					.routeParam("id", postId).go(200).objectNode();

			((ArrayNode) bigPost.get("responses"))
					.add(Json.startObject().put("title", comment).put("author", user.username).build());

			SpaceRequest.put("/v1/data/bigpost/{id}").backendKey(adminAccount).routeParam("id", postId).basicAuth(user)
					.body(bigPost.toString()).go(200);
		}

		@Override
		public Iterator<JsonNode> showWall() throws Exception {

			SpaceRequest.refresh(adminAccount);

			String wallQuery = Json.startObject()//
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
					.startObject("match_all")//
					.build().toString();

			return SpaceRequest.post("/v1/data/bigpost/search").backendKey(adminAccount).body(wallQuery).go(200)
					.jsonNode().get("results").elements();
		}
	}

	public static class SmallPost implements ChauffeLeEngine {

		@Override
		public String createSubject(String subject, ClientUser user) throws Exception {

			String smallPost = Json.startObject().put("title", subject).build().toString();

			return SpaceRequest.post("/v1/data/smallpost").backendKey(adminAccount).basicAuth(user).body(smallPost)
					.go(201).objectNode().get("id").asText();
		}

		@Override
		public void addComment(String parentId, String comment, ClientUser user) throws Exception {

			String smallPost = Json.startObject().put("title", comment)//
					.put("parent", parentId)//
					.build().toString();

			SpaceRequest.post("/v1/data/smallpost").backendKey(adminAccount).basicAuth(user).body(smallPost).go(201);
		}

		@Override
		public Iterator<JsonNode> showWall() throws Exception {

			SpaceRequest.refresh(adminAccount);

			String subjectQuery = Json.startObject()//
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
					.startObject("not")//
					.startObject("exists")//
					.put("field", "parent")//
					.build().toString();

			JsonNode subjectResults = SpaceRequest.post("/v1/data/smallpost/search").backendKey(adminAccount)
					.body(subjectQuery).go(200).jsonNode();

			JsonBuilder<ObjectNode> responsesQuery = Json.startObject()//
					.put("from", 0)//
					.put("size", 10)//
					.startArray("sort")//
					.add("parent")//
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
					.startObject("terms")//
					.startArray("parent");

			Iterator<JsonNode> subjects = subjectResults.get("results").elements();

			while (subjects.hasNext())
				responsesQuery.add(subjects.next().get("meta").get("id").asText());

			SpaceRequest.post("/v1/data/smallpost/search").backendKey(adminAccount)
					.body(responsesQuery.build().toString()).go(200);

			return subjectResults.get("results").elements();
		}
	}
}
