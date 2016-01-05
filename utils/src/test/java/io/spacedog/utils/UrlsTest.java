package io.spacedog.utils;

import org.junit.Assert;
import org.junit.Test;

public class UrlsTest extends Assert {

	@Test
	public void trimSlash() {
		assertEquals("/v1/user", Urls.trimSlash("/v1/user/"));
		assertEquals("/v1/user", Urls.trimSlash("/v1/user"));
	}
}
