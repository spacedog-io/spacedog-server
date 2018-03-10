/**
 * © David Attias 2015
 */
package io.spacedog.utils;

import org.junit.Assert;
import org.junit.Test;

public class KeyValueTest extends Assert {

	@Test
	public void keyValueAsTags() {
		KeyValue keyValue = KeyValue.parse("id=12345");
		assertEquals("id", keyValue.getKey());
		assertEquals("12345", keyValue.getValue());
		assertEquals("id=12345", keyValue.asTag());

		keyValue = KeyValue.parse("id=GTRDF=JHG");
		assertEquals("id", keyValue.getKey());
		assertEquals("GTRDF=JHG", keyValue.getValue());
		assertEquals("id=GTRDF=JHG", keyValue.asTag());
	}
}
