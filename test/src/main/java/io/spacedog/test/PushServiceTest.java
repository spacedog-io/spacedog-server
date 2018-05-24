/**
 * © David Attias 2015
 */
package io.spacedog.test;

import static org.hamcrest.collection.IsArrayContaining.hasItemInArray;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.credentials.Permission;
import io.spacedog.client.credentials.Roles;
import io.spacedog.client.data.DataSettings;
import io.spacedog.client.data.DataWrap;
import io.spacedog.client.push.BadgeStrategy;
import io.spacedog.client.push.Installation;
import io.spacedog.client.push.PushProtocol;
import io.spacedog.client.push.PushRequest;
import io.spacedog.client.push.PushResponse;
import io.spacedog.client.push.PushResponse.Notification;
import io.spacedog.client.push.PushSettings;
import io.spacedog.utils.Json;

public class PushServiceTest extends SpaceTest {

	@Test
	public void usersInstallAppAndPush() {

		// prepare
		prepareTest();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearServer();

		// prepare users
		SpaceDog dave = createTempDog(superadmin, "dave");
		SpaceDog vince = createTempDog(superadmin, "vince");
		SpaceDog fred = createTempDog(superadmin, "fred");
		SpaceDog nath = createTempDog(superadmin, "nath");

		// prepare installation schema
		superadmin.schemas().setDefault(Installation.TYPE);

		// superadmin sets data acl
		DataSettings settings = new DataSettings();
		settings.acl().put(Installation.TYPE, Roles.all, Permission.create, //
				Permission.updateMine);
		settings.acl().put(Installation.TYPE, Roles.user, Permission.create, //
				Permission.readMine, Permission.updateMine);
		settings.acl().put(Installation.TYPE, Roles.admin, Permission.create, //
				Permission.update, Permission.search, Permission.delete);
		superadmin.settings().save(settings);

		// non authenticated user installs joho
		// and fails to set endpoint fields
		String unknownInstallId = guest.push().saveInstallation(//
				new Installation().token("token-unknown").appId("joho")//
						.protocol(PushProtocol.GCM).endpoint("XXX"))
				.id();

		DataWrap<Installation> unknownInstall = superadmin.push().getInstallation(unknownInstallId);

		assertEquals("joho", unknownInstall.source().appId());
		assertEquals(PushProtocol.GCM, unknownInstall.source().protocol());
		assertEquals("token-unknown", unknownInstall.source().token());
		assertEquals("FAKE_ENDPOINT_FOR_TESTING", unknownInstall.source().endpoint());
		assertEquals(Credentials.GUEST.id(), unknownInstall.owner());
		assertTrue(unknownInstall.source().tags().isEmpty());

		// vince and fred install joho
		DataWrap<Installation> vinceInstall = installApplication("joho", PushProtocol.GCM, vince);
		DataWrap<Installation> fredInstall = installApplication("joho", PushProtocol.APNS, fred);
		DataWrap<Installation> daveInstall = installApplication("joho", PushProtocol.APNS, dave);

		// vince fails to push since no roles are yet authorized to push
		assertHttpError(403, () -> vince.push().push(new PushRequest()));

		// superadmin authorizes users to push
		PushSettings pushSettings = new PushSettings();
		pushSettings.authorizedRoles.add(Roles.user);
		superadmin.settings().save(pushSettings);

		// vince pushes a simple message to fred via installation id
		PushResponse response = vince.push().push(new PushRequest()//
				.text("coucou").installationId(fredInstall.id()).refresh(true));

		assertEquals(0, response.failures);
		assertEquals(1, response.notifications.size());
		assertEquals(fredInstall.id(), response.notifications.get(0).installationId);
		assertEquals(fred.id(), response.notifications.get(0).owner);

		// vince pushes a complex object message to dave via credentials id
		ObjectNode message = Json.object("APNS", //
				Json.object("aps", Json.object("alert", "coucou")));

		response = vince.push().push(new PushRequest()//
				.data(message).credentialsId(dave.id()));

		assertEquals(0, response.failures);
		assertEquals(1, response.notifications.size());
		assertNotificationsContains(response, daveInstall.id(), dave.id());

		// vince fails to push to invalid installation id
		response = vince.push().push(new PushRequest().installationId("XXX"));
		assertEquals(0, response.notifications.size());

		// nath installs birdee
		DataWrap<Installation> nathInstall = installApplication("birdee", PushProtocol.APNS, nath);

		// vince updates its installation
		vinceInstall.source().token("super-token-vince").appId("joho")//
				.protocol(PushProtocol.GCM);
		vince.push().saveInstallation(vinceInstall);

		vinceInstall = vince.push().getInstallation(vinceInstall.id());
		assertEquals("joho", vinceInstall.source().appId());
		assertEquals("super-token-vince", vinceInstall.source().token());
		assertEquals(vince.id(), vinceInstall.owner());
		assertTrue(vinceInstall.source().tags().isEmpty());

		// vince fails to get all installations since not admin
		assertHttpError(403, () -> vince.data()//
				.getAllRequest().type(Installation.TYPE).go());

		// admin gets all installations
		List<Installation.Wrap> installations = superadmin.data().getAllRequest()//
				.type(Installation.TYPE).refresh().go(Installation.Results.class).results;

		assertEquals(5, installations.size());
		Set<String> ids = Sets.newHashSet(unknownInstallId, daveInstall.id(), //
				vinceInstall.id(), fredInstall.id(), nathInstall.id());
		for (DataWrap<Installation> installation : installations)
			ids.contains(installation.id());

		// nath adds bonjour tag to her install
		nath.push().addTags(nathInstall.id(), "bonjour");

		String[] tags = nath.push().getTags(nathInstall.id());
		assertEquals(1, tags.length);
		assertThat(tags, hasItemInArray("bonjour"));

		// nath adds again the same tag and it changes nothing
		// there is no duplicate as a result
		nath.push().addTags(nathInstall.id(), "bonjour");

		tags = nath.push().getTags(nathInstall.id());
		assertEquals(1, tags.length);
		assertThat(tags, hasItemInArray("bonjour"));

		// vince adds bonjour tag to his install
		vince.push().addTags(vinceInstall.id(), "bonjour");

		// vince adds hi tag to his install
		vince.push().addTags(vinceInstall.id(), "hi");

		tags = vince.push().getTags(vinceInstall.id());
		assertEquals(2, tags.length);
		assertThat(tags, hasItemInArray("hi"));
		assertThat(tags, hasItemInArray("bonjour"));

		// vince deletes bonjour tag from his install
		vince.push().removeTags(vinceInstall.id(), "bonjour");

		tags = vince.push().getTags(vinceInstall.id());
		assertEquals(1, tags.length);
		assertThat(tags, hasItemInArray("hi"));

		// vince deletes hi tag from his install
		vince.push().removeTags(vinceInstall.id(), "hi");

		tags = vince.push().getTags(vinceInstall.id());
		assertEquals(0, tags.length);

		// vince sets all his install tags to bonjour and hi
		vince.push().setTags(vinceInstall.id(), "hi", "bonjour");

		tags = vince.push().getTags(vinceInstall.id());
		assertEquals(2, tags.length);
		assertThat(tags, hasItemInArray("hi"));
		assertThat(tags, hasItemInArray("bonjour"));

		// fred sets all his install tags to bonjour
		fred.push().setTags(fredInstall.id(), "bonjour");

		tags = fred.push().getTags(fredInstall.id());
		assertEquals(1, tags.length);
		assertThat(tags, hasItemInArray("bonjour"));

		// vince pushes to all joho installations
		// this means users and anonymous installations
		PushRequest pushRequest = new PushRequest().appId("joho")//
				.refresh(true).text("This is a push!");
		response = vince.push().push(pushRequest);

		assertEquals(0, response.failures);
		assertEquals(4, response.notifications.size());
		assertNotificationsContains(response, unknownInstall.id(), null);
		assertNotificationsContains(response, daveInstall.id(), dave.id());
		assertNotificationsContains(response, vinceInstall.id(), vince.id());
		assertNotificationsContains(response, fredInstall.id(), fred.id());

		// vince pushes to all joho users
		// this means excluding anonymous installations
		// refresh false since previous push has already
		// refreshed installation index
		pushRequest.refresh(false).usersOnly(true);
		response = vince.push().push(pushRequest);

		assertEquals(0, response.failures);
		assertEquals(3, response.notifications.size());
		assertNotificationsContains(response, daveInstall.id(), dave.id());
		assertNotificationsContains(response, vinceInstall.id(), vince.id());
		assertNotificationsContains(response, fredInstall.id(), fred.id());

		// vince pushes to APNS only joho users
		pushRequest.protocol(PushProtocol.APNS);
		response = vince.push().push(pushRequest);

		assertEquals(0, response.failures);
		assertEquals(2, response.notifications.size());
		assertNotificationsContains(response, daveInstall.id(), dave.id());
		assertNotificationsContains(response, fredInstall.id(), fred.id());

		// vince pushes to APNS only joho users with tag bonjour
		pushRequest.tags("bonjour");
		response = vince.push().push(pushRequest);

		assertEquals(0, response.failures);
		assertEquals(1, response.notifications.size());
		assertNotificationsContains(response, fredInstall.id(), fred.id());

		// vince pushes to all joho users with tag bonjour
		pushRequest.protocol(null);
		response = vince.push().push(pushRequest);

		assertEquals(0, response.failures);
		assertEquals(2, response.notifications.size());
		assertNotificationsContains(response, vinceInstall.id(), vince.id());
		assertNotificationsContains(response, fredInstall.id(), fred.id());

		// vince pushes to all joho users with tags bonjour and hi
		pushRequest.tags("bonjour", "hi");
		response = vince.push().push(pushRequest);

		assertEquals(0, response.failures);
		assertEquals(1, response.notifications.size());
		assertNotificationsContains(response, vinceInstall.id(), vince.id());

		// vince gets 404 when he pushes to invalid app id
		response = vince.push().push(new PushRequest().appId("XXX").text("This is a push!"));
		assertEquals(0, response.notifications.size());

		// vince can not read, update nor delete dave's installation
		assertHttpError(403, () -> vince.data().get(Installation.TYPE, daveInstall.id()));
		assertHttpError(403, () -> vince.data().save(Installation.TYPE, daveInstall.id(), //
				new Installation().appId("XXX").token("XXX").protocol(PushProtocol.GCM)));
		assertHttpError(403, () -> vince.data().save(Installation.TYPE, daveInstall.id(), "badge", 0));
		assertHttpError(403, () -> vince.data().delete(Installation.TYPE, daveInstall.id()));

		// dave can not update his installation app id
		// if he does not provide the token
		// this is also true for push service and endpoint
		assertHttpError(400, () -> dave.push().saveInstallation(daveInstall.id(), //
				new Installation().appId("joho2")));
		assertHttpError(400, () -> dave.push().saveInstallation(daveInstall.id(), //
				new Installation().protocol(PushProtocol.GCM)));
		assertHttpError(400, () -> dave.push().saveInstallation(daveInstall.id(), //
				new Installation().endpoint("XXX")));
	}

	@Test
	public void badgeManagement() {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearServer();

		// prepare users
		SpaceDog dave = createTempDog(superadmin, "dave");
		SpaceDog vince = createTempDog(superadmin, "vince");

		// prepare installation schema
		superadmin.schemas().setDefault(Installation.TYPE);

		// superadmin authorizes users and superadmins to push
		PushSettings settings = new PushSettings();
		settings.authorizedRoles.add(Roles.user);
		settings.authorizedRoles.add(Roles.superadmin);
		superadmin.settings().save(settings);

		// vince and dave install joho
		DataWrap<Installation> vinceInstall = installApplication(//
				"joho", PushProtocol.APNS, vince);
		DataWrap<Installation> daveInstall = installApplication(//
				"joho", PushProtocol.APNS, dave);

		// vince pushes a message to dave with manual badge = 3
		ObjectNode message = Json.object(PushProtocol.APNS, //
				Json.object("aps", Json.object("alert", "coucou", "badge", 3)));
		PushResponse response = vince.push().push(new PushRequest().data(message)//
				.installationId(daveInstall.id()).refresh(true));

		assertEquals(0, response.failures);
		assertEquals(1, response.notifications.size());
		assertNotificationsContains(response, daveInstall.id(), dave.id());

		// badge is not set in installation
		// since badge management is still manual
		DataWrap<Installation> installation = dave.push()//
				.getInstallation(daveInstall.id());
		assertEquals(0, installation.source().badge());

		// superadmin pushes a message to all with automatic badging
		// this means installation.badge is incremented on the server
		// and installation.badge is sent to each installation
		PushRequest pushRequest = new PushRequest().appId("joho").refresh(true)//
				.badgeStrategy(BadgeStrategy.auto).text("Badge is the new trend!");

		response = superadmin.push().push(pushRequest);

		assertEquals(0, response.failures);
		assertEquals(2, response.notifications.size());
		assertNotificationsContains(response, vinceInstall.id(), vince.id());
		assertNotificationsContains(response, daveInstall.id(), dave.id());

		// check badge is 1 in dave's installation
		installation = dave.push().getInstallation(daveInstall.id());
		assertEquals(1, installation.source().badge());

		// check badge is 1 in vince's installation
		installation = vince.push().getInstallation(vinceInstall.id());
		assertEquals(1, installation.source().badge());

		// vince reads the push and resets its installation badge
		vince.push().saveInstallationField(vinceInstall.id(), "badge", 0);

		installation = vince.push().getInstallation(vinceInstall.id());
		assertEquals(0, installation.source().badge());

		// superadmin pushes again to all with automatic badging
		response = superadmin.push().push(pushRequest);

		assertEquals(0, response.failures);
		assertEquals(2, response.notifications.size());
		assertNotificationsContains(response, vinceInstall.id(), vince.id());
		assertNotificationsContains(response, daveInstall.id(), dave.id());

		// check badge is 2 in dave's installation
		installation = dave.push().getInstallation(daveInstall.id());
		assertEquals(2, installation.source().badge());

		// check badge is 1 in vince's installation
		installation = vince.push().getInstallation(vinceInstall.id());
		assertEquals(1, installation.source().badge());

		// superadmin pushes again to all but with semi automatic badging
		pushRequest.badgeStrategy(BadgeStrategy.semi);
		response = superadmin.push().push(pushRequest);

		assertEquals(0, response.failures);
		assertEquals(2, response.notifications.size());
		assertNotificationsContains(response, vinceInstall.id(), vince.id());
		assertNotificationsContains(response, daveInstall.id(), dave.id());

		// check badge is 2 in dave's installation
		installation = dave.push().getInstallation(daveInstall.id());
		assertEquals(2, installation.source().badge());

		// check badge is 1 in vince's installation
		installation = vince.push().getInstallation(vinceInstall.id());
		assertEquals(1, installation.source().badge());
	}

	private DataWrap<Installation> installApplication(String appId, PushProtocol protocol, SpaceDog user) {

		String installId = user.push().saveInstallation(new Installation()//
				.appId(appId).token("token-" + user.username())//
				.protocol(protocol)).id();

		DataWrap<Installation> installation = user.push().getInstallation(installId);

		assertEquals(appId, installation.source().appId());
		assertEquals(protocol, installation.source().protocol());
		assertEquals("token-" + user.username(), installation.source().token());
		assertEquals(user.id(), installation.owner());
		assertTrue(installation.source().tags().isEmpty());

		return installation;
	}

	private void assertNotificationsContains(PushResponse response, String installationId, String owner) {
		for (Notification notification : response.notifications) {

			if (notification.installationId.equals(installationId) //
					&& (owner == notification.owner //
							|| (owner != null && owner.equals(notification.owner))))
				return;
		}
		failure("no notification to installation [%s] owned by [%s]", installationId, owner);
	}
}
