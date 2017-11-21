/**
 * © David Attias 2015
 */
package io.spacedog.test;

import java.util.Iterator;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceDog;
import io.spacedog.http.SpaceRequest;
import io.spacedog.model.Permission;
import io.spacedog.model.Schema;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;

public class ChauffeLeTest extends SpaceTest {

	private static SpaceDog superadmin;

	private static SpaceDog lui;
	private static SpaceDog elle;
	private static SpaceDog laCopine;

	@BeforeClass
	public static void resetBackend() {

		superadmin = resetTestBackend();

		superadmin.schemas().set(buildBigPostSchema());
		superadmin.schemas().set(buildSmallPostSchema());

		lui = createTempDog(superadmin, "lui");
		elle = createTempDog(superadmin, "elle");
		laCopine = createTempDog(superadmin, "lacopine");
	}

	static Schema buildBigPostSchema() {
		return Schema.builder("bigpost") //
				.acl("user", Permission.create, Permission.search, Permission.updateAll)//
				.text("title").french()//

				.object("responses").array() //
				.text("title").french()//
				.string("author") //
				.close() //

				.build();
	}

	static Schema buildSmallPostSchema() {
		return Schema.builder("smallpost") //
				.acl("user", Permission.create, Permission.search)//
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

			return SpaceRequest.post("/1/data/bigpost").auth(user)//
					.bodyJson("title", subject, "responses", Json.array())//
					.go(201)//
					.asJsonObject().get("id").asText();
		}

		@Override
		public void addComment(SpaceDog user, String postId, String comment) {

			ObjectNode bigPost = (ObjectNode) user.get("/1/data/bigpost/" + postId)//
					.go(200).get("source");

			bigPost.withArray("responses")//
					.add(Json.object("title", comment, "author", user.username()));

			user.put("/1/data/bigpost/" + postId).bodyJson(bigPost).go(200);
		}

		@Override
		public Iterator<JsonNode> showWall(SpaceDog user) {

			String wallQuery = Json.builder().object()//
					.add("from", 0)//
					.add("size", 10)//
					.array("sort")//
					.object()//
					.object("updatedAt")//
					.add("order", "asc")//
					.end()//
					.end()//
					.end()//
					.object("query")//
					.object("match_all")//
					.build().toString();

			return SpaceRequest.post("/1/search/bigpost").refresh().auth(user)//
					.bodyString(wallQuery).go(200).asJson().get("results").elements();
		}
	}

	public static class SmallPost implements ChauffeLeEngine {

		@Override
		public String createSubject(SpaceDog user, String subject) {

			return SpaceRequest.post("/1/data/smallpost").auth(user)//
					.bodyJson("title", subject).go(201).asJsonObject().get("id").asText();
		}

		@Override
		public void addComment(SpaceDog user, String parentId, String comment) {

			SpaceRequest.post("/1/data/smallpost").auth(user)//
					.bodyJson("title", comment, "parent", parentId).go(201);
		}

		@Override
		public Iterator<JsonNode> showWall(SpaceDog user) {

			ObjectNode subjectQuery = Json.builder().object()//
					.add("from", 0)//
					.add("size", 10)//
					.array("sort")//
					.object()//
					.object("updatedAt")//
					.add("order", "asc")//
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
					.add("field", "parent")//
					.build();

			JsonNode subjectResults = user.post("/1/search/smallpost")//
					.refresh().bodyJson(subjectQuery).go(200).asJson();

			JsonBuilder<ObjectNode> responsesQuery = Json.builder().object()//
					.add("from", 0)//
					.add("size", 10)//
					.array("sort")//
					.add("parent")//
					.object()//
					.object("updatedAt")//
					.add("order", "asc")//
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
				responsesQuery.add(subjects.next().get("id").asText());

			user.post("/1/search/smallpost").bodyJson(responsesQuery.build()).go(200);

			return subjectResults.get("results").elements();
		}
	}
}
