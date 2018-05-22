/**
 * © David Attias 2015
 */
package io.spacedog.server;

import java.util.Map;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.search.SearchPhaseExecutionException;

import com.amazonaws.AmazonServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import io.spacedog.client.http.ContentTypes;
import io.spacedog.client.http.SpaceException;
import io.spacedog.client.http.SpaceFields;
import io.spacedog.client.http.SpaceHeaders;
import io.spacedog.utils.Json;
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
		object().put("location", SpaceService.spaceUrl(uri).toString());
		return this;
	}

	public JsonPayload withLocation(String uri, String type, String id) {
		object().put("location", SpaceService.spaceUrl(uri, type, id).toString());
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

	public JsonPayload withHeaders(Map<String, String> headers) {
		payload.withHeaders(headers);
		return this;
	}

	public JsonPayload withContent(Object object) {
		this.node = Json.toJsonNode(object);
		return this;
	}

	public JsonPayload withFields(Object... fields) {
		Json.addAll(object(), fields);
		return this;
	}

	public JsonPayload withResults(ArrayNode array) {
		this.object().set("results", array);
		return this;
	}

	public Payload build() {
		int status = payload.code();

		if (node == null)
			node = Json.object();

		if (node.isObject()) {
			ObjectNode object = (ObjectNode) node;

			if (status >= 400)
				object.put("success", false)//
						.put("status", status);

			Debug debug = Server.context().debug();
			if (debug.isTrue())
				object.set("debug", debug.toNode());
		}

		return new Payload(ContentTypes.JSON_UTF8, node)//
				.withCode(status)//
				.withHeaders(payload.headers())//
				.withCookies(payload.cookies());
	}

	//
	// Factory methods
	//

	public static JsonPayload status(int status) {
		return new JsonPayload(status);
	}

	public static JsonPayload ok() {
		return status(HttpStatus.OK);
	}

	public static JsonPayload created() {
		return status(HttpStatus.CREATED);
	}

	public static JsonPayload saved(boolean created) {
		return new JsonPayload(created ? HttpStatus.CREATED : HttpStatus.OK);
	}

	public static JsonPayload saved(boolean created, String uri, String type, String id) {
		return saved(created)//
				.withFields("id", id, "type", type)//
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
		boolean debug = Server.context().debug().isTrue() //
				|| payload.code() >= 500;
		JsonPayload payload = withError(toJson(throwable, debug));
		if (throwable instanceof SpaceException)
			payload.withHeaders(((SpaceException) throwable).headers());
		return payload;
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

	public static ObjectNode toJson(Throwable t, boolean debug) {
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
					json.with("details").set(key, Json.toJsonNode(elasticException.getHeader(key)));
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
