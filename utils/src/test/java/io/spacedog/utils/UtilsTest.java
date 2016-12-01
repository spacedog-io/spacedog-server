/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class UtilsTest extends Assert {

	@Test
	public void shouldSplitByDotSuccessfuly() {
		assertEquals(Arrays.asList("r", "t"), Arrays.asList(Utils.splitByDot(".r.t.")));
		assertEquals(Arrays.asList("r", "t"), Arrays.asList(Utils.splitByDot("r.t")));
		assertEquals(Arrays.asList("r", "t"), Arrays.asList(Utils.splitByDot(".r.t")));
		assertEquals(Arrays.asList("r", "t"), Arrays.asList(Utils.splitByDot("r.t.")));
	}

	@Test
	public void shouldSplitBySlashSuccessfuly() {
		assertEquals(Arrays.asList("r", "t"), Arrays.asList(Utils.splitBySlash("/r/t/")));
		assertEquals(Arrays.asList("r", "t"), Arrays.asList(Utils.splitBySlash("r/t")));
		assertEquals(Arrays.asList("r", "t"), Arrays.asList(Utils.splitBySlash("/r/t")));
		assertEquals(Arrays.asList("r", "t"), Arrays.asList(Utils.splitBySlash("r/t/")));
	}

	@Test
	public void checkNullOrEmptyCollections() {
		assertTrue(Utils.isNullOrEmpty((List<String>) null));
		assertTrue(Utils.isNullOrEmpty(new ArrayList<>()));
		assertTrue(Utils.isNullOrEmpty(new HashSet<>()));
		assertFalse(Utils.isNullOrEmpty(Arrays.asList("toto")));
	}

	@Test
	public void checkNullOrEmptyArrays() {
		assertTrue(Utils.isNullOrEmpty((Object[]) null));
		assertTrue(Utils.isNullOrEmpty(new String[0]));
		assertFalse(Utils.isNullOrEmpty(new String[1]));
	}

	@Test
	public void testRemoveSuffix() {
		assertEquals("toto", Utils.removeSuffix("toto", "titi"));
		assertEquals("toto", Utils.removeSuffix("tototiti", "titi"));
		assertEquals("tititoto", Utils.removeSuffix("tititoto", "titi"));
		assertEquals("toto", Utils.removeSuffix("toto", ""));
		assertEquals("", Utils.removeSuffix("titi", "titi"));
		assertEquals("", Utils.removeSuffix("", "titi"));
	}

	@Test
	public void testRemovePreffix() {
		assertEquals("toto", Utils.removePreffix("toto", "titi"));
		assertEquals("toto", Utils.removePreffix("tititoto", "titi"));
		assertEquals("tototiti", Utils.removePreffix("tototiti", "titi"));
		assertEquals("toto", Utils.removePreffix("toto", ""));
		assertEquals("", Utils.removePreffix("titi", "titi"));
		assertEquals("", Utils.removePreffix("", "titi"));
	}

}
