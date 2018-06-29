package io.spacedog.test;

import org.junit.Test;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.http.SpaceRequest;

public class ServiceErrorFilterTest extends SpaceTest {

	@Test
	public void catchesFluentResourceErrors() {

		prepareTest();
		SpaceDog superadmin = clearServer();

		// should fail to access invalid route
		SpaceRequest.get("/1/toto")//
				.backend(superadmin.backend())//
				.go(404)//
				.assertEquals("path [/1/toto] invalid", "error.message");

		// should fail to use this method for this valid route
		superadmin.put("/1/login").go(405)//
				.assertEquals("method [PUT] invalid for path [/1/login]", "error.message");
	}

	@Test
	public void notifySuperdogsForInternalServerErrors() {

		// prepare
		prepareTest();

		// this fails and send notification to superdogs with error details
		SpaceRequest.post("/1/admin/_return_500").go(500);
	}
}
