package io.spacedog.client.schema;

import org.junit.Assert;
import org.junit.Test;

import io.spacedog.client.http.SpaceException;
import io.spacedog.client.schema.Schema;

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
