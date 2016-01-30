/**
 * © David Attias 2015
 */
package io.spacedog.services;

import com.google.common.base.Strings;

import io.spacedog.utils.Check;
import io.spacedog.utils.SpaceHeaders;
import io.spacedog.utils.Utils;
import net.codestory.http.Context;

public abstract class AbstractResource {

	public static final String SLASH = "/";

	protected static StringBuilder spaceUrl(String uri, String type, String id) {
		return spaceUrl(uri).append(SLASH).append(type).append(SLASH).append(id);
	}

	protected static StringBuilder spaceUrl(String uri) {
		Check.notNullOrEmpty(uri, "URI");
		Check.isTrue(uri.startsWith(SLASH), "URI must start with a /");
		return spaceRootUrl().append(uri);
	}

	protected static StringBuilder spaceRootUrl() {
		StartConfiguration conf = Start.get().configuration();
		StringBuilder builder = new StringBuilder(conf.url());
		int port = conf.mainPort();
		if (port != 443 && port != 80)
			builder.append(':').append(port);
		return builder;
	}

	protected static String getReferenceType(String reference) {
		return Utils.splitBySlash(reference)[0];
	}

	protected static String getReferenceId(String reference) {
		return Utils.splitBySlash(reference)[1];
	}

	protected static boolean isTest(Context context) {
		String header = context.header(SpaceHeaders.SPACEDOG_TEST);
		return Strings.isNullOrEmpty(header) ? false : Boolean.parseBoolean(header);
	}

	protected static String get(Context context, String name, String defaultValue) {
		String value = context.get(name);
		return value == null ? defaultValue : value;
	}
}
