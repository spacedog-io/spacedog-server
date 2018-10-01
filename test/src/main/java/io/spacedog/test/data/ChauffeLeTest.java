/**
 * © David Attias 2015
 */
package io.spacedog.test.data;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.Roles;
import io.spacedog.client.data.DataResults;
import io.spacedog.client.data.DataSettings;
import io.spacedog.client.elastic.ESQueryBuilders;
import io.spacedog.client.elastic.ESSearchSourceBuilder;
import io.spacedog.client.elastic.ESSortOrder;
import io.spacedog.client.schema.Schema;
import io.spacedog.test.SpaceTest;
import io.spacedog.utils.Json;

public class ChauffeLeTest extends SpaceTest {

	private static SpaceDog superadmin;

	private static SpaceDog lui;
	private static SpaceDog elle;
	private static SpaceDog laCopine;

	@BeforeClass
	public static void resetBackend() {

		prepareTest();
		superadmin = clearServer();

		superadmin.schemas().set(buildBigPostSchema());
		superadmin.schemas().set(buildSmallPostSchema());

		// superadmin sets data schema acls
		DataSettings settings = new DataSettings();
		settings.acl().put("bigpost", Roles.user, Permission.create, Permission.search, Permission.update);
		settings.acl().put("smallpost", Roles.user, Permission.create, Permission.search);
		superadmin.data().settings(settings);

		lui = createTempDog(superadmin, "lui");
		elle = createTempDog(superadmin, "elle");
		laCopine = createTempDog(superadmin, "lacopine");
	}

	static Schema buildBigPostSchema() {
		return Schema.builder("bigpost") //
				.text("title").french()//
				.object("responses") //
				.text("title").french()//
				.keyword("author") //
				.build();
	}

	static Schema buildSmallPostSchema() {
		return Schema.builder("smallpost") //
				.text("title").french()//
				.keyword("parent")//
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

		void showWall(SpaceDog user);
	}

	public static class BigPost implements ChauffeLeEngine {

		@Override
		public String createSubject(SpaceDog user, String subject) {
			return user.data().save("bigpost", //
					Json.object("title", subject, "responses", Json.array()))//
					.id();
		}

		@Override
		public void addComment(SpaceDog user, String postId, String comment) {

			ObjectNode bigPost = user.data().get("bigpost", postId);

			bigPost.withArray("responses")//
					.add(Json.object("title", comment, "author", user.username()));

			user.data().prepareSave(bigPost).type("bigpost").id(postId).go();
		}

		@Override
		public void showWall(SpaceDog user) {

			ESSearchSourceBuilder builder = ESSearchSourceBuilder.searchSource()//
					.from(0)//
					.size(10)//
					.sort("updatedAt", ESSortOrder.ASC)//
					.query(ESQueryBuilders.matchAllQuery());

			user.data().prepareSearch().type("bigpost")//
					.refresh(true).source(builder.toString()).go();
		}
	}

	public static class SmallPost implements ChauffeLeEngine {

		@Override
		public String createSubject(SpaceDog user, String subject) {
			return user.data().save("smallpost", Json.object("title", subject)).id();
		}

		@Override
		public void addComment(SpaceDog user, String parentId, String comment) {
			user.data().save("smallpost", Json.object("title", comment, "parent", parentId));
		}

		@Override
		public void showWall(SpaceDog user) {

			ESSearchSourceBuilder builder = ESSearchSourceBuilder.searchSource()//
					.from(0)//
					.size(10)//
					.sort("updatedAt", ESSortOrder.ASC)//
					.query(ESQueryBuilders.boolQuery()//
							.mustNot(ESQueryBuilders.existsQuery("parent")));

			DataResults<ObjectNode> results = user.data().prepareSearch().type("smallpost")//
					.refresh(true).source(builder.toString()).go();

			assertEquals(5, results.total);

			List<String> subjectIds = results.objects.stream().map(//
					wrap -> wrap.id()).collect(Collectors.toList());

			builder = ESSearchSourceBuilder.searchSource()//
					.from(0)//
					.size(10)//
					.sort("parent")//
					.sort("updatedAt", ESSortOrder.ASC)//
					.query(ESQueryBuilders.termsQuery("parent", subjectIds));

			results = user.data().prepareSearch().type("smallpost").source(builder.toString()).go();

			assertEquals(13, results.total);

		}
	}
}
