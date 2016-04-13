package io.spacedog.watchdog;

import org.junit.Assert;
import org.junit.Test;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.client.SpaceRequest;
import io.spacedog.watchdog.SpaceSuite.TestOncePerDay;

@TestOncePerDay
public class ServiceErrorFilterTestOncePerDay extends Assert {

	@Test
	public void notifySuperdogsForInternalServerErrors() throws Exception {

		// prepare
		SpaceDogHelper.prepareTest();

		// this fails and send notification to superdogs with error details
		SpaceRequest.get("/1/admin/return500").go(500);
	}
}
