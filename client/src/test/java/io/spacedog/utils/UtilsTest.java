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
	public void testTrimSuffix() {
		assertEquals("toto", Utils.trimSuffix("toto", "titi"));
		assertEquals("toto", Utils.trimSuffix("tototiti", "titi"));
		assertEquals("tititoto", Utils.trimSuffix("tititoto", "titi"));
		assertEquals("toto", Utils.trimSuffix("toto", ""));
		assertEquals("", Utils.trimSuffix("titi", "titi"));
		assertEquals("", Utils.trimSuffix("", "titi"));
	}

	@Test
	public void testTrimPreffix() {
		assertEquals("toto", Utils.trimPreffix("toto", "titi"));
		assertEquals("toto", Utils.trimPreffix("tititoto", "titi"));
		assertEquals("tototiti", Utils.trimPreffix("tototiti", "titi"));
		assertEquals("toto", Utils.trimPreffix("toto", ""));
		assertEquals("", Utils.trimPreffix("titi", "titi"));
		assertEquals("", Utils.trimPreffix("", "titi"));
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

	@Test
	public void shouldTrimUntil() {
		assertEquals("titi", Utils.trimUntil("toto--titi", "--"));
		assertEquals("titi", Utils.trimUntil("--titi", "--"));
		assertEquals("-titi", Utils.trimUntil("-titi", "--"));
		assertEquals("toto-titi", Utils.trimUntil("toto-titi", "--"));
		assertEquals("", Utils.trimUntil("toto--", "--"));
		assertEquals("-", Utils.trimUntil("-", "--"));
		assertEquals("", Utils.trimUntil("--", "--"));
	}

	@Test
	public void shouldTrimBefore() {
		assertEquals("--titi", Utils.trimBefore("toto--titi", "--"));
		assertEquals("--titi", Utils.trimBefore("--titi", "--"));
		assertEquals("-titi", Utils.trimBefore("-titi", "--"));
		assertEquals("toto-titi", Utils.trimBefore("toto-titi", "--"));
		assertEquals("--", Utils.trimBefore("toto--", "--"));
		assertEquals("-", Utils.trimBefore("-", "--"));
		assertEquals("--", Utils.trimBefore("--", "--"));
	}

	@Test
	public void shouldTrimFrom() {
		assertEquals("toto", Utils.trimFrom("toto--titi", "--"));
		assertEquals("toto", Utils.trimFrom("toto--", "--"));
		assertEquals("toto-", Utils.trimFrom("toto-", "--"));
		assertEquals("toto", Utils.trimFrom("toto", "--"));
		assertEquals("", Utils.trimFrom("--titi", "--"));
		assertEquals("-titi", Utils.trimFrom("-titi", "--"));
		assertEquals("", Utils.trimFrom("--", "--"));
	}

	@Test
	public void shouldTrimAfter() {
		assertEquals("toto--", Utils.trimAfter("toto--titi", "--"));
		assertEquals("toto--", Utils.trimAfter("toto--", "--"));
		assertEquals("toto-", Utils.trimAfter("toto-", "--"));
		assertEquals("toto", Utils.trimAfter("toto", "--"));
		assertEquals("--", Utils.trimAfter("--titi", "--"));
		assertEquals("-titi", Utils.trimAfter("-titi", "--"));
		assertEquals("--", Utils.trimAfter("--", "--"));
	}
}
