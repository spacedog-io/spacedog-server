/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.junit.Assert;
import org.junit.Test;

import io.spacedog.client.SpaceDogHelper;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class AccountResourceTestOncePerDay extends Assert {

	@Test
	public void createAccountSendNotificationToSuperDogs() throws Exception {

		SpaceDogHelper.printTestHeader();
		// notification is only sent if test = false
		SpaceDogHelper.resetTestAccount(false);
	}
}
