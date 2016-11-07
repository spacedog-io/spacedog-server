/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.junit.Assert;
import org.junit.Test;

import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Maps;

public class MailPebbleResourceTest extends Assert {

	@Test
	public void checkValueSimpleAndValid() {
		assertTrue(MailPebbleResource.checkValueSimpleAndValid("param", "a", "string"));
		assertTrue(MailPebbleResource.checkValueSimpleAndValid("param", 1, "integer"));
		assertTrue(MailPebbleResource.checkValueSimpleAndValid("param", 2l, "long"));
		assertTrue(MailPebbleResource.checkValueSimpleAndValid("param", 3f, "float"));
		assertTrue(MailPebbleResource.checkValueSimpleAndValid("param", 4d, "double"));
		assertTrue(MailPebbleResource.checkValueSimpleAndValid("param", true, "boolean"));
		assertTrue(MailPebbleResource.checkValueSimpleAndValid("param", Lists.newArrayList(), "array"));
		assertTrue(MailPebbleResource.checkValueSimpleAndValid("param", Maps.newHashMap(), "object"));

		try {
			assertFalse(MailPebbleResource.checkValueSimpleAndValid("param", 1, "string"));
			fail();
		} catch (Exception e) {
		}
		try {
			assertFalse(MailPebbleResource.checkValueSimpleAndValid("param", 1d, "integer"));
			fail();
		} catch (Exception e) {
		}
		try {
			assertFalse(MailPebbleResource.checkValueSimpleAndValid("param", 1, "long"));
			fail();
		} catch (Exception e) {
		}
		try {
			assertFalse(MailPebbleResource.checkValueSimpleAndValid("param", 1, "float"));
			fail();
		} catch (Exception e) {
		}
		try {
			assertFalse(MailPebbleResource.checkValueSimpleAndValid("param", 1, "double"));
			fail();
		} catch (Exception e) {
		}
		try {
			assertFalse(MailPebbleResource.checkValueSimpleAndValid("param", 1, "boolean"));
			fail();
		} catch (Exception e) {
		}
		try {
			assertFalse(MailPebbleResource.checkValueSimpleAndValid("param", Maps.newHashMap(), "array"));
			fail();
		} catch (Exception e) {
		}
		try {
			assertFalse(MailPebbleResource.checkValueSimpleAndValid("param", Lists.newArrayList(), "object"));
			fail();
		} catch (Exception e) {
		}

	}
}
