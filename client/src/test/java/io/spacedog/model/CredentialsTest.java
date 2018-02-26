package io.spacedog.model;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.credentials.Credentials.Session;

public class CredentialsTest extends Assert {

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

	@Test
	public void testPurgeSessions() throws InterruptedException {
		Credentials credentials = new Credentials();

		// purgeOldSessions does nothing since no sessions
		credentials.purgeOldSessions(10);

		Session first = Session.newSession(100);
		Thread.sleep(5);
		Session second = Session.newSession(1);
		Thread.sleep(5);
		Session third = Session.newSession(1);

		assertTrue(first.createAt().isBefore(second.createAt()));
		assertTrue(second.createAt().isBefore(third.createAt()));

		assertTrue(first.expiresAt().isAfter(third.expiresAt()));
		assertTrue(third.expiresAt().isAfter(second.expiresAt()));

		// after 1 second only first session has not expired
		Thread.sleep(1000);
		assertTrue(first.expiresIn() > 0);
		assertEquals(0, second.expiresIn());
		assertEquals(0, third.expiresIn());

		credentials.setCurrentSession(first);
		credentials.setCurrentSession(second);
		credentials.setCurrentSession(third);

		assertEquals(3, credentials.sessions().size());
		assertEquals(first, credentials.sessions().get(0));
		assertEquals(second, credentials.sessions().get(1));
		assertEquals(third, credentials.sessions().get(2));

		// purgeOldSessions does nothing since not enough sessions
		credentials.purgeOldSessions(10);
		assertEquals(3, credentials.sessions().size());
		assertEquals(first, credentials.sessions().get(0));
		assertEquals(second, credentials.sessions().get(1));
		assertEquals(third, credentials.sessions().get(2));

		// purgeOldSessions removes first session
		credentials.purgeOldSessions(2);
		assertEquals(2, credentials.sessions().size());
		assertEquals(third, credentials.sessions().get(0));
		assertEquals(second, credentials.sessions().get(1));

		// add new session without createdAt timestamp
		// to simulate old sessions before mapping update
		Session fourth = Session.newSession(1);
		fourth.createdAt = null;
		credentials.setCurrentSession(fourth);

		assertEquals(third, credentials.sessions().get(0));
		assertEquals(second, credentials.sessions().get(1));
		assertEquals(fourth, credentials.sessions().get(2));

		// purgeOldSessions removes latest session
		// since no createdAt timestamp
		credentials.purgeOldSessions(2);
		assertEquals(2, credentials.sessions().size());
		assertEquals(third, credentials.sessions().get(0));
		assertEquals(second, credentials.sessions().get(1));
	}

	private Credentials newCredentials(DateTime enableAfter, DateTime disableAfter) {
		Credentials credentials = new Credentials();
		credentials.enableAfter(enableAfter);
		credentials.disableAfter(disableAfter);
		return credentials;
	}

}
