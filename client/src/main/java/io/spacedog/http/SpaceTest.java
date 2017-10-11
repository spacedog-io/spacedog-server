package io.spacedog.http;

import java.util.Arrays;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.Assert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import io.spacedog.client.SpaceDog;
import io.spacedog.utils.Json;
import io.spacedog.utils.Passwords;
import io.spacedog.utils.SpaceFields;
import io.spacedog.utils.SpaceParams;
import io.spacedog.utils.Utils;

public class SpaceTest extends Assert implements SpaceFields, SpaceParams {

	public static final String DEFAULT_EMAIL = "platform@spacedog.io";

	public static SpaceDog createTempUser(SpaceDog dog, String username) {
		return createTempUser(dog.backendId(), username);
	}

	public static SpaceDog createTempUser(String backendId, String username) {
		return SpaceDog.backendId(backendId).credentials()//
				.create(username, Passwords.random(), DEFAULT_EMAIL);
	}

	public static SpaceDog resetTestBackend() {
		return resetBackend("test", "test", "hi test");
	}

	public static SpaceDog resetTest2Backend() {
		return resetBackend("test2", "test2", "hi test2");
	}

	public static SpaceDog resetBackend(String backendId, String username, String password) {
		return resetBackend(backendId, username, password, DEFAULT_EMAIL);
	}

	public static SpaceDog resetBackend(String backendId, String username, String password, //
			String email) {
		SpaceDog superadmin = SpaceDog.backendId(backendId)//
				.username(username).password(password).email(email);
		superadmin.admin().deleteBackend(backendId);
		SpaceDog.backendId(backendId).admin().createBackend(username, password, email, false);
		return superadmin;
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
		if (superdog == null)
			superdog = superdog(SpaceBackend.defaultBackendId());
		return superdog;
	}

	public static SpaceDog superdog(SpaceDog dog) {
		return superdog(dog.backendId());
	}

	public static SpaceDog superdog(String backendId) {
		SpaceEnv env = SpaceRequest.env();
		return SpaceDog.backendId(backendId).username("superdog")//
				.password(env.getOrElseThrow("spacedog.superdog.password"))//
				.id("superdog");
	}

	public static void fail(String message, Object... args) {
		Assert.fail(String.format(message, args));
	}

	public static void fail(Throwable t) {
		t.printStackTrace();
		fail(t.getMessage());
	}

	public static DateTime assertDateIsValid(JsonNode date) {
		assertNotNull(date);
		if (!date.isTextual())
			fail("json date [%s] is not valid", date);
		return assertDateIsValid(date.asText());
	}

	public static DateTime assertDateIsValid(String date) {
		assertNotNull(date);
		try {
			return DateTime.parse(date);
		} catch (IllegalArgumentException e) {
			fail("string date [%s] is not valid", date);
			return null;
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
			Assert.fail(String.format("date time [%s] is " //
					+ "not a recent enough (now +/- 3s)", date));
		return date;
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
		else
			fail("field [%s] null or not a string", fieldPath);
	}

}
