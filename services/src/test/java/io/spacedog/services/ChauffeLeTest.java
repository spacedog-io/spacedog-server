/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Iterator;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.model.DataPermission;
import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.Schema;

public class ChauffeLeTest extends SpaceTest {

	private static SpaceDog backend;

	private static SpaceDog lui;
	private static SpaceDog elle;
	private static SpaceDog laCopine;

	@BeforeClass
	public static void resetBackend() {

		backend = resetTestBackend();

		backend.schema().set(buildBigPostSchema());
		backend.schema().set(buildSmallPostSchema());

		lui = signUp(backend, "lui", "hi lui", "lui@chauffe.le");
		elle = signUp(backend, "elle", "hi elle", "elle@chauffe.le");
		laCopine = signUp(backend, "lacopine", "hi la copine", "lacopine@chauffe.le");
	}

	static Schema buildBigPostSchema() {
		return Schema.builder("bigpost") //
				.text("title").french()//

				.object("responses").array() //
				.text("title").french()//
				.string("author") //
				.close() //

				.acl("user", DataPermission.create, DataPermission.search, DataPermission.update_all)//
				.build();
	}

	static Schema buildSmallPostSchema() {
		return Schema.builder("smallpost") //
				.text("title").french()//
				.string("parent")//
				.build();
	}

	@Test
	public void chauffeLeWithBigPost() {
		prepareTest();
		chauffeLe(new BigPost());
	}

	@Test
	public void chauffeLeWithSmallPost() {
		prepareTest();
		chauffeLe(new SmallPost());
	}

	public void chauffeLe(ChauffeLeEngine impl) {

		String threadId = impl.createSubject(lui, "je suis partie en mission en argentine");
		impl.addComment(lui, threadId, "tu connais ?");
		impl.addComment(elle, threadId, "hein, heu nan je ne parle pas espagnol");
		impl.addComment(lui, threadId, "un pays génial, à la base, j'étais juste partie qq mois pour bosser");
		impl.addComment(lui, threadId, "et puis finalement je suis resté un an");
		impl.addComment(elle, threadId, "ha ouais.");

		threadId = impl.createSubject(laCopine, "ALORS ??? ton RDV TINDER ??");
		impl.addComment(elle, threadId, "il tourne autour du pot");
		impl.addComment(laCopine, threadId, "CHAUFFE LE !!!!!!!!!!");

		threadId = impl.createSubject(lui, "j'ai traversé la pampa");
		impl.addComment(lui, threadId, "j'ai même été boire du café en Colombie, hein");
		impl.addComment(lui, threadId, "mais que du café ;-)");
		impl.addComment(elle, threadId, "hein hein");
		impl.addComment(lui, threadId, "j'ai vu de la végétation, des arbres immenses...");

		threadId = impl.createSubject(laCopine, "CHAUFFE LE !!!!!!");

		threadId = impl.createSubject(lui, "et, euh, le plus fort");
		impl.addComment(lui, threadId, "t'as des dauphins roses");
		impl.addComment(elle, threadId, "nan");

		impl.showWall(lui);
	}

	public interface ChauffeLeEngine {

		String createSubject(SpaceDog user, String subject);

		void addComment(SpaceDog user, String threadId, String comment);

		Iterator<JsonNode> showWall(SpaceDog user);
	}

	public static class BigPost implements ChauffeLeEngine {

		@Override
		public String createSubject(SpaceDog user, String subject) {

			return SpaceRequest.post("/1/data/bigpost").userAuth(user)//
					.body("title", subject, "responses", Json.array())//
					.go(201)//
					.objectNode().get("id").asText();
		}

		@Override
		public void addComment(SpaceDog user, String postId, String comment) {

			ObjectNode bigPost = SpaceRequest.get("/1/data/bigpost/" + postId)//
					.userAuth(user).go(200).objectNode();

			bigPost.withArray("responses")//
					.add(Json.object("title", comment, "author", user.username()));

			SpaceRequest.put("/1/data/bigpost/" + postId).userAuth(user).body(bigPost).go(200);
		}

		@Override
		public Iterator<JsonNode> showWall(SpaceDog user) {

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

			return SpaceRequest.post("/1/search/bigpost").refresh().userAuth(user)//
					.body(wallQuery).go(200).jsonNode().get("results").elements();
		}
	}

	public static class SmallPost implements ChauffeLeEngine {

		@Override
		public String createSubject(SpaceDog user, String subject) {

			return SpaceRequest.post("/1/data/smallpost").userAuth(user)//
					.body("title", subject).go(201).objectNode().get("id").asText();
		}

		@Override
		public void addComment(SpaceDog user, String parentId, String comment) {

			SpaceRequest.post("/1/data/smallpost").userAuth(user)//
					.body("title", comment, "parent", parentId).go(201);
		}

		@Override
		public Iterator<JsonNode> showWall(SpaceDog user) {

			ObjectNode subjectQuery = Json.objectBuilder()//
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
					.build();

			JsonNode subjectResults = SpaceRequest.post("/1/search/smallpost")//
					.refresh().userAuth(user).body(subjectQuery).go(200).jsonNode();

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

			SpaceRequest.post("/1/search/smallpost")//
					.userAuth(user).body(responsesQuery.build()).go(200);

			return subjectResults.get("results").elements();
		}
	}
}
