/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.junit.Test;

import io.spacedog.client.SpaceTest;
import io.spacedog.sdk.SpaceDog;

public class BackendResourceTestOncePerDay extends SpaceTest {

	@Test
	public void createBackendSendsNotificationToSuperDogs() {

		// prepare
		prepareTest();

		// delete test backend if necessary
		SpaceDog.backend("test").username("test").password("hi test").backend().delete();

		// re create test backend with notification
		SpaceDog.backend("test").backend().create("test", "hi test", "test@dog.com", true);
	}
}
