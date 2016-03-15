/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import java.util.Iterator;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.SchemaBuilder;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class ChauffeLeTest extends Assert {

	private static SpaceDogHelper.Backend adminAccount;

	private static SpaceDogHelper.User lui;
	private static SpaceDogHelper.User elle;
	private static SpaceDogHelper.User laCopine;

	@BeforeClass
	public static void resetBackend() throws Exception {

		adminAccount = SpaceDogHelper.resetTestBackend();

		SpaceDogHelper.setSchema(buildBigPostSchema(), adminAccount);
		SpaceDogHelper.setSchema(buildSmallPostSchema(), adminAccount);

		lui = SpaceDogHelper.createUser(adminAccount, "lui", "hi lui", "lui@chauffe.le");
		elle = SpaceDogHelper.createUser(adminAccount, "elle", "hi elle", "elle@chauffe.le");
		laCopine = SpaceDogHelper.createUser(adminAccount, "lacopine", "hi la copine", "lacopine@chauffe.le");
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
	public void chauffeLeWithBigPost() throws Exception {
		SpaceDogHelper.prepareTest();
		chauffeLe(new BigPost());
	}

	@Test
	public void chauffeLeWithSmallPost() throws Exception {
		SpaceDogHelper.prepareTest();
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
		String createSubject(String subject, SpaceDogHelper.User user) throws Exception;

		void addComment(String threadId, String comment, SpaceDogHelper.User user) throws Exception;

		Iterator<JsonNode> showWall() throws Exception;
	}

	public static class BigPost implements ChauffeLeEngine {

		@Override
		public String createSubject(String subject, SpaceDogHelper.User user) throws Exception {

			String bigPost = Json.objectBuilder().put("title", subject) //
					.array("responses") //
					.end()//
					.build().toString();

			return SpaceRequest.post("/v1/data/bigpost").backend(adminAccount).basicAuth(user).body(bigPost).go(201)
					.objectNode().get("id").asText();
		}

		@Override
		public void addComment(String postId, String comment, SpaceDogHelper.User user) throws Exception {

			ObjectNode bigPost = SpaceRequest.get("/v1/data/bigpost/{id}").backend(adminAccount)
					.routeParam("id", postId).go(200).objectNode();

			((ArrayNode) bigPost.get("responses"))
					.add(Json.objectBuilder().put("title", comment).put("author", user.username).build());

			SpaceRequest.put("/v1/data/bigpost/" + postId).backend(adminAccount).basicAuth(user)
					.body(bigPost.toString()).go(200);
		}

		@Override
		public Iterator<JsonNode> showWall() throws Exception {

			String wallQuery = Json.objectBuilder()//
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
					.object("match_all")//
					.build().toString();

			return SpaceRequest.post("/v1/search/bigpost?refresh=true").backend(adminAccount).body(wallQuery).go(200)
					.jsonNode().get("results").elements();
		}
	}

	public static class SmallPost implements ChauffeLeEngine {

		@Override
		public String createSubject(String subject, SpaceDogHelper.User user) throws Exception {

			String smallPost = Json.objectBuilder().put("title", subject).build().toString();

			return SpaceRequest.post("/v1/data/smallpost").backend(adminAccount).basicAuth(user).body(smallPost)
					.go(201).objectNode().get("id").asText();
		}

		@Override
		public void addComment(String parentId, String comment, SpaceDogHelper.User user) throws Exception {

			String smallPost = Json.objectBuilder().put("title", comment)//
					.put("parent", parentId)//
					.build().toString();

			SpaceRequest.post("/v1/data/smallpost").backend(adminAccount).basicAuth(user).body(smallPost).go(201);
		}

		@Override
		public Iterator<JsonNode> showWall() throws Exception {

			String subjectQuery = Json.objectBuilder()//
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
					.object("not")//
					.object("exists")//
					.put("field", "parent")//
					.build().toString();

			JsonNode subjectResults = SpaceRequest.post("/v1/search/smallpost?refresh=true").backend(adminAccount)
					.body(subjectQuery).go(200).jsonNode();

			JsonBuilder<ObjectNode> responsesQuery = Json.objectBuilder()//
					.put("from", 0)//
					.put("size", 10)//
					.array("sort")//
					.add("parent")//
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
					.object("terms")//
					.array("parent");

			Iterator<JsonNode> subjects = subjectResults.get("results").elements();

			while (subjects.hasNext())
				responsesQuery.add(subjects.next().get("meta").get("id").asText());

			SpaceRequest.post("/v1/data/smallpost/search").backend(adminAccount)
					.body(responsesQuery.build().toString()).go(200);

			return subjectResults.get("results").elements();
		}
	}
}
