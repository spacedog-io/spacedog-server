package io.spacedog.test;

import org.junit.Test;

import io.spacedog.http.SpaceRequest;

public class ServiceErrorFilterTest extends SpaceTest {

	@Test
	public void notifySuperdogsForInternalServerErrors() {

		// prepare
		prepareTest();

		// this fails and send notification to superdogs with error details
		SpaceRequest.get("/1/admin/return500").go(500);
	}
}
