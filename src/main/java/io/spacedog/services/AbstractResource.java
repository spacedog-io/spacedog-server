package io.spacedog.services;

import io.spacedog.services.Account.InvalidAccountException;
import io.spacedog.services.SchemaResource.NotFoundException;
import io.spacedog.services.SchemaValidator.InvalidSchemaException;

import java.util.concurrent.ExecutionException;

import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.mapper.MergeMappingException;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class AbstractResource {

	public static final String JSON_CONTENT = "application/json;charset=UTF-8";
	public static final String HEADER_OBJECT_ID = "x-spacedog-object-id";
	public static final String BASE_URL = "https://spacedog.io";

	private static ObjectMapper objectMapper = new ObjectMapper();

	protected static ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	protected Payload checkExistence(String index, String type, String field,
			String value) {
		try {
			return ElasticHelper.search(index, type, field, value)
					.getTotalHits() == 0 ? Payload.notFound() : Payload.ok();

		} catch (Throwable throwable) {
			return error(throwable);
		}
	}

	public static String toJsonString(Throwable e) {
		return toJsonObject(e).toString();
	}

	public static JsonObject toJsonObject(Throwable e) {
		return add(new JsonObject(), e);
	}

	private static JsonObject add(JsonObject error, Throwable t) {
		JsonArray stack = new JsonArray();
		error.add("type", t.getClass().getName()) //
				.add("message", t.getMessage()) //
				.add("trace", stack);

		for (StackTraceElement element : t.getStackTrace()) {
			stack.add(element.toString());
		}

		if (t.getCause() != null) {
			JsonObject cause = new JsonObject();
			add(cause, t.getCause());
			error.add("cause", cause);
		}
		return error;
	}

	public static Payload success() {
		return new Payload(JSON_CONTENT, "{\"success\":true}", HttpStatus.OK);
	}

	public static Payload error(int httpStatus) {
		return error(httpStatus, null);
	}

	public static Payload error(Throwable t) {

		if (t instanceof AuthenticationException) {
			return error(HttpStatus.UNAUTHORIZED, t);
		}
		if (t instanceof InvalidAccountException) {
			return error(HttpStatus.BAD_REQUEST, t);
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

	public static Payload error(int httpStatus, Throwable throwable) {
		JsonBuilder builder = Json.builder().add("success", false);
		if (throwable != null)
			builder.addJson("error", toJsonObject(throwable));
		return new Payload(JSON_CONTENT, builder.build().toString(), httpStatus);
	}

	public static Payload error(int httpStatus, String message, Object... args) {
		return error(httpStatus,
				new RuntimeException(String.format(message, args)));
	}

	/**
	 * @param parameters
	 *            triples with parameter name, value and message
	 * @return a bad request http payload with a json listing invalid parameters
	 */
	protected static Payload invalidParameters(String... parameters) {
		JsonBuilder builder = Json.builder().add("success", false);
		if (parameters.length > 0 && parameters.length % 3 == 0) {
			builder = builder.stObj("invalidParameters");
			for (int i = 0; i < parameters.length; i += 3)
				builder = builder.stObj(parameters[0])
						.add("value", parameters[1])
						.add("message", parameters[2]);
		}
		return new Payload(JSON_CONTENT, builder.build().toString(),
				HttpStatus.BAD_REQUEST);
	}

	protected static Payload created(String uri, String type, String id) {
		return new Payload(JSON_CONTENT, Json.builder() //
				.add("success", true) //
				.add("id", id).add("type", type) //
				.add("location", toUrl(BASE_URL, uri, type, id)) //
				.build().toString(), HttpStatus.CREATED).withHeader(
				AbstractResource.HEADER_OBJECT_ID, id);
	}

	protected static String toUrl(String baseUrl, String uri, String type,
			String id) {
		return new StringBuilder(baseUrl).append(uri).append('/').append(type)
				.append('/').append(id).toString();
	}

	protected Payload extractResults(SearchResponse response) {
		JsonBuilder builder = Json.builder()
				.add("took", response.getTookInMillis())
				.add("total", response.getHits().getTotalHits())
				.stArr("results");

		for (SearchHit hit : response.getHits().getHits()) {
			JsonObject object = JsonObject.readFrom(hit.sourceAsString());
			object.get("meta").asObject().add("id", hit.id())
					.add("type", hit.type()).add("version", hit.version());
			builder.addJson(object);
		}

		return new Payload(JSON_CONTENT, builder.build().toString(),
				HttpStatus.OK);
	}
}
