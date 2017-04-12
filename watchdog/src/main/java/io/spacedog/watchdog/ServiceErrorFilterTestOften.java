/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.junit.Test;

import io.spacedog.rest.SpaceRequest;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;

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

		SpaceRequest.put("/1/login").auth(test).go(405)//
				.assertFalse("success")//
				.assertEquals("method [PUT] not valid for path [/1/login]", "error.message");
	}
}
