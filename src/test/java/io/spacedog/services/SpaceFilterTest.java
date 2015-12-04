package io.spacedog.services;

import org.junit.Assert;
import org.junit.Test;

import net.codestory.http.payload.Payload;

public class SpaceFilterTest extends Assert {

	@Test
	public void ShouldSucceedToMatchUris() {
		SpaceFilter filter = (uri, context, nextFilter) -> Payload.ok();

		assertTrue(filter.matches("/v1/data", null));
		assertTrue(filter.matches("/v2/data", null));
		assertTrue(filter.matches("/v3/data", null));
		assertTrue(filter.matches("/v4/data", null));
		assertTrue(filter.matches("/v5/data", null));
		assertTrue(filter.matches("/v6/data", null));
		assertTrue(filter.matches("/v7/data", null));
		assertTrue(filter.matches("/v8/data", null));
		assertTrue(filter.matches("/v9/user/david", null));
	}

	@Test
	public void ShouldFailToMatchUris() {
		SpaceFilter filter = (uri, context, nextFilter) -> Payload.ok();

		assertFalse(filter.matches("/v1", null));
		assertFalse(filter.matches("/v11", null));
		assertFalse(filter.matches("/v1x/data", null));
		assertFalse(filter.matches("v1/data", null));
		assertFalse(filter.matches("/user/david", null));
		assertFalse(filter.matches("/v/index.html", null));
		assertFalse(filter.matches("/vx/index.html", null));
	}
}
