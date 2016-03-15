package io.spacedog.services;

import org.junit.Assert;
import org.junit.Test;

import io.spacedog.services.Credentials.Level;

public class CredentialsTest extends Assert {

	@Test
	public void shouldGetLowerOrEqualCredentialsTypes() {
		assertArrayEquals(//
				new Level[] { Level.KEY }, //
				Level.KEY.lowerOrEqual());
		assertArrayEquals(//
				new Level[] { Level.KEY, Level.USER }, //
				Level.USER.lowerOrEqual());
		assertArrayEquals(//
				new Level[] { Level.KEY, Level.USER, Level.OPERATOR }, //
				Level.OPERATOR.lowerOrEqual());
		assertArrayEquals(//
				new Level[] { Level.KEY, Level.USER, Level.OPERATOR, Level.ADMIN }, //
				Level.ADMIN.lowerOrEqual());
		assertArrayEquals(//
				new Level[] { Level.KEY, Level.USER, Level.OPERATOR, Level.ADMIN, Level.SUPER_ADMIN }, //
				Level.SUPER_ADMIN.lowerOrEqual());
		assertArrayEquals(//
				new Level[] { Level.KEY, Level.USER, Level.OPERATOR, Level.ADMIN, Level.SUPER_ADMIN, Level.SUPERDOG }, //
				Level.SUPERDOG.lowerOrEqual());
	}
}
