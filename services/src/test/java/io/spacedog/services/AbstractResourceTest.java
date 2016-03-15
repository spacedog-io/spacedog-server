/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.junit.Assert;
import org.junit.Test;

public class AbstractResourceTest extends Assert {

	@Test
	public void shouldSucceedToSplitReferences() {
		assertEquals("job", Resource.getReferenceType("/job/engineer"));
		assertEquals("engineer", Resource.getReferenceId("/job/engineer"));
	}
}
