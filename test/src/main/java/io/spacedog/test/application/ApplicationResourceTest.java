/**
 * © David Attias 2015
 */
package io.spacedog.test.application;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.ClassResources;
import io.spacedog.utils.Json;

public class ApplicationResourceTest extends SpaceTest {

	@Test
	public void shouldSetAndDeleteApplicationPushCredentials() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		ObjectNode apnsCredentials = Json.object("principal", //
				ClassResources.loadToString(this, "apns-principal.pem"), //
				"credentials", //
				ClassResources.loadToString(this, "apns-credentials.pem"));

		// put fails since invalid push service
		superadmin.put("/1/applications/myapp/XXX").bodyJson(apnsCredentials).go(400);

		// put fails since no push credentials
		superadmin.put("/1/applications/myapp/APNS").go(400);

		// superadmin sets test-myapp APNS push credentials
		superadmin.put("/1/applications/myapp/APNS").bodyJson(apnsCredentials).go(200);

		// delete fails since invalid push service
		superadmin.delete("/1/applications/myapp/XXX").go(400);

		// superadmin deletes test-myapp application APNS credentials
		superadmin.delete("/1/applications/myapp/APNS").go(200);

		// deleting non existing credentials succeeds
		superadmin.delete("/1/applications/myapp/GCM").go(200);
	}
}
