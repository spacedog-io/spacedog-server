package io.spacedog.rest;

import org.junit.Assert;

import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.Backends;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Credentials.Level;
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
		return SpaceDog.backend(backendId).username(username).email(email).signUp(password);
	}

	public static SpaceDog createTempUser(SpaceDog superadmin, String username) {
		return createTempUser(superadmin.backendId(), username);
	}

	public static SpaceDog createTempUser(String backendId, String username) {
		String password = Passwords.random();
		Credentials credentials = SpaceDog.backend(backendId)//
				.credentials().create(username, password, "platform@spacedog.io");
		return SpaceDog.fromCredentials(credentials).password(password);
	}

	public static SpaceDog createAdminCredentials(SpaceDog backend, String username, String password, String email) {

		Credentials credentials = SpaceDog.backend(backend.backendId())//
				.credentials().create(username, password, email, Level.ADMIN);
		return SpaceDog.fromCredentials(credentials).password(password);
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
		SpaceDog superadmin = SpaceDog.backend(backendId)//
				.username(username).password(password).email(email);
		superadmin.admin().deleteBackend(backendId);
		SpaceDog.backend(backendId).admin().createBackend(username, password, email, false);
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

	public static SpaceDog superdog() {
		return superdog(Backends.rootApi());
	}

	public static SpaceDog superdog(SpaceDog dog) {
		return superdog(dog.backendId());
	}

	public static SpaceDog superdog(String backendId) {
		SpaceEnv env = SpaceEnv.defaultEnv();
		return SpaceDog.backend(backendId)//
				.username(env.get("spacedog.superdog.username"))//
				.password(env.get("spacedog.superdog.password"));
	}
}
