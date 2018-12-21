package io.spacedog.test.credentials;

import java.util.Set;

import org.junit.Test;

import com.google.common.collect.Sets;

import io.spacedog.client.SpaceDog;
import io.spacedog.test.SpaceTest;

public class CredentialsGroupsRestyTest extends SpaceTest {

	@Test
	public void createAndShareGroups() {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearServer();

		// superadmin creates fred, vince and nath credentials
		SpaceDog fred = createTempDog(superadmin, "fred");
		SpaceDog vince = createTempDog(superadmin, "vince");
		SpaceDog nath = createTempDog(superadmin, "nath");

		// At first they all have their own default group
		checkDogGroups(fred);
		checkDogGroups(vince);
		checkDogGroups(nath);

		// fred creates a paris group
		String fredParisGroup = fred.credentials().createGroup("paris");
		checkDogGroups(fred, fredParisGroup);

		// if fred reuses the same suffix to create a group
		// it returns the same group
		String fredParisGroup2 = fred.credentials().createGroup("paris");
		assertEquals(fredParisGroup, fredParisGroup2);
		checkDogGroups(fred, fredParisGroup);

		// fred shares his paris group with vince
		fred.credentials().shareGroup(vince.id(), fredParisGroup);
		checkDogGroups(vince, fredParisGroup);

		// vince creates another paris group
		String vinceParisGroup = vince.credentials().createGroup("paris");
		assertFalse(fredParisGroup.equals(vinceParisGroup));
		checkDogGroups(vince, fredParisGroup, vinceParisGroup);

		// vince shares his paris group with fred
		vince.credentials().shareGroup(fred.id(), vinceParisGroup);
		checkDogGroups(fred, fredParisGroup, vinceParisGroup);

		// nath fails to share fred's paris group with herself
		assertHttpError(403, () -> nath.credentials().shareGroup(nath.id(), fredParisGroup));

		// vince fails to share fred's paris group with nath
		// even if it is part of his groups, he doesn't own it
		assertHttpError(403, () -> vince.credentials().shareGroup(nath.id(), fredParisGroup));

		// only fred can share the groups he owns with nath
		fred.credentials().shareGroup(nath.id(), fredParisGroup);
		checkDogGroups(nath, fredParisGroup);

		// vince fails to remove fred's paris group from nath's groups
		// even if it is part of his groups, he doesn't own it
		assertHttpError(403, () -> vince.credentials().unshareGroup(nath.id(), fredParisGroup));
		checkDogGroups(nath, fredParisGroup);

		// fred unshares his paris group from nath's groups
		fred.credentials().unshareGroup(nath.id(), fredParisGroup);
		checkDogGroups(nath);

		// vince removes fred's paris group from his groups
		vince.credentials().removeGroup(fredParisGroup);
		checkDogGroups(vince, vinceParisGroup);

		// vince shares his default group with nath
		vince.credentials().shareGroup(nath.id(), vince.group());
		checkDogGroups(nath, vince.group());

		// superadmin unshares vince's default group from nath
		superadmin.credentials().unshareGroup(nath.id(), vince.group());
		checkDogGroups(nath);

		// vince fails to share a group he didn't create
		// even if it is prefixed with his default group
		assertHttpError(403, () -> vince.credentials().shareGroup(nath.id(), vince.group() + "_rome"));
		checkDogGroups(nath);
	}

	private void checkDogGroups(SpaceDog dog, String... groups) {
		dog.credentials().me(true);
		Set<String> groupSet = Sets.newHashSet(groups);
		groupSet.add(dog.id());
		assertEquals(dog.groups(), groupSet);
	}
}
