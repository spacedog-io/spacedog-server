/**
 * © David Attias 2015
 */
package io.spacedog.test;

import static org.hamcrest.collection.IsArrayContaining.hasItemInArray;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;

import io.spacedog.client.PushRequest;
import io.spacedog.client.SpaceDog;
import io.spacedog.model.BadgeStrategy;
import io.spacedog.model.DataObject;
import io.spacedog.model.Installation;
import io.spacedog.model.InstallationDataObject;
import io.spacedog.model.Permission;
import io.spacedog.model.PushProtocol;
import io.spacedog.model.Schema;
import io.spacedog.utils.Json;
import io.spacedog.utils.Roles;

public class PushServiceTest extends SpaceTest {

	private static final String PUSHED_TO = "pushedTo";
	private static final String FAILURES = "failures";
	private static final String ID = "id";
	private static final String INSTALLATION_ID = "installationId";
	private static final String TEXT = "text";
	private static final String CREDENTIALS_ID = "credentialsId";
	private static final String ENDPOINT = "endpoint";
	private static final String TOKEN = "token";
	private static final String BADGE = "badge";
	private static final String PROTOCOL = "protocol";
	private static final String APP_ID = "appId";

	@Test
	public void usersInstallAppAndPush() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		SpaceDog guest = SpaceDog.backend(superadmin.backend());

		// prepare users
		SpaceDog dave = createTempDog(superadmin, "dave");
		SpaceDog vince = createTempDog(superadmin, "vince");
		SpaceDog fred = createTempDog(superadmin, "fred");
		SpaceDog nath = createTempDog(superadmin, "nath");

		// prepare installation schema
		superadmin.schemas().setDefault("installation");

		// add create permission to guest requests
		Schema schema = superadmin.schemas().get("installation");
		schema.acl(Roles.all, Permission.create, Permission.updateMine);
		schema.acl(Roles.user, Permission.create, Permission.readMine, Permission.updateMine);
		schema.acl(Roles.admin, Permission.create, Permission.update, //
				Permission.search, Permission.delete);
		superadmin.schemas().set(schema);

		// non authenticated user installs joho
		// and fails to set installation userId and endpoint fields
		String unknownInstallId = guest.post("/1/installation")//
				.bodyJson(TOKEN, "token-unknown", APP_ID, "joho", //
						PROTOCOL, PushProtocol.GCM, //
						CREDENTIALS_ID, "XXX", ENDPOINT, "XXX")//
				.go(201).getString(ID);

		DataObject<Installation> unknownInstall = superadmin.push().getInstallation(unknownInstallId);

		assertEquals("joho", unknownInstall.source().appId());
		assertEquals(PushProtocol.GCM, unknownInstall.source().protocol());
		assertEquals("token-unknown", unknownInstall.source().token());
		assertEquals("FAKE_ENDPOINT_FOR_TESTING", unknownInstall.source().endpoint());
		assertNull(unknownInstall.source().credentialsId());
		assertTrue(unknownInstall.source().tags().isEmpty());

		// vince and fred install joho
		DataObject<Installation> vinceInstall = installApplication("joho", PushProtocol.GCM, vince);
		DataObject<Installation> fredInstall = installApplication("joho", PushProtocol.APNS, fred);
		DataObject<Installation> daveInstall = installApplication("joho", PushProtocol.APNS, dave);

		// vince pushes a simple message to fred
		ObjectNode response = vince.push().push(fredInstall.id(), //
				new PushRequest().text("coucou"));

		Json.assertNode(response)//
				.assertEquals(fredInstall.id(), "pushedTo.0.installationId")//
				.assertEquals(fred.id(), "pushedTo.0.credentialsId");

		// vince pushes a complex object message to dave
		ObjectNode message = Json.object("APNS", //
				Json.object("aps", Json.object("alert", "coucou")));

		response = vince.push().push(daveInstall.id(), //
				new PushRequest().data(message));

		// vince fails to push to invalid installation id
		vince.post("/1/installation/XXX/push").bodyJson(TEXT, "coucou").go(404);

		// nath installs birdee
		DataObject<Installation> nathInstall = installApplication("birdee", PushProtocol.APNS, nath);

		// vince updates its installation
		vinceInstall.source().token("super-token-vince").appId("joho")//
				.protocol(PushProtocol.GCM);
		vince.push().saveInstallation(vinceInstall);

		vinceInstall = vince.push().getInstallation(vinceInstall.id());
		assertEquals("joho", vinceInstall.source().appId());
		assertEquals("super-token-vince", vinceInstall.source().token());
		assertEquals(vince.id(), vinceInstall.source().credentialsId());
		assertTrue(vinceInstall.source().tags().isEmpty());

		// vince fails to get all installations since not admin
		vince.get("/1/installation").go(403);

		// admin gets all installations
		List<InstallationDataObject> installations = superadmin.data().getAllRequest()//
				.type("installation").refresh().go(InstallationDataObject.Results.class).results;

		assertEquals(5, installations.size());
		Set<String> ids = Sets.newHashSet(unknownInstallId, daveInstall.id(), //
				vinceInstall.id(), fredInstall.id(), nathInstall.id());
		for (DataObject<Installation> installation : installations)
			ids.contains(installation.id());

		// nath adds bonjour tag to her install
		nath.push().addTag(nathInstall.id(), "bonjour");

		String[] tags = nath.push().getTags(nathInstall.id());
		assertEquals(1, tags.length);
		assertThat(tags, hasItemInArray("bonjour"));

		// nath adds again the same tag and it changes nothing
		// there is no duplicate as a result
		nath.push().addTag(nathInstall.id(), "bonjour");

		tags = nath.push().getTags(nathInstall.id());
		assertEquals(1, tags.length);
		assertThat(tags, hasItemInArray("bonjour"));

		// vince adds bonjour tag to his install
		vince.push().addTag(vinceInstall.id(), "bonjour");

		// vince adds hi tag to his install
		vince.push().addTag(vinceInstall.id(), "hi");

		tags = vince.push().getTags(vinceInstall.id());
		assertEquals(2, tags.length);
		assertThat(tags, hasItemInArray("hi"));
		assertThat(tags, hasItemInArray("bonjour"));

		// vince deletes bonjour tag from his install
		vince.push().deleteTag(vinceInstall.id(), "bonjour");

		tags = vince.push().getTags(vinceInstall.id());
		assertEquals(1, tags.length);
		assertThat(tags, hasItemInArray("hi"));

		// vince deletes hi tag from his install
		vince.push().deleteTag(vinceInstall.id(), "hi");

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

		Json.assertNode(response)//
				.assertFalse(FAILURES)//
				.assertEquals(4, PUSHED_TO)//
				.assertContainsValue(unknownInstallId, INSTALLATION_ID)//
				.assertContainsValue(dave.id(), CREDENTIALS_ID)//
				.assertContainsValue(vince.id(), CREDENTIALS_ID)//
				.assertContainsValue(fred.id(), CREDENTIALS_ID);

		// vince pushes to all joho users
		// this means excluding anonymous installations
		// refresh false since previous push has already
		// refreshed installation index
		pushRequest.refresh(false).usersOnly(true);
		response = vince.push().push(pushRequest);

		Json.assertNode(response)//
				.assertFalse(FAILURES)//
				.assertEquals(3, PUSHED_TO)//
				.assertContainsValue(dave.id(), CREDENTIALS_ID)//
				.assertContainsValue(vince.id(), CREDENTIALS_ID)//
				.assertContainsValue(fred.id(), CREDENTIALS_ID);

		// vince pushes to APNS only joho users
		pushRequest.protocol(PushProtocol.APNS);
		response = vince.push().push(pushRequest);

		Json.assertNode(response)//
				.assertFalse(FAILURES)//
				.assertEquals(2, PUSHED_TO)//
				.assertContainsValue(dave.id(), CREDENTIALS_ID)//
				.assertContainsValue(fred.id(), CREDENTIALS_ID);

		// vince pushes to APNS only joho users with tag bonjour
		pushRequest.tags("bonjour");
		response = vince.push().push(pushRequest);

		Json.assertNode(response)//
				.assertFalse(FAILURES)//
				.assertEquals(1, PUSHED_TO)//
				.assertContainsValue(fred.id(), CREDENTIALS_ID);

		// vince pushes to all joho users with tag bonjour
		pushRequest.protocol(null);
		response = vince.push().push(pushRequest);

		Json.assertNode(response)//
				.assertFalse(FAILURES)//
				.assertEquals(2, PUSHED_TO)//
				.assertContainsValue(vince.id(), CREDENTIALS_ID)//
				.assertContainsValue(fred.id(), CREDENTIALS_ID);

		// vince pushes to all joho users with tags bonjour and hi
		pushRequest.tags("bonjour", "hi");
		response = vince.push().push(pushRequest);

		Json.assertNode(response)//
				.assertFalse(FAILURES)//
				.assertEquals(1, PUSHED_TO)//
				.assertContainsValue(vince.id(), CREDENTIALS_ID);

		// vince gets 404 when he pushes to invalid app id
		pushRequest = new PushRequest().appId("XXX").text("This is a push!");
		vince.post("/1/push").bodyPojo(pushRequest).go(404);

		// vince can not read, update nor delete dave's installation
		vince.get("/1/installation/" + daveInstall.id()).go(403);
		vince.put("/1/installation/" + daveInstall.id())//
				.bodyJson(APP_ID, "XXX", TOKEN, "XXX", PROTOCOL, "GCM").go(403);
		vince.delete("/1/installation/" + daveInstall.id()).go(403);

		// also true with /data/installation route
		vince.get("/1/data/installation/" + daveInstall.id()).go(403);
		vince.put("/1/data/installation/" + daveInstall.id() + "/badge")//
				.bodyJson(IntNode.valueOf(0)).go(403);
		vince.delete("/1/data/installation/" + daveInstall.id()).go(403);

		// dave can not update his installation app id
		// if he does not provide the token
		// this is also true for push service and endpoint
		dave.put("/1/installation/" + daveInstall.id()).bodyJson(APP_ID, "joho2").go(400);
		dave.put("/1/installation/" + daveInstall.id()).bodyJson(PROTOCOL, "GCM").go(400);
		dave.put("/1/installation/" + daveInstall.id()).bodyJson(ENDPOINT, "XXX").go(400);
	}

	@Test
	public void badgeManagement() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();

		// prepare users
		SpaceDog dave = createTempDog(superadmin, "dave");
		SpaceDog vince = createTempDog(superadmin, "vince");

		// prepare installation schema
		superadmin.schemas().setDefault("installation");

		// vince and dave install joho
		DataObject<Installation> vinceInstall = installApplication("joho", PushProtocol.APNS, vince);
		DataObject<Installation> daveInstall = installApplication("joho", PushProtocol.APNS, dave);

		// vince pushes a message to dave with manual badge = 3
		ObjectNode message = Json.object(PushProtocol.APNS, //
				Json.object("aps", Json.object("alert", "coucou", BADGE, 3)));
		ObjectNode response = vince.push().push(daveInstall.id(), //
				new PushRequest().data(message));

		Json.assertNode(response)//
				.assertEquals(daveInstall.id(), "pushedTo.0.installationId");

		// badge is not set in installation
		// since badge management is still manual
		DataObject<Installation> installation = dave.push().getInstallation(daveInstall.id());
		assertEquals(0, installation.source().badge());

		// vince pushes a message to all with automatic badging
		// this means installation.badge is incremented on the server
		// and installation.badge is sent to each installation
		PushRequest pushRequest = new PushRequest().appId("joho").refresh(true)//
				.badgeStrategy(BadgeStrategy.auto).text("Badge is the new trend!");

		response = superadmin.push().push(pushRequest);

		Json.assertNode(response)//
				.assertSizeEquals(2, PUSHED_TO)//
				.assertContainsValue(vinceInstall.id(), INSTALLATION_ID)//
				.assertContainsValue(daveInstall.id(), INSTALLATION_ID);

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

		// admin pushes again to all with automatic badging
		response = superadmin.push().push(pushRequest);

		Json.assertNode(response)//
				.assertSizeEquals(2, PUSHED_TO);

		// check badge is 2 in dave's installation
		installation = dave.push().getInstallation(daveInstall.id());
		assertEquals(2, installation.source().badge());

		// check badge is 1 in vince's installation
		installation = vince.push().getInstallation(vinceInstall.id());
		assertEquals(1, installation.source().badge());

		// admin pushes again to all but with semi automatic badging
		pushRequest.badgeStrategy(BadgeStrategy.semi);
		response = superadmin.push().push(pushRequest);

		Json.assertNode(response)//
				.assertSizeEquals(2, PUSHED_TO);

		// check badge is 2 in dave's installation
		installation = dave.push().getInstallation(daveInstall.id());
		assertEquals(2, installation.source().badge());

		// check badge is 1 in vince's installation
		installation = vince.push().getInstallation(vinceInstall.id());
		assertEquals(1, installation.source().badge());
	}

	private DataObject<Installation> installApplication(String appId, PushProtocol protocol, SpaceDog user) {

		String installId = user.push().saveInstallation(new Installation()//
				.appId(appId).token("token-" + user.username())//
				.protocol(protocol)).id();

		DataObject<Installation> installation = user.push().getInstallation(installId);

		assertEquals(appId, installation.source().appId());
		assertEquals(protocol, installation.source().protocol());
		assertEquals("token-" + user.username(), installation.source().token());
		assertEquals(user.id(), installation.source().credentialsId());
		assertTrue(installation.source().tags().isEmpty());

		return installation;
	}
}
