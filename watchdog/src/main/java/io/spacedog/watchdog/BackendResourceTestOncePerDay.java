/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.junit.Test;

import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;

public class BackendResourceTestOncePerDay extends SpaceTest {

	@Test
	public void createBackendSendsNotificationToSuperDogs() {

		// prepare
		prepareTest();

		// delete test backend if necessary
		SpaceDog.backend("test").username("test").password("hi test")//
				.admin().deleteBackend("test");

		// re create test backend with notification
		SpaceDog.backend("test").admin().createBackend("test", "hi test", "test@dog.com", true);
	}
}
