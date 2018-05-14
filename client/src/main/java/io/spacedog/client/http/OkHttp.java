package io.spacedog.client.http;

import okhttp3.MediaType;

public class OkHttp {

	public static final MediaType JSON = MediaType.parse(ContentTypes.JSON_UTF8);
	public static final MediaType TEXT_PLAIN = MediaType.parse(ContentTypes.TEXT_PLAIN_UTF8);
	public static final MediaType OCTET_STREAM = MediaType.parse(ContentTypes.OCTET_STREAM);
}
