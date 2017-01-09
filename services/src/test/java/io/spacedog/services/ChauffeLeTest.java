/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Iterator;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceClient.Backend;
import io.spacedog.client.SpaceClient.User;
import io.spacedog.client.SpaceRequest;
import io.spacedog.utils.DataPermission;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.Schema;

public class ChauffeLeTest extends Assert {

	private static Backend backend;

	private static User lui;
	private static User elle;
	private static User laCopine;

	@BeforeClass
	public static void resetBackend() {

		backend = SpaceClient.resetTestBackend();

		SpaceClient.setSchema(buildBigPostSchema(), backend);
		SpaceClient.setSchema(buildSmallPostSchema(), backend);

		lui = SpaceClient.signUp(backend, "lui", "hi lui", "lui@chauffe.le");
		elle = SpaceClient.signUp(backend, "elle", "hi elle", "elle@chauffe.le");
		laCopine = SpaceClient.signUp(backend, "lacopine", "hi la copine", "lacopine@chauffe.le");
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
		SpaceClient.prepareTest();
		chauffeLe(new BigPost());
	}

	@Test
	public void chauffeLeWithSmallPost() {
		SpaceClient.prepareTest();
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

		String createSubject(User user, String subject);

		void addComment(User user, String threadId, String comment);

		Iterator<JsonNode> showWall(User user);
	}

	public static class BigPost implements ChauffeLeEngine {

		@Override
		public String createSubject(User user, String subject) {

			return SpaceRequest.post("/1/data/bigpost").userAuth(user)//
					.body("title", subject, "responses", Json.array())//
					.go(201)//
					.objectNode().get("id").asText();
		}

		@Override
		public void addComment(User user, String postId, String comment) {

			ObjectNode bigPost = SpaceRequest.get("/1/data/bigpost/" + postId)//
					.userAuth(user).go(200).objectNode();

			bigPost.withArray("responses")//
					.add(Json.object("title", comment, "author", user.username));

			SpaceRequest.put("/1/data/bigpost/" + postId).userAuth(user).body(bigPost).go(200);
		}

		@Override
		public Iterator<JsonNode> showWall(User user) {

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
		public String createSubject(User user, String subject) {

			return SpaceRequest.post("/1/data/smallpost").userAuth(user)//
					.body("title", subject).go(201).objectNode().get("id").asText();
		}

		@Override
		public void addComment(SpaceClient.User user, String parentId, String comment) {

			SpaceRequest.post("/1/data/smallpost").userAuth(user)//
					.body("title", comment, "parent", parentId).go(201);
		}

		@Override
		public Iterator<JsonNode> showWall(User user) {

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
