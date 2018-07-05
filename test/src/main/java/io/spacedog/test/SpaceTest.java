package io.spacedog.test;

import java.util.function.Supplier;

import io.spacedog.client.http.SpaceAssert;
import io.spacedog.client.http.SpaceRequestException;

public class SpaceTest extends SpaceAssert {

	public static SpaceRequestException assertHttpError(int status, Supplier<?> action) {
		return assertHttpError(status, () -> {
			action.get();
		});
	}
}
