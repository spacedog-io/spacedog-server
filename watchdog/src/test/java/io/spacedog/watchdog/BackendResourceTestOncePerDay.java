/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.junit.Assert;
import org.junit.Test;

import io.spacedog.client.SpaceClient;
import io.spacedog.client.SpaceClient.Backend;
import io.spacedog.watchdog.SpaceSuite.TestOncePerDay;

@TestOncePerDay
public class BackendResourceTestOncePerDay extends Assert {

	@Test
	public void createBackendSendsNotificationToSuperDogs() {

		SpaceClient.prepareTest();
		Backend test = new Backend("test", "test", "hi test", "david@spacedog.io");
		SpaceClient.deleteBackend(test);
		SpaceClient.createBackend(test, true);
	}
}
