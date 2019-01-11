package io.spacedog.services.log;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.http.ContentTypes;
import io.spacedog.client.http.SpaceFields;
import io.spacedog.client.http.SpaceHeaders;
import io.spacedog.server.Server;
import io.spacedog.server.Services;
import io.spacedog.server.SpaceFilter;
import io.spacedog.services.JsonPayload;
import io.spacedog.services.SpaceResty;
import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;
import net.codestory.http.Context;
import net.codestory.http.filters.PayloadSupplier;
import net.codestory.http.payload.Payload;

public class LogFilter implements SpaceFilter, SpaceFields {

	private static final long serialVersionUID = 5621427145724229373L;

	@Override
	public boolean matches(String uri, Context context) {

		// ping requests should not be logged
		if (uri.isEmpty() || uri.equals(SpaceResty.SLASH))
			return Server.context().isWww();

		return true;
	}

	@Override
	public Payload apply(String uri, Context context, PayloadSupplier nextFilter) throws Exception {
		Payload payload = null;
		DateTime receivedAt = DateTime.now();

		try {
			payload = nextFilter.get();
		} catch (Throwable t) {
			payload = JsonPayload.error(t).build();
		}

		try {
			log(uri, context, receivedAt, payload);
		} catch (Exception e) {
			// TODO: log platform unexpected error with a true logger
			e.printStackTrace();
		}

		return payload;
	}

	//
	// Implementation
	//

	private static final String PAYLOAD_FIELD = "payload";
	private static final String CREDENTIALS_FIELD = "credentials";
	private static final String PARAMETERS_FIELD = "parameters";
	private static final String HEADERS_FIELD = "headers";

	private String log(String uri, Context context, DateTime receivedAt, Payload payload) {

		ObjectNode log = Json.object(//
				"method", context.method(), //
				"path", uri, //
				RECEIVED_AT_FIELD, receivedAt.toString(), //
				"processedIn", DateTime.now().getMillis() - receivedAt.getMillis(), //
				"status", payload == null ? 500 : payload.code());

		addCredentials(log);
		addQuery(log, context);
		addHeaders(log, context.request().headers().entrySet());
		addRequestPayload(log, context);
		addResponsePayload(log, payload, context);

		return SpaceResty.elastic().index(Services.logs().index(), log).getId();
	}

	private void addResponsePayload(ObjectNode log, Payload payload, Context context) {
		if (payload != null) {
			String contentType = payload.rawContentType();
			// content type null means json
			if (contentType == null || ContentTypes.isJsonContent(contentType)) {
				Object rawContent = payload.rawContent();
				// the payload of resty methods returning void is ""
				// this doesn't fit in LogItem.response
				if (rawContent != null && !rawContent.equals(""))
					log.putPOJO("response", rawContent);
			}
		}
	}

	private void addRequestPayload(ObjectNode log, Context context) {

		try {
			String content = context.request().content();

			if (!Strings.isNullOrEmpty(content)) {

				// TODO: fix the content type problem
				// String contentType = context.request().contentType();
				// log.put("contentType", contentType);
				// if (PayloadHelper.JSON_CONTENT.equals(contentType))
				// log.node("content", content);
				// else
				// log.put("content", content);

				if (Json.isObject(content)) {
					JsonNode securedContent = Json.fullReplaceTextualFields(//
							Json.readNode(content), PASSWORD_FIELD, "********");

					log.set(PAYLOAD_FIELD, securedContent);
				}
			}
		} catch (Exception e) {
			log.set(PAYLOAD_FIELD, Json.object(ERROR_FIELD, JsonPayload.toJson(e, true)));
		}
	}

	private void addQuery(ObjectNode log, Context context) {
		ArrayNode parametersNode = Json.array();

		for (String key : context.query().keys()) {
			String value = key.equals(PASSWORD_FIELD) //
					? "********"
					: context.get(key);
			parametersNode.add(key + ": " + value);
		}

		if (parametersNode.size() > 0)
			log.set(PARAMETERS_FIELD, parametersNode);
	}

	private void addCredentials(ObjectNode log) {
		Credentials credentials = Server.context().credentials();
		ObjectNode logCredentials = log.putObject(CREDENTIALS_FIELD);
		logCredentials.put(ID_FIELD, credentials.id());
		logCredentials.put(USERNAME_FIELD, credentials.username());
		logCredentials.putPOJO(ROLES_FIELD, credentials.roles());
	}

	private void addHeaders(ObjectNode log, Set<Entry<String, List<String>>> headers) {

		ArrayNode headersNode = Json.array();
		for (Entry<String, List<String>> header : headers) {

			String key = header.getKey();
			List<String> values = header.getValue();

			if (key.equalsIgnoreCase(SpaceHeaders.AUTHORIZATION))
				continue;

			if (Utils.isNullOrEmpty(values))
				continue;

			for (String value : values)
				headersNode.add(key + ": " + value);
		}
		if (headersNode.size() > 0)
			log.set(HEADERS_FIELD, headersNode);
	}
}
