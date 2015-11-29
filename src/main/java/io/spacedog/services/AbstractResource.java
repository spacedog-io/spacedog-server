/**
 * © David Attias 2015
 */
package io.spacedog.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import net.codestory.http.payload.Payload;

public abstract class AbstractResource {

	public static final String JSON_CONTENT = "application/json;charset=UTF-8";
	public static final String BASE_URL = "https://spacedog.io";

	protected Payload checkExistence(String index, String type, String field, String value) {
		try {
			return ElasticHelper.get().search(index, type, field, value).getTotalHits() == 0 ? Payload.notFound()
					: Payload.ok();

		} catch (Throwable throwable) {
			return PayloadHelper.error(throwable);
		}
	}

	public static JsonNode checkNotNullOrEmpty(JsonNode input, String propertyPath, String type) {
		JsonNode node = Json.get(input, propertyPath);
		if (node == null || Strings.isNullOrEmpty(node.asText()))
			throw new IllegalArgumentException(
					String.format("property [%s] is required in type [%s]", propertyPath, type));
		return node;
	}

	public static void checkNotPresent(JsonNode input, String propertyPath, String type) {
		JsonNode node = Json.get(input, propertyPath);
		if (node != null)
			throw new IllegalArgumentException(
					String.format("property [%s] is forbidden in type [%s]", propertyPath, type));
	}

	public static String checkString(JsonNode input, String propertyPath, boolean required, String in) {
		JsonNode node = Json.get(input, propertyPath);
		if (required && node == null)
			throw new IllegalArgumentException(String.format("property [%s] is mandatory in %s", propertyPath, in));
		if (!node.isTextual())
			throw new IllegalArgumentException(String.format("property [%s] must be textual", propertyPath));
		return node.asText();
	}

	protected static JsonBuilder<ObjectNode> initSavedBuilder(String uri, String type, String id, long version) {
		JsonBuilder<ObjectNode> builder = Json.objectBuilder() //
				.put("success", true) //
				.put("id", id) //
				.put("type", type) //
				.put("location", toUrl(BASE_URL, uri, type, id));

		if (version > 0) //
			builder.put("version", version);

		return builder;
	}

	protected static String toUrl(String baseUrl, String uri, String type, String id) {
		return new StringBuilder(baseUrl).append(uri).append('/').append(type).append('/').append(id).toString();
	}

	protected static String getReferenceType(String reference) {
		return Utils.splitBySlash(reference)[0];
	}

	protected static String getReferenceId(String reference) {
		return Utils.splitBySlash(reference)[1];
	}
}
