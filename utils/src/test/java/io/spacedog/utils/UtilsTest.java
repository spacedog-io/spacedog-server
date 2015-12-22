/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

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
		assertTrue(Utils.isNullOrEmpty(null));
		assertTrue(Utils.isNullOrEmpty(new ArrayList<>()));
		assertTrue(Utils.isNullOrEmpty(new HashSet<>()));
		assertFalse(Utils.isNullOrEmpty(Arrays.asList("toto")));
	}
}
