/**
 * © David Attias 2015
 */
package io.spacedog.services;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.search.SearchPhaseExecutionException;

import com.amazonaws.AmazonServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import io.spacedog.utils.Json;
import io.spacedog.utils.SpaceException;
import io.spacedog.utils.SpaceFields;
import io.spacedog.utils.SpaceHeaders;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

public class JsonPayload implements SpaceFields {

	private JsonNode node;
	private Payload payload;

	private JsonPayload(int status) {
		this.payload = new Payload(status);
	}

	private ObjectNode object() {
		if (node == null)
			node = Json.object();
		return Json.checkObject(node);
	}

	public JsonPayload withLocation(String uri) {
		object().put("location", Resource.spaceUrl(uri).toString());
		return this;
	}

	public JsonPayload withLocation(String uri, String type, String id) {
		object().put("location", Resource.spaceUrl(uri, type, id).toString());
		return this;
	}

	public JsonPayload withVersion(long version) {
		if (version > 0)
			object().put(VERSION_FIELD, version);
		return this;
	}

	public JsonPayload withCode(int code) {
		payload.withCode(code);
		return this;
	}

	public JsonPayload withHeader(String key, String value) {
		payload.withHeader(key, value);
		return this;
	}

	public JsonPayload with(JsonNode node) {
		this.node = node;
		return this;
	}

	public JsonPayload with(Object... fields) {
		Json.addAll(object(), fields);
		return this;
	}

	public JsonPayload withResults(ArrayNode array) {
		this.object().set("results", array);
		return this;
	}

	public Payload build() {

		int status = payload.code();
		if (status >= 400)
			object().put("success", status < 400)//
					.put("status", status);

		return new Payload(Json.JSON_CONTENT_UTF8, node)//
				.withCode(status)//
				.withHeaders(payload.headers())//
				.withCookies(payload.cookies());
	}

	//
	// Factory methods
	//

	public static JsonPayload ok() {
		return status(HttpStatus.OK);
	}

	public static JsonPayload created() {
		return status(HttpStatus.CREATED);
	}

	public static JsonPayload status(int status) {
		return new JsonPayload(status);
	}

	public static JsonPayload saved(boolean created, String uri, String type, String id) {

		JsonPayload payload2 = status(created ? HttpStatus.CREATED : HttpStatus.OK);
		payload2.object().put("id", id) //
				.put("type", type);

		return payload2.withLocation(uri, type, id)//
				.withLocation(uri, type, id)//
				.withHeader(SpaceHeaders.SPACEDOG_OBJECT_ID, id);
	}

	//
	// Error payloads
	//

	public static JsonPayload error(int httpStatus) {
		return status(httpStatus);
	}

	public static JsonPayload error(Throwable t) {
		return error(toStatus(t)).withError(t);
	}

	public static JsonPayload error(int httpStatus, String message, Object... args) {
		return error(httpStatus).withError(message, args);
	}

	public JsonPayload withError(String message, Object... args) {
		return withError(Json.object("message", String.format(message, args)));
	}

	public JsonPayload withError(Throwable throwable) {
		boolean debug = SpaceContext.isDebug() || payload.code() >= 500;
		return withError(toJson(throwable, debug));
	}

	public JsonPayload withError(JsonNode error) {
		this.object().set("error", error);
		return this;
	}

	public static int toStatus(Throwable t) {

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
			return toStatus(t.getCause());

		return HttpStatus.INTERNAL_SERVER_ERROR;
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

}
