package io.spacedog.utils;

import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;

public class Uris {

	private static Escaper segmentEscaper = UrlEscapers.urlPathSegmentEscaper();
	private static Escaper parameterEscaper = UrlEscapers.urlFormParameterEscaper();
	private static Escaper fragmentEscaper = UrlEscapers.urlFragmentEscaper();

	public static String escapeSegment(String segment) {
		return segmentEscaper.escape(segment);
	}

	public static String escapeParameter(String parameter) {
		return parameterEscaper.escape(parameter);
	}

	public static String escapeFragment(String fragment) {
		return fragmentEscaper.escape(fragment);
	}

}
