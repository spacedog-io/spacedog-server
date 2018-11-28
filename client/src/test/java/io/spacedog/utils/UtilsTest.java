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

	@Test
	public void testJoin() {
		assertEquals("toto", Utils.join("/", "toto"));
		assertEquals("toto/titi", Utils.join("/", "toto", "titi"));
	}

	@Test
	public void testReplaceTagDelimiters() {
		assertEquals("SpaceDog (spacedog@toolee.fr)", //
				Utils.replaceTagDelimiters("SpaceDog <spacedog@toolee.fr>"));
	}

	@Test
	public void shouldSlugify() {
		assertEquals("foobar", Utils.slugify("foobar"));
		assertEquals("foobar", Utils.slugify("  foobar "));
		assertEquals("foobar", Utils.slugify(" --foobar- -"));
		assertEquals("foo-bar", Utils.slugify("foo!bar"));
		assertEquals("buf", Utils.slugify("bœuf"));
		assertEquals("etre", Utils.slugify("êTRe"));
		assertEquals("les-amis", Utils.slugify("Les Amis"));
		assertEquals("les-amis", Utils.slugify("leS -aMis"));
		assertEquals("123-soleil", Utils.slugify("123 soleil"));
		assertEquals("a-50", Utils.slugify("à 50%"));
	}

	@Test
	public void shouldUppercaseFirstLetter() {
		assertEquals("Foobar", Utils.uppercaseFirstLetter("foobar"));
		assertEquals("Étable", Utils.uppercaseFirstLetter("étable"));
		assertEquals("-foobar", Utils.uppercaseFirstLetter("-foobar"));
		assertEquals(" foobar", Utils.uppercaseFirstLetter(" foobar"));
	}
}
