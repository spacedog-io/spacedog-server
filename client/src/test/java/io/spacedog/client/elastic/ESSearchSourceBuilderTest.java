package io.spacedog.client.elastic;

import org.junit.Assert;
import org.junit.Test;

import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;

public class ESSearchSourceBuilderTest extends Assert {

	@Test
	public void builderToString() {

		String source = ESSearchSourceBuilder.searchSource()//
				.from(0).size(20).fields("firstname", "lastname").sort("firstname")//
				.query(ESQueryBuilders.boolQuery()//
						.must(ESQueryBuilders.termQuery("firstname", "Fred")))
				.toString();

		Utils.info(source);

		Json.assertNode(Json.readObject(source))//
				.assertEquals(0, "from")//
				.assertEquals(20, "size")//
				.assertEquals("Fred", "query.bool.must.term.firstname")//
				.assertEquals("firstname", "fields.0")//
				.assertEquals("lastname", "fields.1")//
				.assertNotNull("sort.0.firstname");
	}

}
