package io.spacedog.services;

import org.joda.time.DateTime;
import org.junit.Test;

import io.spacedog.client.SpaceTest;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Credentials.Level;

public class CredentialsTest extends SpaceTest {

	@Test
	public void shouldGetLowerOrEqualCredentialsTypes() {
		assertArrayEquals(//
				new Level[] { Level.KEY }, //
				Level.KEY.lowerOrEqual());
		assertArrayEquals(//
				new Level[] { Level.KEY, Level.USER }, //
				Level.USER.lowerOrEqual());
		assertArrayEquals(//
				new Level[] { Level.KEY, Level.USER, Level.ADMIN }, //
				Level.ADMIN.lowerOrEqual());
		assertArrayEquals(//
				new Level[] { Level.KEY, Level.USER, Level.ADMIN, Level.SUPER_ADMIN }, //
				Level.SUPER_ADMIN.lowerOrEqual());
		assertArrayEquals(//
				new Level[] { Level.KEY, Level.USER, Level.ADMIN, Level.SUPER_ADMIN, Level.SUPERDOG }, //
				Level.SUPERDOG.lowerOrEqual());
	}

	@Test
	public void testEnabled() {
		DateTime now = DateTime.now();

		assertFalse(newCredentials(now.minusHours(2), now.minusHours(1)).isReallyEnabled());
		assertTrue(newCredentials(now.minusHours(2), now.plusHours(2)).isReallyEnabled());
		assertFalse(newCredentials(now.plusHours(1), now.plusHours(2)).isReallyEnabled());

		assertTrue(newCredentials(now.minusHours(1), now.minusHours(2)).isReallyEnabled());
		assertFalse(newCredentials(now.plusHours(2), now.minusHours(2)).isReallyEnabled());
		assertFalse(newCredentials(now.plusHours(2), now.plusHours(1)).isReallyEnabled());

		assertTrue(newCredentials(now.minusHours(2), null).isReallyEnabled());
		assertFalse(newCredentials(now.plusHours(2), null).isReallyEnabled());

		assertFalse(newCredentials(null, now.minusHours(2)).isReallyEnabled());
		assertTrue(newCredentials(null, now.plusHours(2)).isReallyEnabled());
		assertTrue(newCredentials(null, null).isReallyEnabled());
	}

	private Credentials newCredentials(DateTime enableAfter, DateTime disableAfter) {
		Credentials credentials = new Credentials();
		credentials.enableAfter(enableAfter);
		credentials.disableAfter(disableAfter);
		return credentials;
	}

}
