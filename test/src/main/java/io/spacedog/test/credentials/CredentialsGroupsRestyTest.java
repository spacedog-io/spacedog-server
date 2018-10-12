package io.spacedog.test.credentials;

import org.junit.Test;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import io.spacedog.client.SpaceDog;
import io.spacedog.test.SpaceTest;

public class CredentialsGroupsRestyTest extends SpaceTest {

	@Test
	public void test() {

		// prepare
		prepareTest();
		SpaceDog guest = SpaceDog.dog();
		SpaceDog superadmin = clearServer();
		superadmin.credentials().enableGuestSignUp(true);

		// fred signs up
		SpaceDog fred = createTempDog(guest, "fred");

		// fred has his own group
		assertFalse(Strings.isNullOrEmpty(fred.group()));

		// // fred creates nath credentials
		// SpaceDog nath = createTempDog(fred, "nath");
		//
		// // nath has the same group than fred
		// assertFalse(Strings.isNullOrEmpty(nath.group()));
		// assertEquals(fredDefaultGroup, nath.group());

		// vince signs up
		SpaceDog vince = createTempDog(guest, "vince");

		// vince and fred have their own unique group
		assertFalse(Strings.isNullOrEmpty(vince.group()));
		assertNotEquals(vince.group(), fred.group());

		// fred shares his group with vince
		fred.credentials().shareGroup(vince.id(), fred.group());

		// vince has now 2 groups, his default and fred's group
		vince.credentials().me(true);
		assertTrue(vince.groups().contains(vince.group()));
		assertTrue(vince.groups().contains(fred.group()));
		assertEquals(2, vince.groups().size());

		// fred unshares his group with vince
		fred.credentials().unshareGroup(vince.id(), fred.group());

		// vince only has his default group
		vince.credentials().me(true);
		assertEquals(vince.group(), vince.group());
		assertEquals(1, vince.groups().size());

		// fred fails to add to himself vince's group
		assertHttpError(403, () -> fred.credentials().shareGroup(fred.id(), vince.group()));

		// fred is not changed
		fred.credentials().me(true);
		assertFalse(fred.groups().contains(vince.group()));

		// vince share his default group with fred
		vince.credentials().shareGroup(fred.id(), vince.group());

		// fred has his default group and vince's group
		fred.credentials().me(true);
		assertTrue(fred.groups().contains(fred.group()));
		assertTrue(fred.groups().contains(vince.group()));

		// fred fails to share a group he doesn't own
		assertHttpError(403, () -> fred.credentials().shareGroup(vince.id(), "XXX"));

		// vince is not changed
		vince.credentials().me(true);
		assertFalse(vince.groups().contains("XXX"));

		// fred fails to unshare a group he doesn't own
		assertHttpError(403, () -> fred.credentials().unshareGroup(vince.id(), "XXX"));

		// vince is not changed
		vince.credentials().me(true);
		assertEquals(1, vince.groups().size());
		assertEquals(vince.group(), vince.group());
	}

	@Test
	public void testCreateGroup() {

		// prepare
		prepareTest();
		SpaceDog superadmin = clearServer();

		// fred signs up
		SpaceDog fred = createTempDog(superadmin, "fred");

		// fred creates a new group
		fred.credentials().createGroup("spacedog");

		// fred has 2 groups
		fred.credentials().me(true);
		assertEquals(2, fred.groups().size());
		assertEquals(fred.groups(), Sets.newHashSet(fred.id(), fred.id() + "__" + "spacedog"));

		// fred creates an already existing group
		fred.credentials().createGroup("spacedog");

		// fred does not change
		fred.credentials().me(true);
		assertEquals(2, fred.groups().size());
		assertEquals(fred.groups(), Sets.newHashSet(fred.id(), fred.id() + "__" + "spacedog"));
	}
}
