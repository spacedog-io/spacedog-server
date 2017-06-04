package io.spacedog.test;

import java.util.function.Supplier;

import io.spacedog.rest.SpaceRequestException;

public class AssertFails {

	public static <T> SpaceRequestException assertHttpStatus(//
			int expectedHttpStatus, Supplier<T> request) {

		try {
			request.get();

			throw assertionError("expected http status was [%s]", //
					expectedHttpStatus);

		} catch (SpaceRequestException e) {

			if (expectedHttpStatus != e.httpStatus())
				throw assertionError(e, "expected http status [%s] but was [%s]", //
						expectedHttpStatus, e.httpStatus());

			return e;
		}
	}

	public static AssertionError assertionError(String message, Object... args) {
		return new AssertionError(String.format(message, args));
	}

	public static AssertionError assertionError(Throwable t, String message, Object... args) {
		return new AssertionError(String.format(message, args), t);
	}

}
