package io.spacedog.client.http;

import okhttp3.MediaType;

public class OkHttp {

	public static final MediaType JSON = MediaType.parse("application/json;charset=utf-8;");
	public static final MediaType TEXT_PLAIN = MediaType.parse("text/plain;charset=utf-8;");
	public static final MediaType OCTET_STREAM = MediaType.parse("application/octet-stream");
}
