package io.spacedog.services;

import org.junit.Assert;
import org.junit.Test;

import io.spacedog.services.SnapshotResource.SpaceRepository;

public class SpaceRepositoryTest extends Assert {

	@Test
	public void testToRepositoryId() {
		assertEquals("2015-01", SpaceRepository.toRepositoryId(2015, 1));
		assertEquals("2015-09", SpaceRepository.toRepositoryId(2015, 9));
		assertEquals("2015-22", SpaceRepository.toRepositoryId(2015, 22));
	}

	@Test
	public void testGetPreviousRepositoryId() {
		assertEquals("2015-11", SpaceRepository.getPreviousRepositoryId("2015-12", 1));
		assertEquals("2015-08", SpaceRepository.getPreviousRepositoryId("2015-12", 4));
		assertEquals("2014-52", SpaceRepository.getPreviousRepositoryId("2015-12", 12));
		assertEquals("2014-52", SpaceRepository.getPreviousRepositoryId("2015-01", 1));
		assertEquals("2014-50", SpaceRepository.getPreviousRepositoryId("2015-01", 3));
		assertEquals("2014-01", SpaceRepository.getPreviousRepositoryId("2015-01", 52));
	}
}
