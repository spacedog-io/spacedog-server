/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.junit.Assert;
import org.junit.Test;

public class AbstractResourceTest extends Assert {

	@Test
	public void shouldSucceedToSplitReferences() {
		assertEquals("job", AbstractResource.getReferenceType("/job/engineer"));
		assertEquals("engineer", AbstractResource.getReferenceId("/job/engineer"));
	}
}
