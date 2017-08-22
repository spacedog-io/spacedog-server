/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ShardOperationFailedException;
import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchPhaseExecutionException;

import com.amazonaws.AmazonServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.SpaceException;
import io.spacedog.utils.SpaceHeaders;
import io.spacedog.utils.Utils;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

public class JsonPayload {

	private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

	public static Payload success() {
		return json(HttpStatus.OK);
	}

	public static Payload error(Throwable t) {
		return error(status(t), t);
	}

	public static int status(Throwable t) {

		if (t instanceof IllegalArgumentException)
			return HttpStatus.BAD_REQUEST;
		if (t instanceof AmazonServiceException)
			return ((AmazonServiceException) t).getStatusCode();
		if (t instanceof SpaceException)
			return ((SpaceException) t).httpStatus();
		// elastic returns 500 when result window is too large
		// let's return 400 instead
		if (t instanceof SearchPhaseExecutionException)
			if (t.toString().contains("from + size must be less"))
				return HttpStatus.BAD_REQUEST;
		if (t instanceof ElasticsearchException)
			return ((ElasticsearchException) t).status().getStatus();
		if (t.getCause() != null)
			return status(t.getCause());

		return HttpStatus.INTERNAL_SERVER_ERROR;
	}

	public static Payload error(int httpStatus, String message, Object... args) {
		return error(httpStatus, Exceptions.runtime(message, args));
	}

	public static Payload error(int httpStatus) {
		return json(httpStatus);
	}

	public static Payload error(int httpStatus, Throwable throwable) {
		JsonNode errorNode = toJson(throwable, SpaceContext.isDebug() || httpStatus >= 500);
		return json(builder(httpStatus).node("error", errorNode), httpStatus);
	}

	public static JsonNode toJson(Throwable t, boolean debug) {
		ObjectNode json = Json.object();

		if (!Strings.isNullOrEmpty(t.getMessage()))//
			json.put("message", t.getMessage());

		if (t instanceof SpaceException) {
			SpaceException se = (SpaceException) t;
			if (se.code() != null)
				json.put("code", se.code());
			if (se.details() != null)
				json.set("details", se.details());
		}

		if (debug) {
			json.put("type", t.getClass().getName());

			if (t instanceof ElasticsearchException) {
				ElasticsearchException elasticException = ((ElasticsearchException) t);
				for (String key : elasticException.getHeaderKeys()) {
					json.with("elastic").set(key, Json.toNode(elasticException.getHeader(key)));
				}
			}

			ArrayNode array = json.putArray("trace");
			for (StackTraceElement element : t.getStackTrace())
				array.add(element.toString());

		}

		if (t.getCause() != null)
			json.set("cause", toJson(t.getCause(), debug));

		return json;
	}

	public static Payload toJson(String uriBase, IndexResponse response) {
		return JsonPayload.saved(response.isCreated(), uriBase, response.getType(), //
				response.getId(), response.getVersion());
	}

	/**
	 * @param parameters
	 *            triples with parameter name, value and message
	 * @return a bad request http payload with a json listing invalid parameters
	 */
	protected static Payload invalidParameters(String... parameters) {
		JsonBuilder<ObjectNode> builder = builder(HttpStatus.BAD_REQUEST);

		if (parameters.length > 0 && parameters.length % 3 == 0) {
			builder.object("invalidParameters");
			for (int i = 0; i < parameters.length; i += 3)
				builder.object(parameters[0])//
						.put("value", parameters[1])//
						.put("message", parameters[2]);
		}

		return json(builder, HttpStatus.BAD_REQUEST);
	}

	public static JsonBuilder<ObjectNode> builder(boolean created, String uri, String type, String id) {
		return builder(created, uri, type, id, 0);
	}

	public static Payload saved(boolean created, String uri, String type, String id, boolean isUriFinal) {
		return saved(created, uri, type, id, 0, isUriFinal);
	}

	public static Payload saved(boolean created, String uri, String type, String id) {
		return saved(created, uri, type, id, 0, false);
	}

	public static Payload saved(boolean created, String uri, String type, String id, long version) {
		return saved(created, uri, type, id, version, false);
	}

	public static Payload saved(boolean created, String uri, String type, String id, long version, boolean isUriFinal) {
		JsonBuilder<ObjectNode> builder = JsonPayload.builder(created, uri, type, id, version, isUriFinal);
		return json(builder, created ? HttpStatus.CREATED : HttpStatus.OK)//
				.withHeader(SpaceHeaders.SPACEDOG_OBJECT_ID, id);
	}

	public static JsonBuilder<ObjectNode> builder(boolean created, String uri, String type, String id, long version) {
		return builder(created, uri, type, id, version, false);
	}

	public static JsonBuilder<ObjectNode> builder(boolean created, String uri, String type, String id, long version,
			boolean isUriFinal) {

		JsonBuilder<ObjectNode> builder = builder(created ? HttpStatus.CREATED : HttpStatus.OK) //
				.put("id", id) //
				.put("type", type);

		if (isUriFinal)
			builder.put("location", Resource.spaceUrl(uri).toString());
		else
			builder.put("location", Resource.spaceUrl(uri, type, id).toString());

		if (version > 0) //
			builder.put("version", version);

		return builder;
	}

	public static Payload json(int httpStatus) {
		return json(builder(httpStatus), httpStatus);
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
		if (content == null)
			content = NullNode.getInstance();
		if (content.isObject() && SpaceContext.isDebug())
			((ObjectNode) content).set("debug", SpaceContext.debug().toNode());

		return new Payload(Json.JSON_CONTENT_UTF8, content, httpStatus);
	}

	public static Payload json(String content) {
		return json(content, HttpStatus.OK);
	}

	public static Payload json(String content, int httpStatus) {
		return new Payload(Json.JSON_CONTENT_UTF8, content, httpStatus);
	}

	public static Payload pojo(Object pojo) {
		return pojo(pojo, HttpStatus.OK);
	}

	public static Payload pojo(Object pojo, int httpStatus) {
		return new Payload(Json.JSON_CONTENT_UTF8, Json.toString(pojo), httpStatus);
	}

	public static Payload json(int status, ShardOperationFailedException[] failures) {

		if (status < 400)
			return json(status);

		JsonBuilder<ObjectNode> builder = builder(status).array("error");

		for (ShardOperationFailedException failure : failures)
			builder.object().put("type", failure.getClass().getName()).put("message", failure.reason())
					.put("shardId", failure.shardId()).end();

		return json(builder, status);
	}

	public static Payload json(DeleteByQueryResponse response) {

		if (response.isTimedOut())
			return error(504, //
					"the delete by query operation timed out, some objects might have been deleted");

		if (response.getTotalFound() != response.getTotalDeleted())
			return error(500, String.format(//
					"the delete by query operation failed to delete all objects found, "
							+ "objects found [%s], objects deleted [%s]",
					response.getTotalFound(), response.getTotalDeleted()));

		if (response.getShardFailures().length > 0)
			return json(500, response.getShardFailures());

		return json(builder()//
				.put("totalDeleted", response.getTotalDeleted()));
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
				: payload.rawContentType().startsWith(Json.JSON_CONTENT);
	}

	public static JsonBuilder<ObjectNode> builder() {
		return builder(200);
	}

	public static JsonBuilder<ObjectNode> builder(int status) {
		return Json.objectBuilder().put("success", status < 400).put("status", status);
	}

	public static JsonNode toJsonNode(Payload payload) {

		Object rawContent = payload.rawContent();
		if (rawContent instanceof JsonNode)
			return (JsonNode) rawContent;
		if (rawContent instanceof String)
			return Json.readNode((String) rawContent);

		return JsonPayload.builder(payload.code()).build();
		// throw Exceptions.illegalArgument("non json payload: [%s]",
		// rawContent);
	}

}
