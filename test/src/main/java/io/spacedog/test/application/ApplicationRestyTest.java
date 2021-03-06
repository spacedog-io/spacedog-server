/**
 * © David Attias 2015
 */
package io.spacedog.test.application;

import java.util.List;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.push.PushApplication;
import io.spacedog.client.push.PushApplication.Credentials;
import io.spacedog.client.push.PushProtocol;
import io.spacedog.test.SpaceTest;
import io.spacedog.utils.ClassResources;

public class ApplicationRestyTest extends SpaceTest {

	// @Test
	public void shouldSetAndDeleteApplicationPushCredentials() throws InterruptedException {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearServer();

		// put fails since invalid push service
		assertHttpError(400, () -> superadmin.push().saveApp("myapp", "XXX", new Credentials()));

		// put fails since no push credentials
		assertHttpError(400, () -> superadmin.push().saveApp("myapp", "APNS", new Credentials()));

		// superadmin sets test-myapp APNS push credentials
		PushApplication app = new PushApplication().name("myapp").protocol(PushProtocol.APNS)//
				.principal(ClassResources.loadAsString(this, "apns-principal.pem"))//
				.credentials(ClassResources.loadAsString(this, "apns-credentials.pem"));

		superadmin.push().saveApp(app);

		// superadmin lists all backend push apps
		// retry since app save request is asynch
		List<PushApplication> apps = retry(5, 3000, () -> {
			List<PushApplication> list = superadmin.push().listApps();
			assertEquals(1, list.size());
			return list;
		});

		assertEquals("spacedog", apps.get(0).backendId);
		assertEquals("myapp", apps.get(0).name);
		assertEquals("APNS", apps.get(0).protocol.toString());
		assertEquals("true", apps.get(0).attributes.get("Enabled"));
		assertNotNull(apps.get(0).attributes.get("AppleCertificateExpirationDate"));

		// superadmin sets test-myapp APNS push credentials again
		superadmin.push().saveApp(app);

		// delete fails since invalid push service
		assertHttpError(400, () -> superadmin.push().deleteApp("myapp", "XXX"));

		// superadmin deletes myapp application APNS credentials
		superadmin.push().deleteApp(app);

		// superadmin lists all backend push apps
		// retry since app delete request is asynch
		retry(5, 3000, () -> {
			List<PushApplication> list = superadmin.push().listApps();
			assertEquals(0, list.size());
			return list;
		});

		// deleting non existing credentials succeeds
		superadmin.push().deleteApp("myapp", "GCM");
	}
}
