package io.spacedog.client.snapshot;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

public class SpaceRepositoryTest extends Assert {

	@Test
	public void testToRepositoryId() {
		assertEquals("2016-52", SpaceRepository.toRepositoryId(//
				DateTime.parse("2017-01-01T00:00:00.000Z")));
		assertEquals("2017-01", SpaceRepository.toRepositoryId(//
				DateTime.parse("2017-01-02T00:00:00.000Z")));
	}

	@Test
	public void testToDateTime() {
		assertEquals(DateTime.parse("2017-01-02T00:00:00.000Z"), //
				SpaceRepository.toDateTime("2017-01"));
		assertEquals(DateTime.parse("2017-01-09T00:00:00.000Z"), //
				SpaceRepository.toDateTime("2017-02"));
		assertEquals(DateTime.parse("2017-12-25T00:00:00.000Z"), //
				SpaceRepository.toDateTime("2017-52"));
		assertEquals(DateTime.parse("2013-12-30T00:00:00.000Z"), //
				SpaceRepository.toDateTime("2014-01"));
		assertEquals(DateTime.parse("2013-12-23T00:00:00.000Z"), //
				SpaceRepository.toDateTime("2013-52"));
	}

	@Test
	public void testGetPreviousRepositoryId() {
		assertEquals("2015-11", SpaceRepository.pastRepositoryId("2015-12", 1));
		assertEquals("2015-08", SpaceRepository.pastRepositoryId("2015-12", 4));
		assertEquals("2014-52", SpaceRepository.pastRepositoryId("2015-12", 12));
		assertEquals("2014-52", SpaceRepository.pastRepositoryId("2015-01", 1));
		assertEquals("2014-50", SpaceRepository.pastRepositoryId("2015-01", 3));
		assertEquals("2014-01", SpaceRepository.pastRepositoryId("2015-01", 52));
	}
}
