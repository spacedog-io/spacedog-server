package io.spacedog.client.http;

import org.junit.Assert;
import org.junit.Test;

public class WebPathTest extends Assert {

	@Test
	public void test() {

		WebPath path = WebPath.newPath();
		assertEquals(0, path.size());
		assertEquals("/", path.toString());
		assertEquals("/", path.toEscapedString());

		path = path.addLast("david");
		assertEquals(1, path.size());
		assertEquals("/david", path.toString());
		assertEquals("/david", path.toEscapedString());

		path = path.addLast("vince");
		assertEquals(2, path.size());
		assertEquals("/david/vince", path.toString());
		assertEquals("/david/vince", path.toEscapedString());

		path = path.addFirst("nath");
		assertEquals(3, path.size());
		assertEquals("/nath/david/vince", path.toString());
		assertEquals("/nath/david/vince", path.toEscapedString());

		path = path.addLast("a ou b ?");
		assertEquals(4, path.size());
		assertEquals("/nath/david/vince/a ou b ?", path.toString());
		assertEquals("/nath/david/vince/a%20ou%20b%20%3F", path.toEscapedString());

		path = path.removeLast();
		assertEquals(3, path.size());
		assertEquals("/nath/david/vince", path.toString());
		assertEquals("/nath/david/vince", path.toEscapedString());

		path = path.removeFirst();
		assertEquals(2, path.size());
		assertEquals("/david/vince", path.toString());
		assertEquals("/david/vince", path.toEscapedString());

		path = path.removeFirst();
		assertEquals(1, path.size());
		assertEquals("/vince", path.toString());
		assertEquals("/vince", path.toEscapedString());

		path = path.removeFirst();
		assertEquals(0, path.size());
		assertEquals("/", path.toString());
		assertEquals("/", path.toEscapedString());
	}
}
