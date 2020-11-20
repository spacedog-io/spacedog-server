package io.spacedog.services.elastic;

import org.junit.Assert;
import org.junit.Test;

import io.spacedog.database.elastic.ElasticIndex;

public class IndexTest extends Assert {

	@Test
	public void testWithoutType() {

		ElasticIndex index = ElasticIndex.valueOf("test-credentials-1");
		assertEquals("test", index.backendId());
		assertEquals("credentials", index.service());
		assertEquals("credentials", index.type());
		assertEquals(1, index.version());
		assertEquals("test-credentials-1", index.toString());
		assertEquals("test-credentials", index.alias());
		assertEquals("test-credentials-1", new ElasticIndex("credentials")//
				.backendId("test").version(1).toString());
		assertEquals(index, new ElasticIndex("credentials")//
				.backendId("test").version(1).toString());
	}

	@Test
	public void testWithType() {

		ElasticIndex index = ElasticIndex.valueOf("test-data-message-0");
		assertEquals("test", index.backendId());
		assertEquals("data", index.service());
		assertEquals("message", index.type());
		assertEquals(0, index.version());
		assertEquals("test-data-message-0", index.toString());
		assertEquals("test-data-message", index.alias());
		assertEquals("test-data-message-0", new ElasticIndex("data")//
				.type("message").backendId("test").version(0).toString());
		assertEquals(index, new ElasticIndex("data")//
				.type("message").backendId("test").version(0).toString());
	}

}
