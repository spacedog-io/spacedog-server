package io.spacedog.utils;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import com.google.common.collect.Maps;
import com.mashape.unirest.http.Headers;

public class SpaceHeaders {

	// SpaceDog custom headers

	public static final String SPACEDOG_TEST = "x-spacedog-test";
	public static final String SPACEDOG_DEBUG = "x-spacedog-debug";
	public static final String SPACEDOG_OWNER = "x-spacedog-owner";
	public static final String SPACEDOG_OBJECT_ID = "x-spacedog-object-id";

	// Common values

	public static final String BASIC_SCHEME = "Basic";
	public static final String BEARER_SCHEME = "Bearer";
	public static final String ALLOW_METHODS = "GET, POST, PUT, DELETE, HEAD";

	// Common headers from CodeStory

	public static final String ACCEPT = "Accept";
	public static final String ACCEPT_CHARSET = "Accept-Charset";
	public static final String ACCEPT_ENCODING = "Accept-Encoding";
	public static final String ACCEPT_LANGUAGE = "Accept-Language";
	public static final String ALLOW = "Allow";
	public static final String AUTHORIZATION = "Authorization";
	public static final String CACHE_CONTROL = "Cache-Control";
	public static final String CONNECTION = "Connection";
	public static final String CONTENT_DISPOSITION = "Content-Disposition";
	public static final String CONTENT_ENCODING = "Content-Encoding";
	public static final String CONTENT_ID = "Content-ID";
	public static final String CONTENT_LANGUAGE = "Content-Language";
	public static final String CONTENT_LENGTH = "Content-Length";
	public static final String CONTENT_LOCATION = "Content-Location";
	public static final String CONTENT_TYPE = "Content-Type";
	public static final String DATE = "Date";
	public static final String ETAG = "ETag";
	public static final String EXPIRES = "Expires";
	public static final String HOST = "Host";
	public static final String IF_MATCH = "If-Match";
	public static final String IF_MODIFIED_SINCE = "If-Modified-Since";
	public static final String IF_NONE_MATCH = "If-None-Match";
	public static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
	public static final String LAST_MODIFIED = "Last-Modified";
	public static final String LOCATION = "Location";
	public static final String LINK = "Link";
	public static final String RETRY_AFTER = "Retry-After";
	public static final String USER_AGENT = "User-Agent";
	public static final String VARY = "Vary";
	public static final String WWW_AUTHENTICATE = "WWW-Authenticate";
	public static final String COOKIE = "Cookie";
	public static final String SET_COOKIE = "Set-Cookie";
	public static final String X_FORWARDED_FOR = "X-Forwarded-For";
	public static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
	public static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
	public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
	public static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
	public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
	public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
	public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
	public static final String ORIGIN = "Origin";

	// fields

	private Map<String, List<String>> headerMap;

	public SpaceHeaders(Headers headers) {
		headerMap = Maps.newHashMap();
		headers.forEach((name, value) -> headerMap.put(name.trim().toLowerCase(), value));
	}

	public String first(String name) {
		List<String> list = get(name);
		if (!Utils.isNullOrEmpty(list))
			return list.get(0);
		return null;
	}

	public List<String> get(String name) {
		// header name is converted to lower case to get around Unirest bug
		// where header names are only lower cased
		return headerMap.get(name.trim().toLowerCase());
	}

	public void forEach(BiConsumer<String, List<String>> action) {
		headerMap.forEach(action);
	}
}
