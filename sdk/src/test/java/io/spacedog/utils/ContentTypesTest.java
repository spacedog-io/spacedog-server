package io.spacedog.utils;

import org.junit.Assert;
import org.junit.Test;

public class ContentTypesTest extends Assert {

	@Test
	public void shouldGetContentTypesFromFileName() {
		assertEquals(ContentTypes.PDF, ContentTypes.parseFileExtension("foo.pdf"));
		assertEquals(ContentTypes.PDF, ContentTypes.parseFileExtension("foo.PDF"));
		assertEquals(ContentTypes.PDF, ContentTypes.parseFileExtension("foo .pdf"));

		assertEquals(ContentTypes.OCTET_STREAM, ContentTypes.parseFileExtension("foopdf"));
		assertEquals(ContentTypes.OCTET_STREAM, ContentTypes.parseFileExtension("foo. pdf"));
		assertEquals(ContentTypes.OCTET_STREAM, ContentTypes.parseFileExtension(""));
		assertEquals(ContentTypes.OCTET_STREAM, ContentTypes.parseFileExtension(null));
	}
}
