package io.spacedog.rest;

import org.junit.Assert;

import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Optional7;
import io.spacedog.utils.Passwords;
import io.spacedog.utils.SpaceFields;
import io.spacedog.utils.SpaceParams;
import io.spacedog.utils.Utils;

public class SpaceTest extends Assert implements SpaceFields, SpaceParams {

	public static SpaceDog signUp(SpaceDog backend, String username, String password) {
		return signUp(backend.backendId(), username, password);
	}

	public static SpaceDog signUp(String backendId, String username, String password) {
		return signUp(backendId, username, password, "platform@spacedog.io");
	}

	public static SpaceDog signUp(SpaceDog backend, String username, String password, String email) {
		return signUp(backend.backendId(), username, password, email);
	}

	public static SpaceDog signUp(String backendId, String username, String password, String email) {
		return SpaceDog.backendId(backendId).username(username).email(email).signUp(password);
	}

	public static SpaceDog createTempUser(SpaceDog superadmin, String username) {
		return createTempUser(superadmin.backendId(), username);
	}

	public static SpaceDog createTempUser(String backendId, String username) {
		String password = Passwords.random();
		SpaceDog.backendId(backendId)//
				.credentials().create(username, password, "platform@spacedog.io");
		return SpaceDog.backendId(backendId).username(username)//
				.email("platform@spacedog.io").password(password);
	}

	public static void superdogDeletesCredentials(String backendId, String username) {

		SpaceDog superdog = superdog(backendId);
		Optional7<Credentials> optional = superdog.credentials().getByUsername(username);

		if (optional.isPresent())
			superdog.credentials().delete(optional.get().id());
	}

	public static SpaceDog resetTestBackend() {
		return resetBackend("test", "test", "hi test");
	}

	public static SpaceDog resetTest2Backend() {
		return resetBackend("test2", "test2", "hi test2");
	}

	public static SpaceDog resetBackend(String backendId, String username, String password) {
		return resetBackend(backendId, username, password, "platform@spacedog.io");
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
				.password(env.getOrElseThrow("spacedog.superdog.password"));
	}
}
