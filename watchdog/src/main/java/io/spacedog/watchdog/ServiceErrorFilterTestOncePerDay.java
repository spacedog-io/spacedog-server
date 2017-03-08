package io.spacedog.watchdog;

import org.junit.Test;

import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceTest;

public class ServiceErrorFilterTestOncePerDay extends SpaceTest {

	@Test
	public void notifySuperdogsForInternalServerErrors() {

		// prepare
		prepareTest();

		// this fails and send notification to superdogs with error details
		SpaceRequest.get("/1/admin/return500").go(500);
	}
}
