/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.ShardOperationFailedException;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.elasticsearch.index.mapper.MergeMappingException;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.rest.RestStatus;

import com.amazonaws.AmazonServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.services.SchemaValidator.InvalidSchemaException;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.Utils;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

public class Payloads {

	private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

	public static final String JSON_CONTENT = "application/json";
	public static final String JSON_CONTENT_UTF8 = JSON_CONTENT + ";charset=UTF-8";
	public static final String HEADER_OBJECT_ID = "x-spacedog-object-id";

	public static Payload success() {
		return json(HttpStatus.OK);
	}

	public static Payload error(Throwable t) {

		if (t instanceof AmazonServiceException) {
			return error(((AmazonServiceException) t).getStatusCode(), t);
		}
		if (t instanceof VersionConflictEngineException) {
			return error(HttpStatus.CONFLICT, t);
		}
		if (t instanceof AuthenticationException) {
			return error(HttpStatus.UNAUTHORIZED, t);
		}
		if (t instanceof NotFoundException) {
			return error(HttpStatus.NOT_FOUND, t);
		}
		if (t instanceof IndexMissingException) {
			return error(HttpStatus.NOT_FOUND, t);
		}
		if (t instanceof IllegalArgumentException) {
			return error(HttpStatus.BAD_REQUEST, t);
		}
		if (t instanceof NumberFormatException) {
			return error(HttpStatus.BAD_REQUEST, t);
		}
		if (t instanceof InvalidSchemaException) {
			return error(HttpStatus.BAD_REQUEST, t);
		}
		if (t instanceof ExecutionException) {
			if (t.getCause() instanceof MergeMappingException)
				return error(HttpStatus.BAD_REQUEST, t.getCause());
			else
				return error(HttpStatus.INTERNAL_SERVER_ERROR, t);
		}

		return error(HttpStatus.INTERNAL_SERVER_ERROR, t);
	}

	public static Payload error(int httpStatus, String message, Object... args) {
		return error(httpStatus, new RuntimeException(String.format(message, args)));
	}

	public static Payload error(int httpStatus) {
		return json(httpStatus);
	}

	public static Payload error(int httpStatus, Throwable throwable) {
		return json(minimalBuilder(httpStatus).node("error", Json.toJson(throwable)), httpStatus);
	}

	/**
	 * @param parameters
	 *            triples with parameter name, value and message
	 * @return a bad request http payload with a json listing invalid parameters
	 */
	protected static Payload invalidParameters(String... parameters) {
		JsonBuilder<ObjectNode> builder = minimalBuilder(HttpStatus.BAD_REQUEST);

		if (parameters.length > 0 && parameters.length % 3 == 0) {
			builder.object("invalidParameters");
			for (int i = 0; i < parameters.length; i += 3)
				builder.object(parameters[0])//
						.put("value", parameters[1])//
						.put("message", parameters[2]);
		}

		return json(builder, HttpStatus.BAD_REQUEST);
	}

	public static JsonBuilder<ObjectNode> savedBuilder(boolean created, String uri, String type, String id) {
		return savedBuilder(created, uri, type, id, 0);
	}

	public static JsonBuilder<ObjectNode> savedBuilder(boolean created, String uri, String type, String id,
			long version) {

		JsonBuilder<ObjectNode> builder = minimalBuilder(created ? HttpStatus.CREATED : HttpStatus.OK) //
				.put("id", id) //
				.put("type", type) //
				.put("location", AbstractResource.spaceUrl(uri, type, id).toString());

		if (version > 0) //
			builder.put("version", version);

		return builder;
	}

	public static Payload saved(boolean created, String uri, String type, String id) {
		return saved(created, uri, type, id, 0);
	}

	public static Payload saved(boolean created, String uri, String type, String id, long version) {
		JsonBuilder<ObjectNode> builder = Payloads.savedBuilder(created, uri, type, id, version);
		return json(builder, created ? HttpStatus.CREATED : HttpStatus.OK)//
				.withHeader(HEADER_OBJECT_ID, id);
	}

	public static Payload json(int httpStatus) {
		return json(minimalBuilder(httpStatus), httpStatus);
	}

	public static <N extends JsonNode> Payload json(JsonBuilder<N> content) {
		return json(content.build());
	}

	public static <N extends JsonNode> Payload json(JsonBuilder<N> content, int httpStatus) {
		return json(content.build(), httpStatus);
	}

	public static Payload json(JsonNode content) {
		return json(content, HttpStatus.OK);
	}

	public static Payload json(JsonNode content, int httpStatus) {
		if (content.isObject() && SpaceContext.get().debug())
			((ObjectNode) content).set("debug", Debug.buildDebugObjectNode());

		return new Payload(JSON_CONTENT_UTF8, content, httpStatus);
	}

	public static Payload json(RestStatus status, ShardOperationFailedException[] failures) {

		if (status.getStatus() < 400)
			return json(status.getStatus());

		JsonBuilder<ObjectNode> builder = minimalBuilder(status.getStatus()).array("error");

		for (ShardOperationFailedException failure : failures)
			builder.object().put("type", failure.getClass().getName()).put("message", failure.reason())
					.put("shardId", failure.shardId()).end();

		return json(builder, status.getStatus());
	}

	public static byte[] toBytes(Object content) {
		if (content == null)
			return EMPTY_BYTE_ARRAY;

		if (content instanceof byte[])
			return (byte[]) content;

		if (content instanceof Payload)
			return toBytes(((Payload) content).rawContent());

		return content.toString().getBytes(Utils.UTF8);
	}

	public static boolean isJson(Payload payload) {
		return payload.rawContentType() == null ? false//
				: payload.rawContentType().startsWith(JSON_CONTENT);
	}

	public static JsonBuilder<ObjectNode> minimalBuilder(int status) {
		return Json.objectBuilder().put("success", status < 400).put("status", status);
	}
}
