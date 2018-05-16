package io.spacedog.client.http;

import com.google.common.escape.Escaper;
import com.google.common.net.PercentEscaper;
import com.google.common.net.UrlEscapers;

public class Uris {

	private static final Escaper PATH_SEGMENT_ESCAPER = UrlEscapers.urlPathSegmentEscaper();
	private static final Escaper PATH_ESCAPER = new PercentEscaper("-._~!$'()*,;&=@:+/", false);
	private static final Escaper PARAMETER_ESCAPER = UrlEscapers.urlFormParameterEscaper();
	private static final Escaper FRAGMENT_ESCAPER = UrlEscapers.urlFragmentEscaper();

	public static String escapePath(String path) {
		return PATH_ESCAPER.escape(path);
	}

	public static String escapePathSegment(String pathSegment) {
		return PATH_SEGMENT_ESCAPER.escape(pathSegment);
	}

	public static String escapeParameter(String parameter) {
		return PARAMETER_ESCAPER.escape(parameter);
	}

	public static String escapeFragment(String fragment) {
		return FRAGMENT_ESCAPER.escape(fragment);
	}

}
