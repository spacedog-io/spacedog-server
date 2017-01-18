/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

public class InternalsTest extends Assert {

	@Test
	public void testNotify() {
		Internals.get().notify(Optional.empty(), "title", "message");
	}
}
