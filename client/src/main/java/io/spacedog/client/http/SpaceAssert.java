package io.spacedog.client.http;

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
import io.spacedog.client.SpacePlatform;
import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.credentials.Passwords;
import io.spacedog.client.credentials.Roles;
import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;

public class SpaceAssert extends Assert implements SpacePlatform, SpaceFields, SpaceParams {

	public static SpaceDog createTempDog(SpaceDog parentDog, String username, String... role) {
		SpaceDog childDog = SpaceDog.dog(parentDog.backend()).username(username).password(Passwords.random());
		parentDog.credentials().create(childDog.username(), childDog.password().get(), DEFAULT_EMAIL, role);
		return childDog;
	}

	public static SpaceDog signUpTempDog(SpaceBackend backend, String username) {
		SpaceDog guest = SpaceDog.dog(backend);
		SpaceDog dog = SpaceDog.dog(backend).username(username).password(Passwords.random());
		guest.credentials().create(dog.username(), dog.password().get(), DEFAULT_EMAIL);
		return dog;
	}

	public static SpaceDog clearServer() {
		return clearServer(false);
	}

	public static SpaceDog clearServer(boolean files) {
		SpaceDog superdog = superdog();
		superdog.post("/1/admin/_clear").queryParam("files", files).go(200);
		return createTempDog(superdog, Roles.superadmin, Roles.superadmin);
	}

	public static void prepareTest() {
		prepareTest(true, false);
	}

	public static void prepareTest(boolean forTesting, boolean serverDebug) {

		SpaceRequest.setForTestingDefault(forTesting);
		SpaceRequest.setDebugServerDefault(serverDebug);

		StackTraceElement grandParentStackTraceElement = Utils.getGrandParentStackTraceElement();

		Utils.info();
		Utils.info("--- %s.%s() ---", //
				grandParentStackTraceElement.getClassName(), //
				grandParentStackTraceElement.getMethodName());
	}

	public static SpaceDog superdog() {
		return SpaceDog.dog()//
				.username(Credentials.SUPERDOG.username())//
				.password(SpaceEnv.env().superdogPassword());
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

	public static void assertContainsValue(String expected, JsonNode node, String fieldName) {
		if (!node.findValuesAsText(fieldName).contains(expected))
			throw failure("no field [%s] found with value [%s]", fieldName, expected);
	}

	public static void assertContains(JsonNode expected, JsonNode node) {
		if (!Iterators.contains(node.elements(), expected))
			throw failure("node [%s] does not contain [%s]", node, expected);
	}

	public static void assertContains(JsonNode expected, JsonNode node, String fieldPath) {
		if (!Iterators.contains(Json.get(node, fieldPath).elements(), expected))
			throw failure("node [%s] does not contain field [%s] containing value [%s]", //
					node, fieldPath, expected);
	}

	private static final List<String> metaFieldNames = Lists.newArrayList(//
			OWNER_FIELD, GROUP_FIELD, CREATED_AT_FIELD, UPDATED_AT_FIELD);

	public static void assertAlmostEquals(Object expected, Object value, String... without) {
		assertAlmostEquals(Json.toObjectNode(expected), Json.toObjectNode(value), without);
	}

	public static void assertAlmostEquals(ObjectNode expected, ObjectNode value, String... without) {
		assertEquals(expected.deepCopy().without(metaFieldNames).without(Arrays.asList(without)), //
				value.deepCopy().without(metaFieldNames).without(Arrays.asList(without)));
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

	public static SpaceRequestException assertHttpError(int status, Runnable action) {
		SpaceRequestException exception = assertThrow(SpaceRequestException.class, action);
		assertEquals(status, exception.httpStatus());
		return exception;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Throwable> T assertThrow(//
			Class<T> throwableClass, Runnable action) {

		try {
			action.run();

		} catch (Throwable e) {
			if (throwableClass.isAssignableFrom(e.getClass()))
				return (T) e;
			throw e;
		}
		throw failure("did not throw any [%s]", throwableClass);
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
