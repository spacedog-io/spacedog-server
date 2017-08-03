/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.common.Strings;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.spacedog.utils.Check;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.SpaceFields;
import io.spacedog.utils.SpaceParams;
import io.spacedog.utils.Utils;
import net.codestory.http.Context;
import net.codestory.http.Part;
import net.codestory.http.Query;

public abstract class Resource implements SpaceFields, SpaceParams {

	public static final String SLASH = "/";
	public static final String SPACEDOG_BACKEND = "spacedog";

	public static StringBuilder spaceUrl(String backendId, String uri, String type, String id) {
		return spaceUrl(backendId, uri).append(SLASH).append(type).append(SLASH).append(id);
	}

	public static StringBuilder spaceUrl(String backendId, String uri) {
		Check.notNullOrEmpty(uri, "URI");
		Check.isTrue(uri.startsWith(SLASH), "URI must start with a /");
		return spaceRootUrl(backendId).append(uri);
	}

	public static StringBuilder spaceRootUrl(String backendId) {
		return new StringBuilder(Start.get().configuration().apiUrl(backendId));
	}

	protected static String getReferenceType(String reference) {
		return Utils.splitBySlash(reference)[0];
	}

	protected static String getReferenceId(String reference) {
		return Utils.splitBySlash(reference)[1];
	}

	protected static String get(Context context, String name, String defaultValue) {
		String value = context.get(name);
		return value == null ? defaultValue : value;
	}

	public static String getBucketName(String bucketSuffix) {
		return Start.get().configuration().getAwsBucketPrefix() + bucketSuffix;
	}

	public static FormQuery formQuery(Context context) {
		return new FormQuery(context);
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

	protected static ElasticClient elastic() {
		return Start.get().getElasticClient();
	}

}
