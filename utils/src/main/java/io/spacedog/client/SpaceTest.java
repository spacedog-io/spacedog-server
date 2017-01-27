package io.spacedog.client;

import java.util.Optional;

import org.junit.Assert;

import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Passwords;
import io.spacedog.utils.SpaceFields;
import io.spacedog.utils.SpaceParams;
import io.spacedog.utils.Utils;

public class SpaceTest extends Assert implements SpaceFields, SpaceParams {

	public static SpaceDog signUp(SpaceDog backend, String username, String password) {
		return SpaceDog.backend(backend.backendId()).signUp(username, password, "platform@spacedog.io");
	}

	public static SpaceDog signUp(String backendId, String username, String password) {
		return SpaceDog.backend(backendId).signUp(username, password, "platform@spacedog.io");
	}

	public static SpaceDog signUp(SpaceDog backend, String username, String password, String email) {
		return SpaceDog.backend(backend.backendId()).signUp(username, password, email);
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
				.credentials().create(username, password, email, true);
		return SpaceDog.fromCredentials(credentials).password(password);
	}

	public static void superdogDeletesCredentials(String backendId, String username) {

		SpaceDog superdog = superdog(backendId);
		Optional<Credentials> optional = superdog.credentials().getByUsername(username);

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

		return SpaceDog.backend(backendId).username(username).password(password).email(email)//
				.backend().delete()//
				.dog().signUpBackend();
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

	public static void deleteAll(String type, SpaceDog backend) {
		SpaceRequest.delete("/1/data/" + type).adminAuth(backend).go(200);
	}

	public static void setRole(SpaceDog admin, SpaceDog user, String role) {
		admin.credentials().setRole(user.id(), role);
	}

	public static SpaceDog superdog(String backendId) {
		SpaceEnv env = SpaceEnv.defaultEnv();
		return SpaceDog.backend(backendId)//
				.username(env.get("spacedog.superdog.username"))//
				.password(env.get("spacedog.superdog.password"));
	}
}
