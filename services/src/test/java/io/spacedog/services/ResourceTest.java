/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.junit.Test;

import io.spacedog.client.SpaceTest;

public class ResourceTest extends SpaceTest {

	@Test
	public void shouldSucceedToSplitReferences() {
		assertEquals("job", Resource.getReferenceType("/job/engineer"));
		assertEquals("engineer", Resource.getReferenceId("/job/engineer"));
	}
}
