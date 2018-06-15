/**
 * © David Attias 2015
 */
package io.spacedog.test;

import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.Roles;
import io.spacedog.client.data.DataSettings;
import io.spacedog.client.data.ObjectNodeWrap;
import io.spacedog.client.data.ObjectNodeWrap.Results;
import io.spacedog.client.elastic.ESQueryBuilders;
import io.spacedog.client.elastic.ESSearchSourceBuilder;
import io.spacedog.client.elastic.ESSortOrder;
import io.spacedog.client.schema.Schema;
import io.spacedog.utils.Json;

public class SearchRestyTest extends SpaceTest {

	@Test
	public void searchAndDeleteObjects() {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearServer();
		superadmin.schemas().set(Message.schema());
		superadmin.schemas().set(Schema.builder("rubric").text("name").build());

		// creates 4 messages and 1 rubric
		superadmin.data().save("rubric", //
				Json.object("name", "riri, fifi and loulou"));
		superadmin.data().save("message", //
				Json.object("text", "what's up?"));
		superadmin.data().save("message", //
				Json.object("text", "wanna drink something?"));
		superadmin.data().save("message", //
				Json.object("text", "pretty cool something, hein?"));
		superadmin.data().save("message", //
				Json.object("text", "so long guys"));

		assertEquals(5, superadmin.data().searchRequest().refresh().go().total);

		// search for messages with full text query
		ESSearchSourceBuilder source = ESSearchSourceBuilder.searchSource().query(//
				ESQueryBuilders.matchQuery("text", "something to drink"));
		Results results = superadmin.data().searchRequest().source(source).go();

		assertEquals(2, results.total);
		assertEquals("wanna drink something?", //
				results.results.get(0).source().get("text").asText());
		assertEquals("pretty cool something, hein?", //
				results.results.get(1).source().get("text").asText());

		// check search scores
		assertTrue(results.results.get(0).score() //
		> results.results.get(1).score());

		// check all meta are there
		assertNotNull(results.results.get(0).id());
		assertNotNull(results.results.get(0).version());
		assertNotNull(results.results.get(0).owner());
		assertNotNull(results.results.get(0).group());
		assertNotNull(results.results.get(0).createdAt());
		assertNotNull(results.results.get(0).updatedAt());

		// deletes messages containing 'up' by query
		long deleted = superadmin.data().deleteBulkRequest()//
				.query(ESQueryBuilders.matchQuery("text", "something"))//
				.go();

		assertEquals(2, deleted);
		assertEquals(2, //
				superadmin.data().getAllRequest()//
						.type("message").refresh().go().total);

		// deletes data objects containing 'wanna' or 'riri'
		deleted = superadmin.data().deleteBulkRequest()//
				.query(ESQueryBuilders.boolQuery()//
						.should(ESQueryBuilders.matchQuery("name", "riri"))//
						.should(ESQueryBuilders.matchQuery("text", "so")))//
				.go();

		assertEquals(2, deleted);

		// only "what's up?" remains
		results = superadmin.data().getAllRequest().refresh().go();
		assertEquals(1, results.total);
		assertEquals("what's up?", //
				results.results.get(0).source().get("text").asText());
	}

	@Test
	public void aggregateToGetDistinctCityNames() {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearServer();
		SpaceDog vince = createTempDog(superadmin, "vince");

		// superadmin sets schemas
		superadmin.schemas().set(Schema.builder("city").keyword("name").build());

		// superadmin sets data acls
		DataSettings settings = new DataSettings();
		settings.acl().put("city", Roles.user, Permission.create, Permission.search);
		superadmin.settings().save(settings);

		// creates 5 cities but whith only 3 distinct names
		vince.data().save("city", Json.object("name", "Paris"));
		vince.data().save("city", Json.object("name", "Bordeaux"));
		vince.data().save("city", Json.object("name", "Nice"));
		vince.data().save("city", Json.object("name", "Paris"));
		vince.data().save("city", Json.object("name", "Nice"));

		// search with 'terms' aggregation to get
		// all 3 distinct city names Paris, Bordeaux and Nice
		ObjectNode query = Json.builder().object()//
				.add("size", 0)//
				.object("aggs")//
				.object("distinctCities")//
				.object("terms")//
				.add("field", "name")//
				.build();

		Results results = vince.data().searchRequest()//
				.source(query.toString()).refresh().go();

		assertEquals(0, results.results.size());
		assertEquals(3, Json.get(results.aggregations, "distinctCities.buckets").size());
		assertContainsValue("Paris", results.aggregations, "key");
		assertContainsValue("Bordeaux", results.aggregations, "key");
		assertContainsValue("Nice", results.aggregations, "key");
	}

	@Test
	public void sortSearchResults() {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearServer();

		superadmin.schemas().set(Schema.builder("number").integer("i").keyword("t").build());

		// creates 5 numbers
		for (int i = 0; i < 5; i++)
			superadmin.data().save("number", Json.object("i", i, "t", "" + i));

		// search with ascendent sorting
		ESSearchSourceBuilder searchSource = ESSearchSourceBuilder.searchSource().sort("i");
		ObjectNodeWrap.Results results = superadmin.data().searchRequest()//
				.source(searchSource).refresh().go();
		assertEquals(5, results.total);

		List<ObjectNodeWrap> objects = results.results;
		for (int i = 0; i < objects.size(); i++) {
			assertEquals(i, objects.get(i).source().get("i").asInt());
			assertEquals(i, objects.get(i).sort()[0]);
		}

		// search with descendant sorting
		searchSource = ESSearchSourceBuilder.searchSource().sort("t", ESSortOrder.DESC);
		results = superadmin.data().searchRequest()//
				.source(searchSource).refresh().go();
		assertEquals(5, results.total);

		objects = results.results;
		for (int i = 0; i < objects.size(); i++) {
			assertEquals(4 - i, objects.get(i).source().get("i").asInt());
			assertEquals(String.valueOf(4 - i), objects.get(i).sort()[0]);
		}
	}

	@Test
	public void testBadSimpleQueryStringQueries() {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearServer();
		superadmin.schemas().set(Schema.builder("message").text("text").build());

		// check simple query string '**' doesn't throw null pointer exception
		// ElasticSearch version 2.2 do contain this bug
		// Fixeds in ElasticSearech version 2.4.6
		ESSearchSourceBuilder source = ESSearchSourceBuilder.searchSource().query(//
				ESQueryBuilders.simpleQueryStringQuery("**").analyzeWildcard(true));
		superadmin.data().searchRequest().source(source).go();
	}
}
