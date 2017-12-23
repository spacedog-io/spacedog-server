package io.spacedog.http;

import java.util.Arrays;
import java.util.Iterator;

import com.google.common.base.Strings;
import com.google.common.collect.ObjectArrays;

import io.spacedog.utils.Utils;

public class WebPath implements Iterable<String> {

	private String[] segments;
	private String uriPath;

	public static final String SLASH = "/";
	public static final WebPath ROOT = new WebPath();

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

	public boolean isRoot() {
		return Utils.isNullOrEmpty(segments);
	}

	public String join() {
		return String.join(SLASH, segments);
	}

	@Override
	public String toString() {
		return SLASH + join();
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

	@Override
	public Iterator<String> iterator() {
		return new Iterator<String>() {

			private int next = 0;

			@Override
			public boolean hasNext() {
				return next < segments.length;
			}

			@Override
			public String next() {
				String result = segments[next];
				next = next + 1;
				return result;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

		};
	}
}
