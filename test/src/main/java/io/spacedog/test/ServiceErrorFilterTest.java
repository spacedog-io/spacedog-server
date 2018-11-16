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
		SpaceRequest.get("/2/toto")//
				.backend(superadmin.backend())//
				.go(404).asVoid()//
				.assertEquals("[path][/2/toto] not found", "error.message");

		// should fail to use this method for this valid route
		superadmin.put("/2/credentials/_login").go(405).asVoid()//
				.assertEquals("[PUT][/2/credentials/_login] is not yet supported", "error.message");
	}

	@Test
	public void notifySuperdogsForInternalServerErrors() {

		// prepare
		prepareTest();

		// this fails and send notification to superdogs with error details
		SpaceRequest.post("/2/admin/_return_500").go(500).asVoid();
	}
}
