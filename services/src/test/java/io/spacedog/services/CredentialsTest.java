package io.spacedog.services;

import org.junit.Assert;
import org.junit.Test;

import io.spacedog.services.Credentials.Type;

public class CredentialsTest extends Assert {

	@Test
	public void shouldGetLowerOrEqualCredentialsTypes() {
		assertArrayEquals(//
				new Type[] { Type.KEY }, Type.KEY.lowerOrEqual());
		assertArrayEquals(//
				new Type[] { Type.KEY, Type.USER }, Type.USER.lowerOrEqual());
		assertArrayEquals(//
				new Type[] { Type.KEY, Type.USER, Type.ADMIN }, Type.ADMIN.lowerOrEqual());
		assertArrayEquals(//
				new Type[] { Type.KEY, Type.USER, Type.ADMIN, Type.SUPERDOG }, Type.SUPERDOG.lowerOrEqual());
	}
}
