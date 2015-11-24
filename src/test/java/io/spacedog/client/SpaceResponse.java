/**
 * © David Attias 2015
 */
package io.spacedog.client;

import java.io.IOException;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;

import io.spacedog.services.AdminResource;
import io.spacedog.services.Json;

public class SpaceResponse {

	private HttpRequest httpRequest;
	private HttpResponse<String> response;
	private DateTime before;
	private JsonNode jsonResponseContent;

	public SpaceResponse(HttpRequest request, JsonNode jsonRequestContent)
			throws JsonProcessingException, IOException, UnirestException {

		this.httpRequest = request;
		this.before = DateTime.now();

		response = httpRequest.asString();

		System.out.println();
		System.out.println(String.format("%s %s => %s => %s", httpRequest.getHttpMethod(), httpRequest.getUrl(),
				response.getStatus(), response.getStatusText()));

		httpRequest.getHeaders().forEach((key, value) -> printHeader(key, value));

		if (jsonRequestContent != null)
			System.out.println(String.format("Request body: %s",
					Json.getMapper().writerWithDefaultPrettyPrinter().writeValueAsString(jsonRequestContent)));

		String responseBody = response.getBody();
		if (Json.isJson(responseBody))
			jsonResponseContent = Json.readJsonNode(responseBody);

		response.getHeaders().forEach((key, value) -> System.out.println(String.format("=> %s: %s", key, value)));

		String responseContent = isJson()
				? Json.getMapper().writerWithDefaultPrettyPrinter().writeValueAsString(jsonResponseContent)
				: response.getBody().length() < 550 ? response.getBody()
						: response.getBody().substring(0, 500) + " ...";

		System.out.println(String.format("=> Response body: %s", responseContent));
	}

	private static void printHeader(String key, List<String> value) {
		if (key.equals(AdminResource.AUTHORIZATION_HEADER)) {
			AdminResource.decodeAuthorizationHeader(value.get(0)).ifPresent(
					tokens -> System.out.println(String.format("%s: %s (= %s:%s)", key, value, tokens[0], tokens[1])));
			return;
		}

		System.out.println(String.format("%s: %s", key, value));
	}

	public DateTime before() {
		return before;
	}

	public boolean isJson() {
		return jsonResponseContent != null;
	}

	public JsonNode jsonNode() {
		if (jsonResponseContent == null)
			throw new IllegalStateException("no json yet");
		return jsonResponseContent;
	}

	public ObjectNode objectNode() {
		if (!jsonNode().isObject())
			throw new RuntimeException(String.format("not a json object but [%s]", jsonResponseContent.getNodeType()));
		return (ObjectNode) jsonResponseContent;
	}

	public ArrayNode arrayNode() {
		if (!jsonNode().isArray())
			throw new RuntimeException(String.format("not a json array but [%s]", jsonResponseContent.getNodeType()));
		return (ArrayNode) jsonResponseContent;
	}

	public HttpResponse<String> httpResponse() {
		if (response == null)
			throw new IllegalStateException("no response yet");
		return response;
	}

	public JsonNode getFromJson(String jsonPath) {
		return Json.get(jsonResponseContent, jsonPath);
	}

	public JsonNode findValue(String fieldName) {
		return jsonResponseContent.findValue(fieldName);
	}

	public SpaceResponse assertEquals(String expected, String jsonPath) {
		Assert.assertEquals(expected, Json.get(jsonResponseContent, jsonPath).textValue());
		return this;
	}

	public SpaceResponse assertEquals(int expected, String jsonPath) {
		JsonNode node = Json.get(jsonResponseContent, jsonPath);
		Assert.assertEquals(expected, //
				node.isArray() || node.isObject() ? node.size() : node.intValue());
		return this;
	}

	public SpaceResponse assertEquals(long expected, String jsonPath) {
		Assert.assertEquals(expected, Json.get(jsonResponseContent, jsonPath).longValue());
		return this;
	}

	public SpaceResponse assertEquals(float expected, String jsonPath, float delta) {
		Assert.assertEquals(expected, Json.get(jsonResponseContent, jsonPath).floatValue(), delta);
		return this;
	}

	public SpaceResponse assertEquals(double expected, String jsonPath, double delta) {
		Assert.assertEquals(expected, Json.get(jsonResponseContent, jsonPath).doubleValue(), delta);
		return this;
	}

	public SpaceResponse assertEquals(DateTime expected, String jsonPath) {
		Assert.assertEquals(expected, DateTime.parse(Json.get(jsonResponseContent, jsonPath).asText()));
		return this;
	}

	public SpaceResponse assertEquals(JsonNode expected) {
		Assert.assertEquals(expected, jsonResponseContent);
		return this;
	}

	public SpaceResponse assertEqualsWithoutMeta(JsonNode expected) {
		Assert.assertEquals(expected, objectNode().deepCopy().without("meta"));
		return this;
	}

	public SpaceResponse assertNotNull(String jsonPath) {
		Assert.assertFalse(Json.get(jsonResponseContent, jsonPath).isNull());
		return this;
	}

	public SpaceResponse assertTrue(String jsonPath) {
		Assert.assertTrue(Json.get(jsonResponseContent, jsonPath).asBoolean());
		return this;
	}

	public SpaceResponse assertFalse(String jsonPath) {
		Assert.assertFalse(Json.get(jsonResponseContent, jsonPath).asBoolean());
		return this;
	}

}
