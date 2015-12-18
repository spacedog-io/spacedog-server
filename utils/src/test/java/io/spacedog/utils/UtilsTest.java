/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import io.spacedog.utils.Utils;

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

}
