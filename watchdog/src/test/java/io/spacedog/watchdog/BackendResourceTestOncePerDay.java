/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.junit.Assert;
import org.junit.Test;

import io.spacedog.client.SpaceClient;
import io.spacedog.watchdog.SpaceSuite.TestOncePerDay;

@TestOncePerDay
public class BackendResourceTestOncePerDay extends Assert {

	@Test
	public void createBackendSendsNotificationToSuperDogs() throws Exception {

		// notification is only sent if forTesting = false
		SpaceClient.prepareTest(false);
		SpaceClient.resetTestBackend();
	}
}