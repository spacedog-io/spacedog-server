package io.spacedog.server;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import io.spacedog.server.ElasticRepository;

public class ElasticRepositoryTest extends Assert {

	@Test
	public void testToRepositoryId() {
		assertEquals("2016-52", ElasticRepository.toRepositoryId(//
				DateTime.parse("2017-01-01T00:00:00.000Z")));
		assertEquals("2017-01", ElasticRepository.toRepositoryId(//
				DateTime.parse("2017-01-02T00:00:00.000Z")));
	}

	@Test
	public void testToDateTime() {
		assertEquals(DateTime.parse("2017-01-02T00:00:00.000Z"), //
				ElasticRepository.toDateTime("2017-01"));
		assertEquals(DateTime.parse("2017-01-09T00:00:00.000Z"), //
				ElasticRepository.toDateTime("2017-02"));
		assertEquals(DateTime.parse("2017-12-25T00:00:00.000Z"), //
				ElasticRepository.toDateTime("2017-52"));
		assertEquals(DateTime.parse("2013-12-30T00:00:00.000Z"), //
				ElasticRepository.toDateTime("2014-01"));
		assertEquals(DateTime.parse("2013-12-23T00:00:00.000Z"), //
				ElasticRepository.toDateTime("2013-52"));
	}

	@Test
	public void testGetPreviousRepositoryId() {
		assertEquals("2015-11", ElasticRepository.getPreviousRepositoryId("2015-12", 1));
		assertEquals("2015-08", ElasticRepository.getPreviousRepositoryId("2015-12", 4));
		assertEquals("2014-52", ElasticRepository.getPreviousRepositoryId("2015-12", 12));
		assertEquals("2014-52", ElasticRepository.getPreviousRepositoryId("2015-01", 1));
		assertEquals("2014-50", ElasticRepository.getPreviousRepositoryId("2015-01", 3));
		assertEquals("2014-01", ElasticRepository.getPreviousRepositoryId("2015-01", 52));
	}
}
