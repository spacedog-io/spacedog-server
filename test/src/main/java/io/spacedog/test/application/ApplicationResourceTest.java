/**
 * © David Attias 2015
 */
package io.spacedog.test.application;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.ClassResources;
import io.spacedog.utils.Json7;

public class ApplicationResourceTest extends SpaceTest {

	public void shouldSetAndDeleteApplicationPushCredentials() throws InterruptedException {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		ObjectNode apnsCredentials = Json7.object(//
				"principal", ClassResources.loadToString(this, "apns-principal.pem"), //
				"credentials", ClassResources.loadToString(this, "apns-credentials.pem"));

		// put fails since invalid push service
		superadmin.put("/1/applications/myapp/XXX").bodyJson(apnsCredentials).go(400);

		// put fails since no push credentials
		superadmin.put("/1/applications/myapp/APNS").go(400);

		// superadmin sets test-myapp APNS push credentials
		superadmin.put("/1/applications/myapp/APNS").bodyJson(apnsCredentials).go(200);

		// superadmin lists all backend push apps
		// wait 5 seconds to make sure previous request has propagated
		Thread.sleep(5000);
		superadmin.get("/1/applications").go(200)//
				.assertSizeEquals(1)//
				.assertEquals("test", "0.backendId")//
				.assertEquals("myapp", "0.name")//
				.assertEquals("APNS", "0.service")//
				.assertEquals("true", "0.attributes.Enabled")//
				.assertPresent("0.attributes.AppleCertificateExpirationDate");

		// superadmin sets test-myapp APNS push credentials again
		superadmin.put("/1/applications/myapp/APNS").bodyJson(apnsCredentials).go(200);

		// delete fails since invalid push service
		superadmin.delete("/1/applications/myapp/XXX").go(400);

		// superadmin deletes test-myapp application APNS credentials
		superadmin.delete("/1/applications/myapp/APNS").go(200);

		// superadmin lists all backend push apps
		// wait 5 seconds to make sure previous request has propagated
		Thread.sleep(5000);
		superadmin.get("/1/applications").go(200)//
				.assertSizeEquals(0);

		// deleting non existing credentials succeeds
		superadmin.delete("/1/applications/myapp/GCM").go(200);
	}
}
