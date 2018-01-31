package io.spacedog.model;

import org.junit.Assert;
import org.junit.Test;

import io.spacedog.model.Schema;
import io.spacedog.utils.SpaceException;

public class SchemaTest extends Assert {

	@Test
	public void testStringReferenceMetadata() {

		try {
			Schema.builder("house")//
					.text("description").refType("XXX").build();
			fail();
		} catch (SpaceException ignored) {
		}

		Schema.builder("house")//
				.string("owner").refType("user").build();

	}
}