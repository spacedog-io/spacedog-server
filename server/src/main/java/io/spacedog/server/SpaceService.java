/**
 * © David Attias 2015
 */
package io.spacedog.server;

import java.io.IOException;
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

public abstract class SpaceService implements SpaceFields, SpaceParams {

	public static final String SLASH = "/";

	public static ElasticClient elastic() {
		return Server.get().elasticClient();
	}

	protected static boolean isRefreshRequested(Context context) {
		return isRefreshRequested(context, false);
	}

	protected static boolean isRefreshRequested(Context context, boolean defaultValue) {
		return context.query().getBoolean(REFRESH_PARAM, defaultValue);
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
		return SpaceContext.backend().urlBuilder();
	}

	protected static String get(Context context, String name, String defaultValue) {
		String value = context.get(name);
		return value == null ? defaultValue : value;
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
