package io.spacedog.client.schema;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;

public class JsonGeneratorTest extends Assert {

	@Test
	public void test() {

		Schema schema = Schema.builder("person")//
				.string("name")//
				.object("address")//
				.string("street")//
				.string("city")//
				.string("name")//
				.close()//
				.build();

		DataObjetGenerator generator = new DataObjetGenerator();
		generator.regPath("name", "vince", "william");
		generator.regPath("address.name", "vince", "william");
		ObjectNode person = generator.gen(schema);
		Utils.info(Json.toPrettyString(person));
	}
}
