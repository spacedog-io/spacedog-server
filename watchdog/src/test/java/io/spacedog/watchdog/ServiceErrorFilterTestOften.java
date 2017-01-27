/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.junit.Test;

import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceTest;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.watchdog.SpaceSuite.TestOften;

@TestOften
public class ServiceErrorFilterTestOften extends SpaceTest {

	@Test
	public void catchesFluentResourceErrors() {

		prepareTest();
		SpaceDog test = resetTestBackend();

		// should fail to access invalid route

		SpaceRequest.get("/1/toto").backend(test).go(404)//
				.assertFalse("success")//
				.assertEquals("[/1/toto] not a valid path", "error.message");

		// should fail to use this method for this valid route

		SpaceRequest.put("/1/login").adminAuth(test).go(405)//
				.assertFalse("success")//
				.assertEquals("method [PUT] not valid for path [/1/login]", "error.message");
	}
}
