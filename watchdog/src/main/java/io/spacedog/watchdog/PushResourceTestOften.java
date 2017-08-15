/**
 * © David Attias 2015
 */
package io.spacedog.watchdog;

import static org.hamcrest.collection.IsArrayContaining.hasItemInArray;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;

import io.spacedog.model.BadgeStrategy;
import io.spacedog.model.DataPermission;
import io.spacedog.model.Installation;
import io.spacedog.model.PushService;
import io.spacedog.model.Schema;
import io.spacedog.model.Schema.DataAcl;
import io.spacedog.rest.SpaceTest;
import io.spacedog.sdk.PushRequest;
import io.spacedog.sdk.SpaceDog;
import io.spacedog.utils.Json7;

public class PushResourceTestOften extends SpaceTest {

	private static final String PUSHED_TO = "pushedTo";
	private static final String FAILURES = "failures";
	private static final String ID = "id";
	private static final String INSTALLATION_ID = "installationId";
	private static final String TEXT = "text";
	private static final String CREDENTIALS_ID = "credentialsId";
	private static final String ENDPOINT = "endpoint";
	private static final String TOKEN = "token";
	private static final String BADGE = "badge";
	private static final String PUSH_SERVICE = "pushService";
	private static final String APP_ID = "appId";

	@Test
	public void usersInstallAppAndPush() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();
		SpaceDog guest = SpaceDog.backend(superadmin);

		// prepare users
		SpaceDog dave = signUp(guest, "dave", "hi dave");
		SpaceDog vince = signUp(guest, "vince", "hi vince");
		SpaceDog fred = signUp(guest, "fred", "hi fred");
		SpaceDog nath = signUp(guest, "nath", "hi nath");

		// prepare installation schema
		superadmin.schema().setDefault("installation");

		// add create permission to guest requests
		Schema schema = superadmin.schema().get("installation");
		schema.acl(new DataAcl()//
				.set("all", DataPermission.create, DataPermission.update)//
				.set("user", DataPermission.create, DataPermission.read, DataPermission.update)//
				.set("admin", DataPermission.create, DataPermission.update_all, //
						DataPermission.search, DataPermission.delete_all));
		superadmin.schema().set(schema);

		// non authenticated user installs joho
		// and fails to set installation userId and endpoint fields
		String unknownInstallId = guest.post("/1/installation")//
				.bodyJson(TOKEN, "token-unknown", APP_ID, "joho", //
						PUSH_SERVICE, PushService.GCM, //
						CREDENTIALS_ID, "XXX", ENDPOINT, "XXX")//
				.go(201).getString(ID);

		Installation install = superadmin.push().getInstallation(unknownInstallId);

		assertEquals("joho", install.appId());
		assertEquals(PushService.GCM, install.pushService());
		assertEquals("token-unknown", install.token());
		assertEquals("FAKE_ENDPOINT_FOR_TESTING", install.endpoint());
		assertNull(install.credentialsId());
		assertTrue(install.tags().isEmpty());

		// vince and fred install joho
		String vinceInstallId = installApplication("joho", PushService.GCM, vince);
		String fredInstallId = installApplication("joho", PushService.APNS, fred);
		String daveInstallId = installApplication("joho", PushService.APNS, dave);

		// vince pushes a simple message to fred
		ObjectNode response = vince.push().push(fredInstallId, //
				new PushRequest().text("coucou"));

		Json7.assertNode(response)//
				.assertEquals(fredInstallId, "pushedTo.0.installationId")//
				.assertEquals(fred.id(), "pushedTo.0.credentialsId");

		// vince pushes a complex object message to dave
		ObjectNode message = Json7.object("APNS", //
				Json7.object("aps", Json7.object("alert", "coucou")));

		response = vince.push().push(daveInstallId, //
				new PushRequest().data(message));

		// vince fails to push to invalid installation id
		vince.post("/1/installation/XXX/push").bodyJson(TEXT, "coucou").go(404);

		// nath installs birdee
		String nathInstallId = installApplication("birdee", PushService.APNS, nath);

		// vince updates its installation
		vince.push().getInstallation(vinceInstallId).token("super-token-vince")//
				.appId("joho").pushService(PushService.GCM).save();

		install = vince.push().getInstallation(vinceInstallId);
		assertEquals("joho", install.appId());
		assertEquals("super-token-vince", install.token());
		assertEquals(vince.id(), install.credentialsId());
		assertTrue(install.tags().isEmpty());

		// vince fails to get all installations since not admin
		vince.get("/1/installation").go(403);

		// admin gets all installations
		List<Installation> installations = superadmin.data().getAll().type("installation")//
				.refresh().get(Installation.class);

		assertEquals(5, installations.size());
		Set<String> ids = Sets.newHashSet(unknownInstallId, daveInstallId, //
				vinceInstallId, fredInstallId, nathInstallId);
		for (Installation installation : installations)
			ids.contains(installation.id());

		// nath adds bonjour tag to her install
		nath.push().addTag(nathInstallId, "bonjour");

		String[] tags = nath.push().getTags(nathInstallId);
		assertEquals(1, tags.length);
		assertThat(tags, hasItemInArray("bonjour"));

		// nath adds again the same tag and it changes nothing
		// there is no duplicate as a result
		nath.push().addTag(nathInstallId, "bonjour");

		tags = nath.push().getTags(nathInstallId);
		assertEquals(1, tags.length);
		assertThat(tags, hasItemInArray("bonjour"));

		// vince adds bonjour tag to his install
		vince.push().addTag(vinceInstallId, "bonjour");

		// vince adds hi tag to his install
		vince.push().addTag(vinceInstallId, "hi");

		tags = vince.push().getTags(vinceInstallId);
		assertEquals(2, tags.length);
		assertThat(tags, hasItemInArray("hi"));
		assertThat(tags, hasItemInArray("bonjour"));

		// vince deletes bonjour tag from his install
		vince.push().deleteTag(vinceInstallId, "bonjour");

		tags = vince.push().getTags(vinceInstallId);
		assertEquals(1, tags.length);
		assertThat(tags, hasItemInArray("hi"));

		// vince deletes hi tag from his install
		vince.push().deleteTag(vinceInstallId, "hi");

		tags = vince.push().getTags(vinceInstallId);
		assertEquals(0, tags.length);

		// vince sets all his install tags to bonjour and hi
		vince.push().setTags(vinceInstallId, "hi", "bonjour");

		tags = vince.push().getTags(vinceInstallId);
		assertEquals(2, tags.length);
		assertThat(tags, hasItemInArray("hi"));
		assertThat(tags, hasItemInArray("bonjour"));

		// fred sets all his install tags to bonjour
		fred.push().setTags(fredInstallId, "bonjour");

		tags = fred.push().getTags(fredInstallId);
		assertEquals(1, tags.length);
		assertThat(tags, hasItemInArray("bonjour"));

		// vince pushes to all joho installations
		// this means users and anonymous installations
		PushRequest pushRequest = new PushRequest().appId("joho")//
				.refresh(true).text("This is a push!");
		response = vince.push().push(pushRequest);

		Json7.assertNode(response)//
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

		Json7.assertNode(response)//
				.assertFalse(FAILURES)//
				.assertEquals(3, PUSHED_TO)//
				.assertContainsValue(dave.id(), CREDENTIALS_ID)//
				.assertContainsValue(vince.id(), CREDENTIALS_ID)//
				.assertContainsValue(fred.id(), CREDENTIALS_ID);

		// vince pushes to APNS only joho users
		pushRequest.pushService(PushService.APNS);
		response = vince.push().push(pushRequest);

		Json7.assertNode(response)//
				.assertFalse(FAILURES)//
				.assertEquals(2, PUSHED_TO)//
				.assertContainsValue(dave.id(), CREDENTIALS_ID)//
				.assertContainsValue(fred.id(), CREDENTIALS_ID);

		// vince pushes to APNS only joho users with tag bonjour
		pushRequest.tags("bonjour");
		response = vince.push().push(pushRequest);

		Json7.assertNode(response)//
				.assertFalse(FAILURES)//
				.assertEquals(1, PUSHED_TO)//
				.assertContainsValue(fred.id(), CREDENTIALS_ID);

		// vince pushes to all joho users with tag bonjour
		pushRequest.pushService(null);
		response = vince.push().push(pushRequest);

		Json7.assertNode(response)//
				.assertFalse(FAILURES)//
				.assertEquals(2, PUSHED_TO)//
				.assertContainsValue(vince.id(), CREDENTIALS_ID)//
				.assertContainsValue(fred.id(), CREDENTIALS_ID);

		// vince pushes to all joho users with tags bonjour and hi
		pushRequest.tags("bonjour", "hi");
		response = vince.push().push(pushRequest);

		Json7.assertNode(response)//
				.assertFalse(FAILURES)//
				.assertEquals(1, PUSHED_TO)//
				.assertContainsValue(vince.id(), CREDENTIALS_ID);

		// vince gets 404 when he pushes to invalid app id
		pushRequest = new PushRequest().appId("XXX").text("This is a push!");
		vince.post("/1/push").bodyPojo(pushRequest).go(404);

		// vince can not read, update nor delete dave's installation
		vince.get("/1/installation/" + daveInstallId).go(403);
		vince.put("/1/installation/" + daveInstallId)//
				.bodyJson(APP_ID, "XXX", TOKEN, "XXX", PUSH_SERVICE, "GCM").go(403);
		vince.delete("/1/installation/" + daveInstallId).go(403);

		// also true with /data/installation route
		vince.get("/1/data/installation/" + daveInstallId).go(403);
		vince.put("/1/data/installation/" + daveInstallId + "/badge")//
				.bodyJson(IntNode.valueOf(0)).go(403);
		vince.delete("/1/data/installation/" + daveInstallId).go(403);

		// dave can not update his installation app id
		// if he does not provide the token
		// this is also true for push service and endpoint
		dave.put("/1/installation/" + daveInstallId).bodyJson(APP_ID, "joho2").go(400);
		dave.put("/1/installation/" + daveInstallId).bodyJson(PUSH_SERVICE, "GCM").go(400);
		dave.put("/1/installation/" + daveInstallId).bodyJson(ENDPOINT, "XXX").go(400);
	}

	@Test
	public void badgeManagement() {

		// prepare
		prepareTest();
		SpaceDog superadmin = resetTestBackend();

		// prepare users
		SpaceDog dave = signUp(superadmin, "dave", "hi dave");
		SpaceDog vince = signUp(superadmin, "vince", "hi vince");

		// prepare installation schema
		superadmin.schema().setDefault("installation");

		// vince and dave install joho
		String vinceInstallId = installApplication("joho", PushService.APNS, vince);
		String daveInstallId = installApplication("joho", PushService.APNS, dave);

		// vince pushes a message to dave with manual badge = 3
		ObjectNode message = Json7.object(PushService.APNS, //
				Json7.object("aps", Json7.object("alert", "coucou", BADGE, 3)));
		ObjectNode response = vince.push().push(daveInstallId, //
				new PushRequest().data(message));

		Json7.assertNode(response)//
				.assertEquals(daveInstallId, "pushedTo.0.installationId");

		// badge is not set in installation
		// since badge management is still manual
		Installation installation = dave.push().getInstallation(daveInstallId);
		assertEquals(0, installation.badge());

		// vince pushes a message to all with automatic badging
		// this means installation.badge is incremented on the server
		// and installation.badge is sent to each installation
		PushRequest pushRequest = new PushRequest().appId("joho").refresh(true)//
				.badgeStrategy(BadgeStrategy.auto).text("Badge is the new trend!");

		response = superadmin.push().push(pushRequest);

		Json7.assertNode(response)//
				.assertSizeEquals(2, PUSHED_TO)//
				.assertContainsValue(vinceInstallId, INSTALLATION_ID)//
				.assertContainsValue(daveInstallId, INSTALLATION_ID);

		// check badge is 1 in dave's installation
		installation = dave.push().getInstallation(daveInstallId);
		assertEquals(1, installation.badge());

		// check badge is 1 in vince's installation
		installation = vince.push().getInstallation(vinceInstallId);
		assertEquals(1, installation.badge());

		// vince reads the push and resets its installation badge
		vince.data().save("installation", vinceInstallId, "badge", 0);

		installation = vince.push().getInstallation(vinceInstallId);
		assertEquals(0, installation.badge());

		// admin pushes again to all with automatic badging
		response = superadmin.push().push(pushRequest);

		Json7.assertNode(response)//
				.assertSizeEquals(2, PUSHED_TO);

		// check badge is 2 in dave's installation
		installation = dave.push().getInstallation(daveInstallId);
		assertEquals(2, installation.badge());

		// check badge is 1 in vince's installation
		installation = vince.push().getInstallation(vinceInstallId);
		assertEquals(1, installation.badge());

		// admin pushes again to all but with semi automatic badging
		pushRequest.badgeStrategy(BadgeStrategy.semi);
		response = superadmin.push().push(pushRequest);

		Json7.assertNode(response)//
				.assertSizeEquals(2, PUSHED_TO);

		// check badge is 2 in dave's installation
		installation = dave.push().getInstallation(daveInstallId);
		assertEquals(2, installation.badge());

		// check badge is 1 in vince's installation
		installation = vince.push().getInstallation(vinceInstallId);
		assertEquals(1, installation.badge());
	}

	private String installApplication(String appId, PushService pushService, SpaceDog user) {

		String installId = user.push().newInstallation().appId(appId)//
				.token("token-" + user.username())//
				.pushService(pushService).save().id();

		Installation installation = user.push().getInstallation(installId);

		assertEquals(appId, installation.appId());
		assertEquals(pushService, installation.pushService());
		assertEquals("token-" + user.username(), installation.token());
		assertEquals(user.id(), installation.credentialsId());
		assertTrue(installation.tags().isEmpty());

		return installId;
	}
}
