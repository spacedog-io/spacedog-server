package io.spacedog.test;

import java.util.function.Supplier;

import io.spacedog.client.http.SpaceAssert;
import io.spacedog.client.http.SpaceException;

public class SpaceTest extends SpaceAssert {

	public static SpaceException assertHttpError(int status, Supplier<?> action) {
		return assertHttpError(status, () -> {
			action.get();
		});
	}
}
