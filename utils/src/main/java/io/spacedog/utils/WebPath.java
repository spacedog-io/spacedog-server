package io.spacedog.utils;

import java.util.Arrays;

import com.google.common.base.Strings;
import com.google.common.collect.ObjectArrays;

public class WebPath {

	private String[] segments;
	private String uriPath;
	private String s3Key;

	public static final String SLASH = "/";
	public static final WebPath ROOT = new WebPath();;

	private WebPath(String... segments) {
		this.segments = segments;
	}

	public static WebPath newPath(String... segments) {
		return segments.length == 0 ? ROOT : new WebPath(segments);
	}

	public static WebPath parse(String path) {
		if (Strings.isNullOrEmpty(path) || path.equals(SLASH))
			return ROOT;
		if (path.startsWith(SLASH))
			path = path.substring(1);
		if (path.endsWith(SLASH))
			path = path.substring(0, path.length() - 1);
		return new WebPath(path.split(SLASH));
	}

	public WebPath addFirst(String... strings) {
		if (strings.length == 0)
			return this;

		return new WebPath(ObjectArrays.concat(strings, segments, String.class));
	}

	public WebPath addLast(String... strings) {
		if (strings.length == 0)
			return this;

		return new WebPath(ObjectArrays.concat(segments, strings, String.class));
	}

	public WebPath removeFirst() {
		String[] result = new String[segments.length - 1];
		System.arraycopy(segments, 1, result, 0, result.length);
		return newPath(result);
	}

	public WebPath removeLast() {
		String[] result = new String[segments.length - 1];
		System.arraycopy(segments, 0, result, 0, result.length);
		return newPath(result);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (this == obj)
			return true;
		if (obj instanceof WebPath)
			return Arrays.equals(segments, ((WebPath) obj).segments);
		return false;
	}

	@Override
	public String toString() {
		return SLASH + toS3Key();
	}

	public String toS3Key() {
		if (this.s3Key == null)
			this.s3Key = String.join(SLASH, segments);
		return this.s3Key;
	}

	public String toS3Prefix() {
		return toS3Key() + SLASH;
	}

	public String toEscapedString() {
		if (this.uriPath == null) {
			StringBuilder builder = new StringBuilder();
			for (String segment : segments)
				builder.append(SLASH).append(Uris.escapeSegment(segment));
			this.uriPath = builder.length() == 0 ? SLASH : builder.toString();
		}
		return this.uriPath;
	}

	public int size() {
		return segments.length;
	}

	public String first() {
		return segments[0];
	}

	public String last() {
		return segments[segments.length - 1];
	}

	public String get(int index) {
		return segments[index];
	}
}
