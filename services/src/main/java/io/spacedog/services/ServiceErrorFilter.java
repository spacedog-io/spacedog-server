package io.spacedog.services;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.utils.Internals;
import io.spacedog.utils.Json;
import io.spacedog.utils.JsonBuilder;
import io.spacedog.utils.Utils;
import net.codestory.http.Context;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.filters.PayloadSupplier;
import net.codestory.http.payload.Payload;

@SuppressWarnings("serial")
public class ServiceErrorFilter implements SpaceFilter {

	@Override
	public Payload apply(String uri, Context context, PayloadSupplier nextFilter) throws Exception {

		Payload payload = null;

		try {
			payload = nextFilter.get();
		} catch (IllegalStateException e) {
			// Fluent wraps non runtime exceptions into IllegalStateException
			// let's unwrap them
			payload = e.getCause() != null ? JsonPayload.error(e.getCause())//
					: JsonPayload.error(e);
		} catch (Throwable t) {
			payload = JsonPayload.error(t);
		}

		if (payload == null)
			payload = JsonPayload.error(HttpStatus.INTERNAL_SERVER_ERROR, //
					"[%s][%s] response payload is null", context.method(), uri);

		if (payload.code() == HttpStatus.INTERNAL_SERVER_ERROR)
			notifyInternalErrorToSuperdogs(uri, context, payload);

		// uri is already checked by SpaceFilter default matches method

		if (payload.isError() && payload.rawContent() == null) {

			JsonBuilder<ObjectNode> nodeBuilder = Json.objectBuilder()//
					.put("success", false)//
					.put("status", payload.code())//
					.object("error");

			if (payload.code() == HttpStatus.NOT_FOUND) {
				nodeBuilder.put("message", //
						String.format("[%s] not a valid path", uri));
				return JsonPayload.json(nodeBuilder, payload.code());
			} else if (payload.code() == HttpStatus.METHOD_NOT_ALLOWED) {
				nodeBuilder.put("message", //
						String.format("method [%s] not valid for path [%s]", context.method(), uri));
				return JsonPayload.json(nodeBuilder, payload.code());
			} else {
				nodeBuilder.put("message", "no details available for this error");
				return JsonPayload.json(nodeBuilder, payload.code());
			}
		}
		return payload;
	}

	private void notifyInternalErrorToSuperdogs(String uri, Context context, Payload payload) throws IOException {
		try {
			StringBuilder builder = new StringBuilder();
			builder.append(context.request().method())//
					.append(' ').append(uri).append(" => 500\n");

			appendMap(builder, "Request parameters", context.query().keyValues());
			appendMap(builder, "Request headers", context.request().headers());

			appendBody(builder, "Request body", context.request().content());
			appendBody(builder, "Response body", payload.rawContent().toString());

			Internals.get().notify(//
					Start.get().configuration().superdogAwsNotificationTopic(), //
					String.format("%s is 500 500 500", uri), //
					builder.toString());

		} catch (Throwable t) {
			Utils.warn("failed to notify superdogs of an internal error", t);
		}
	}

	private void appendMap(StringBuilder builder, String name, Map<?, ?> map) {
		builder.append(name).append(" =\n");
		for (Entry<?, ?> entry : map.entrySet()) {
			builder.append('\t')//
					.append(entry.getKey())//
					.append(" : ")//
					.append(entry.getValue())//
					.append('\n');
		}
	}

	private void appendBody(StringBuilder builder, String name, String body) throws JsonProcessingException {
		builder.append(name).append(" = ");

		if (Json.isJson(body))
			body = Json.toPrettyString(Json.readNode(body));

		builder.append(body).append('\n');
	}

}