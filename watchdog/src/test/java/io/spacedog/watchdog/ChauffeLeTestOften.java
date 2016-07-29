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

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceClient.Backend;
import io.spacedog.client.SpaceClient.User;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.Schema;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class ChauffeLeTestOften extends Assert {

	private static Backend backend;

	private static User lui;
	private static User elle;
	private static User laCopine;

	@BeforeClass
	public static void resetBackend() throws Exception {

		backend = SpaceClient.resetTestBackend();

		SpaceClient.setSchema(buildBigPostSchema(), backend);
		SpaceClient.setSchema(buildSmallPostSchema(), backend);

		lui = SpaceClient.newCredentials(backend, "lui", "hi lui", "lui@chauffe.le");
		elle = SpaceClient.newCredentials(backend, "elle", "hi elle", "elle@chauffe.le");
		laCopine = SpaceClient.newCredentials(backend, "lacopine", "hi la copine", "lacopine@chauffe.le");
	}

	static Schema buildBigPostSchema() {
		return Schema.builder("bigpost") //
				.text("title").french()//

				.object("responses").array() //
				.text("title").french()//
				.string("author") //
				.close() //

				.build();
	}

	static Schema buildSmallPostSchema() {
		return Schema.builder("smallpost") //
				.text("title").french()//
				.string("parent")//
				.build();
	}

	@Test
	public void chauffeLeWithBigPost() throws Exception {
		SpaceClient.prepareTest();
		chauffeLe(new BigPost());
	}

	@Test
	public void chauffeLeWithSmallPost() throws Exception {
		SpaceClient.prepareTest();
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
		String createSubject(String subject, SpaceClient.User user) throws Exception;

		void addComment(String threadId, String comment, SpaceClient.User user) throws Exception;

		Iterator<JsonNode> showWall() throws Exception;
	}

	public static class BigPost implements ChauffeLeEngine {

		@Override
		public String createSubject(String subject, SpaceClient.User user) throws Exception {

			String bigPost = Json.objectBuilder().put("title", subject) //
					.array("responses") //
					.end()//
					.build().toString();

			return SpaceRequest.post("/1/data/bigpost").backend(backend).userAuth(user).body(bigPost).go(201)
					.objectNode().get("id").asText();
		}

		@Override
		public void addComment(String postId, String comment, SpaceClient.User user) throws Exception {

			ObjectNode bigPost = SpaceRequest.get("/1/data/bigpost/{id}").backend(backend).routeParam("id", postId)
					.go(200).objectNode();

			((ArrayNode) bigPost.get("responses"))
					.add(Json.objectBuilder().put("title", comment).put("author", user.username).build());

			SpaceRequest.put("/1/data/bigpost/" + postId).backend(backend).userAuth(user).body(bigPost.toString())
					.go(200);
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

			return SpaceRequest.post("/1/search/bigpost").refresh().backend(backend).body(wallQuery).go(200).jsonNode()
					.get("results").elements();
		}
	}

	public static class SmallPost implements ChauffeLeEngine {

		@Override
		public String createSubject(String subject, SpaceClient.User user) throws Exception {

			String smallPost = Json.objectBuilder().put("title", subject).build().toString();

			return SpaceRequest.post("/1/data/smallpost").backend(backend).userAuth(user).body(smallPost).go(201)
					.objectNode().get("id").asText();
		}

		@Override
		public void addComment(String parentId, String comment, SpaceClient.User user) throws Exception {

			String smallPost = Json.objectBuilder().put("title", comment)//
					.put("parent", parentId)//
					.build().toString();

			SpaceRequest.post("/1/data/smallpost").backend(backend).userAuth(user).body(smallPost).go(201);
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

			JsonNode subjectResults = SpaceRequest.post("/1/search/smallpost").refresh().backend(backend)
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

			SpaceRequest.post("/1/data/smallpost/search").backend(backend).body(responsesQuery.build().toString())
					.go(200);

			return subjectResults.get("results").elements();
		}
	}
}
