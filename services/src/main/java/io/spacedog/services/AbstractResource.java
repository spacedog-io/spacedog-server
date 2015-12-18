/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;

import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;
import io.spacedog.utils.Json.Type;
import net.codestory.http.Context;
import net.codestory.http.payload.Payload;

public abstract class AbstractResource {

	public static final String BASE_URL = "https://spacedog.io";

	protected Payload checkExistence(String index, String type, String field, String value) {
		return ElasticHelper.get().search(index, type, field, value).getTotalHits() == 0 ? Payload.notFound()
				: Payload.ok();
	}

	protected void refreshIfNecessary(String index, Context context, boolean defaultValue) {
		boolean refresh = context.query().getBoolean(SearchResource.REFRESH, defaultValue);
		ElasticHelper.get().refresh(refresh, index);
	}

	public static String checkStringNotNullOrEmpty(JsonNode input, String propertyPath) {
		String string = checkStringNode(input, propertyPath, true).get().asText();
		if (Strings.isNullOrEmpty(string)) {
			throw new IllegalArgumentException(String.format("property [%s] must not be null or empty", propertyPath));
		}
		return string;
	}

	public static Optional<JsonNode> checkStringNode(JsonNode input, String propertyPath, boolean required) {
		return checkJsonNodeOfType(input, propertyPath, Type.String, required);
	}

	public static Optional<JsonNode> checkBooleanNode(JsonNode input, String propertyPath, boolean required) {
		return checkJsonNodeOfType(input, propertyPath, Type.Boolean, required);
	}

	public static Optional<JsonNode> checkIntegerNode(JsonNode input, String propertyPath, boolean required) {
		return checkJsonNodeOfType(input, propertyPath, Type.Integer, required);
	}

	public static Optional<JsonNode> checkLongNode(JsonNode input, String propertyPath, boolean required) {
		return checkJsonNodeOfType(input, propertyPath, Type.Long, required);
	}

	public static Optional<JsonNode> checkFloatNode(JsonNode input, String propertyPath, boolean required) {
		return checkJsonNodeOfType(input, propertyPath, Type.Float, required);
	}

	public static Optional<JsonNode> checkDoubleNode(JsonNode input, String propertyPath, boolean required) {
		return checkJsonNodeOfType(input, propertyPath, Type.Double, required);
	}

	public static Optional<JsonNode> checkObjectNode(JsonNode input, String propertyPath, boolean required) {
		return checkJsonNodeOfType(input, propertyPath, Type.Object, required);
	}

	public static Optional<JsonNode> checkArrayNode(JsonNode input, String propertyPath, boolean required) {
		return checkJsonNodeOfType(input, propertyPath, Type.Array, required);
	}

	public static Optional<JsonNode> checkJsonNodeOfType(JsonNode input, String propertyPath, Json.Type expected,
			boolean required) {
		JsonNode node = Json.get(input, propertyPath);
		if (node == null) {
			if (required)
				throw new IllegalArgumentException(String.format("property [%s] must not be null", propertyPath));
			return Optional.ofNullable(null);
		}
		if (Json.isOfType(expected, node))
			return Optional.of(node);
		else
			throw new IllegalArgumentException(//
					String.format("property [%s] must be of type [%s] instead of [%s]", //
							propertyPath, expected, node.getNodeType()));
	}

	public static Optional<JsonNode> checkJsonNode(JsonNode input, String propertyPath, boolean required) {
		JsonNode node = Json.get(input, propertyPath);
		if (node == null) {
			if (required)
				throw new IllegalArgumentException(String.format("property [%s] must not be null", propertyPath));
			return Optional.ofNullable(null);
		}
		return Optional.of(node);
	}

	public static void checkNotPresent(JsonNode input, String propertyPath, String type) {
		JsonNode node = Json.get(input, propertyPath);
		if (node != null)
			throw new IllegalArgumentException(
					String.format("property [%s] is forbidden in type [%s]", propertyPath, type));
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
