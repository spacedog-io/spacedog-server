/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import org.junit.Test;

import io.spacedog.client.SpaceDog;
import io.spacedog.http.SpaceRequest;
import io.spacedog.http.SpaceTest;

public class ServiceErrorFilterTestOften extends SpaceTest {

	@Test
	public void catchesFluentResourceErrors() {

		prepareTest();
		SpaceDog test = resetTestBackend();

		// should fail to access invalid route

		SpaceRequest.get("/1/toto").backend(test).go(404)//
				.assertFalse("success")//
				.assertEquals("path [/1/toto] invalid", "error.message");

		// should fail to use this method for this valid route

		SpaceRequest.put("/1/login").auth(test).go(405)//
				.assertFalse("success")//
				.assertEquals("method [PUT] invalid for path [/1/login]", "error.message");
	}
}
