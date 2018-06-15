/**
 * © David Attias 2015
 */
package io.spacedog.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.common.Strings;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.spacedog.client.http.SpaceFields;
import io.spacedog.client.http.SpaceHeaders;
import io.spacedog.client.http.SpaceParams;
import io.spacedog.utils.Check;
import io.spacedog.utils.Exceptions;
import net.codestory.http.Context;
import net.codestory.http.Part;
import net.codestory.http.Query;

public abstract class SpaceResty implements SpaceFields, SpaceParams {

	public static final String SLASH = "/";

	public static ElasticClient elastic() {
		return Server.get().elasticClient();
	}

	public static boolean isRefreshRequested(Context context) {
		return isRefreshRequested(context, false);
	}

	public static boolean isRefreshRequested(Context context, boolean defaultValue) {
		return context.query().getBoolean(REFRESH_PARAM, defaultValue);
	}

	public static boolean isFailRequested(Context context) {
		return context.get(FAIL_PARAM) != null;
	}

	public static StringBuilder spaceUrl(String uri, String type, String id) {
		return spaceUrl(uri).append(SLASH).append(type).append(SLASH).append(id);
	}

	public static StringBuilder spaceUrl(String uri) {
		Check.notNullOrEmpty(uri, "URI");
		Check.isTrue(uri.startsWith(SLASH), "URI must start with a /");
		return spaceRootUrl().append(uri);
	}

	public static StringBuilder spaceRootUrl() {
		return Server.backend().urlBuilder();
	}

	protected static String get(Context context, String name, String defaultValue) {
		String value = context.get(name);
		return value == null ? defaultValue : value;
	}

	protected static String getRequestContent(Context context) {
		try {
			return context.request().content();
		} catch (IOException e) {
			throw Exceptions.illegalArgument(e, "error reading http request content");
		}
	}

	protected static byte[] getRequestContentAsBytes(Context context) {
		try {
			return context.request().contentAsBytes();
		} catch (IOException e) {
			throw Exceptions.illegalArgument(e, "error reading http request content");
		}
	}

	protected static InputStream getRequestContentAsInputStream(Context context) {
		try {
			return context.request().inputStream();
		} catch (IOException e) {
			throw Exceptions.illegalArgument(e, "error reading http request content");
		}
	}

	public static FormQuery formQuery(Context context) {
		return new FormQuery(context);
	}

	public Locale getRequestLocale(Context context) {
		String languageTag = context.header(SpaceHeaders.ACCEPT_LANGUAGE);
		return Strings.isNullOrEmpty(languageTag) //
				? Locale.getDefault()//
				: Locale.forLanguageTag(languageTag);
	}

	public static class FormQuery {

		private Query query;
		private Map<String, String> parts;

		public FormQuery(Context context) {
			try {
				this.query = context.query();
				List<Part> parts = context.parts();
				if (parts != null) {
					this.parts = Maps.newHashMap();
					for (Part part : parts)
						if (!part.isFile())
							this.parts.put(part.name(), part.content());
				}
			} catch (IOException e) {
				throw Exceptions.runtime(e);
			}
		}

		public String get(String name) {
			String result = query.get(name);
			if (Strings.isNullOrEmpty(result))
				result = parts.get(name);
			return result;
		}

		public String get(String name, String defaultValue) {
			String result = this.get(name);
			return Strings.isNullOrEmpty(result) ? defaultValue : result;
		}

		public Iterable<String> keys() {
			Collection<String> queryKeys = this.query.keys();
			Set<String> partsKeys = this.parts.keySet();
			Set<String> keys = Sets.newHashSetWithExpectedSize(queryKeys.size() + partsKeys.size());
			keys.addAll(queryKeys);
			keys.addAll(partsKeys);
			return keys;
		}

	}

}
