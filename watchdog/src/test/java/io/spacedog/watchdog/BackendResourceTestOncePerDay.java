/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.junit.Test;

import io.spacedog.client.SpaceTest;
import io.spacedog.watchdog.SpaceSuite.TestOncePerDay;

@TestOncePerDay
public class BackendResourceTestOncePerDay extends SpaceTest {

	@Test
	public void createBackendSendsNotificationToSuperDogs() {

		prepareTest();
		Backend test = new Backend("test", "test", "hi test", "david@spacedog.io");
		deleteBackend(test);
		createBackend(test, true);
	}
}
