package io.spacedog.services;

import org.junit.Assert;
import org.junit.Test;

import net.codestory.http.payload.Payload;

public class SpaceFilterTest extends Assert {

	@Test
	public void ShouldSucceedToMatchUris() {
		SpaceFilter filter = (uri, context, nextFilter) -> Payload.ok();

		assertTrue(filter.matches("/1/", null));
		assertTrue(filter.matches("/v1/", null));
		assertTrue(filter.matches("/1/data", null));
		assertTrue(filter.matches("/v1/user", null));
	}

	@Test
	public void ShouldFailToMatchUris() {
		SpaceFilter filter = (uri, context, nextFilter) -> Payload.ok();

		assertFalse(filter.matches("/2", null));
		assertFalse(filter.matches("/v2", null));
		assertFalse(filter.matches("/v11", null));
		assertFalse(filter.matches("/v1x/data", null));
		assertFalse(filter.matches("v1/data", null));
		assertFalse(filter.matches("/user/david", null));
		assertFalse(filter.matches("/v/index.html", null));
		assertFalse(filter.matches("/vx/index.html", null));
	}
}
