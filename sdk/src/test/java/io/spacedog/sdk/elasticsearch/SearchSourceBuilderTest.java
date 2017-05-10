package io.spacedog.sdk.elasticsearch;

import org.junit.Assert;
import org.junit.Test;

import io.spacedog.sdk.elastic.ESQueryBuilders;
import io.spacedog.sdk.elastic.ESSearchSourceBuilder;
import io.spacedog.utils.Json7;
import io.spacedog.utils.Utils;

public class SearchSourceBuilderTest extends Assert {

	@Test
	public void builderToString() {

		String source = ESSearchSourceBuilder.searchSource()//
				.from(0).size(20).fields("firstname", "lastname").sort("firstname")//
				.query(ESQueryBuilders.boolQuery()//
						.must(ESQueryBuilders.termQuery("firstname", "Fred")))
				.toString();

		Utils.info(source);

		Json7.assertNode(Json7.readObject(source))//
				.assertEquals(0, "from")//
				.assertEquals(20, "size")//
				.assertEquals("Fred", "query.bool.must.term.firstname.0")//
				.assertEquals("firstname", "fields.0")//
				.assertEquals("lastname", "fields.1")//
				.assertNotNull("sort.0.firstname");
	}

}
