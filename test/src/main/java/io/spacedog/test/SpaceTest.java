package io.spacedog.test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.joda.time.DateTime;
import org.junit.Assert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

import io.spacedog.client.SpaceDog;
import io.spacedog.http.SpaceFields;
import io.spacedog.http.SpaceParams;
import io.spacedog.http.SpaceRequest;
import io.spacedog.http.SpaceRequestException;
import io.spacedog.model.Passwords;
import io.spacedog.model.Roles;
import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;

public class SpaceTest extends Assert implements SpaceFields, SpaceParams {

	public static final String DEFAULT_EMAIL = "platform@spacedog.io";

	public static SpaceDog createTempDog(SpaceDog dog, String username) {
		return dog.credentials()//
				.create(username, Passwords.random(), DEFAULT_EMAIL);
	}

	public SpaceDog createTempDog(SpaceDog dog, String username, String role) {
		return dog.credentials()//
				.create(username, Passwords.random(), DEFAULT_EMAIL, role);
	}

	public static SpaceDog signUpTempDog(String backendId, String username) {
		return SpaceDog.backendId(backendId).credentials()//
				.create(username, Passwords.random(), DEFAULT_EMAIL);
	}

	public static SpaceDog clearRootBackend() {
		superdog().post("/1/admin/clear").go(200);
		return superdog.credentials().create("superadmin", Passwords.random(), //
				"platform@spacedog.io", Roles.superadmin);
	}

	public static void prepareTest() {
		prepareTestInternal(true);
	}

	public static void prepareTest(boolean forTesting) {
		prepareTestInternal(forTesting);
	}

	private static void prepareTestInternal(boolean forTesting) {

		SpaceRequest.setForTestingDefault(forTesting);
		StackTraceElement grandParentStackTraceElement = Utils.getGrandParentStackTraceElement();

		Utils.info();
		Utils.info("--- %s.%s() ---", //
				grandParentStackTraceElement.getClassName(), //
				grandParentStackTraceElement.getMethodName());
	}

	private static SpaceDog superdog;

	public static SpaceDog superdog() {
		if (superdog == null) {
			String password = SpaceRequest.env()//
					.getOrElseThrow("spacedog.superdog.password");
			superdog = SpaceDog.defaultBackend().username("superdog")//
					.password(password).id("superdog");
		}
		return superdog;
	}

	public static DateTime assertDateIsValid(JsonNode date) {
		assertNotNull(date);
		if (!date.isTextual()) {
			Object[] args = { date };
			throw failure("json date [%s] is not valid", args);
		}
		return assertDateIsValid(date.asText());
	}

	public static DateTime assertDateIsValid(String date) {
		assertNotNull(date);
		try {
			return DateTime.parse(date);
		} catch (IllegalArgumentException e) {
			throw failure("string date [%s] is not valid", date);
		}
	}

	public static DateTime assertDateIsRecent(JsonNode node) {
		DateTime date = assertDateIsValid(node);
		assertDateIsRecent(date);
		return date;
	}

	public static DateTime assertDateIsRecent(String string) {
		DateTime date = assertDateIsValid(string);
		assertDateIsRecent(date);
		return date;
	}

	public static DateTime assertDateIsRecent(DateTime date) {
		long now = DateTime.now().getMillis();
		if (date.isBefore(now - 3000) || date.isAfter(now + 3000))
			throw failure("date time [%s] is not a recent enough (now +/- 3s)", date);
		return date;
	}

	public void assertContainsValue(String expected, JsonNode node, String fieldName) {
		if (!node.findValuesAsText(fieldName).contains(expected))
			throw failure("no field [%s] found with value [%s]", fieldName, expected);
	}

	public void assertContains(JsonNode expected, JsonNode node) {
		if (!Iterators.contains(node.elements(), expected))
			throw failure("node [%s] does not contain [%s]", node, expected);
	}

	public void assertContains(JsonNode expected, JsonNode node, String fieldPath) {
		if (!Iterators.contains(Json.get(node, fieldPath).elements(), expected))
			throw failure("node [%s] does not contain field [%s] containing value [%s]", //
					node, fieldPath, expected);
	}

	private static final List<String> metadataFieldNames = Lists.newArrayList(//
			OWNER_FIELD, GROUP_FIELD, CREATED_AT_FIELD, UPDATED_AT_FIELD);

	public static void assertSourceAlmostEquals(ObjectNode expected, ObjectNode value, String... without) {
		assertEquals(expected.deepCopy().without(metadataFieldNames).without(Arrays.asList(without)), //
				value.deepCopy().without(metadataFieldNames).without(Arrays.asList(without)));
	}

	public static void assertFieldEquals(String expected, JsonNode node, String fieldPath) {
		JsonNode fieldNode = Json.get(node, fieldPath);
		if (fieldNode != null && fieldNode.isTextual())
			assertEquals(expected, fieldNode.asText());
		else {
			Object[] args = { fieldPath };
			throw failure("field [%s] null or not a string", args);
		}
	}

	public static SpaceRequestException assertHttpError(int status, Supplier<?> action) {
		return assertHttpError(status, () -> {
			action.get();
		});
	}

	public static SpaceRequestException assertHttpError(int status, Runnable action) {
		SpaceRequestException exception = assertThrow(SpaceRequestException.class, action);
		assertEquals(status, exception.httpStatus());
		return exception;
	}

	public static <T extends RuntimeException> T assertThrow(//
			Class<T> exceptionClass, Supplier<?> action) {

		return assertThrow(exceptionClass, () -> {
			action.get();
		});
	}

	@SuppressWarnings("unchecked")
	public static <T extends RuntimeException> T assertThrow(//
			Class<T> exceptionClass, Runnable action) {

		try {
			action.run();

		} catch (Exception e) {
			if (exceptionClass.isAssignableFrom(e.getClass()))
				return (T) e;
			throw e;
		}
		throw failure("function did not throw any [%s]", exceptionClass);
	}

	public static <T> T retry(int tries, long millis, Supplier<T> action) {

		AssertionError e = null;

		for (int i = 0; i < tries; i++) {
			try {
				return action.get();

			} catch (AssertionError ee) {

				if (i == tries - 1)
					e = failure(ee, "assertion error despite [%s] tries", tries);
				else
					try {
						Thread.sleep(millis);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
			}
		}

		throw e;
	}

	public static AssertionError failure(String message, Object... args) {
		return new AssertionError(String.format(message, args));
	}

	public static AssertionError failure(Throwable t) {
		return failure(t, "unexpected exception");
	}

	public static AssertionError failure(Throwable t, String message, Object... args) {
		t.printStackTrace();
		return new AssertionError(String.format(message, args), t);
	}

}
