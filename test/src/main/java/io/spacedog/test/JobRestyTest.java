package io.spacedog.test;

import org.junit.Test;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.log.LogSearchResults;

public class JobRestyTest extends SpaceTest {

	@Test
	public void test() {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearServer();
		SpaceDog fred = createTempDog(superadmin, "fred").login();

		fred.data().prepareGetAll().go();
		fred.data().prepareGetAll().go();

		// superadmin checks everything is in place
		LogSearchResults log = superadmin.logs().get(10, true);
	}

}
