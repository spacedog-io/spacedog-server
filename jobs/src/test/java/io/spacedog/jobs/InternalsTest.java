/**
 * © David Attias 2015
 */
package io.spacedog.jobs;

import org.junit.Assert;
import org.junit.Test;

public class InternalsTest extends Assert {

	@Test
	public void testNotify() {
		Internals.get().notify(null, "title", "message");
	}
}
