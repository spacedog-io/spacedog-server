package io.spacedog.services;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import io.spacedog.core.Json8;
import io.spacedog.jobs.Internals;
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

		if (payload.code() >= HttpStatus.INTERNAL_SERVER_ERROR)
			notifyInternalErrorToSuperdogs(uri, context, payload);

		// uri is already checked by SpaceFilter default matches method

		if (payload.isError() && payload.rawContent() == null) {

			JsonBuilder<ObjectNode> nodeBuilder = Json8.objectBuilder()//
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

			appendQueryParams(builder, context.query().keyValues());
			appendHeaders(builder, context.request().headers());

			if (!context.request().isUrlEncodedForm())
				appendContent(builder, "Request body", context.request().content());
			appendContent(builder, "Response body", payload.rawContent().toString());

			Internals.get().notify(//
					Start.get().configuration().awsSuperdogNotificationTopic().orElse(null), //
					String.format("%s is 500 500 500", uri), //
					builder.toString());

		} catch (Throwable t) {
			Utils.warn("failed to notify superdogs of an internal error", t);
		}
	}

	private void appendQueryParams(StringBuilder builder, Map<String, String> map) {
		builder.append("Request parameters :\n");
		for (Entry<String, String> entry : map.entrySet()) {

			if (Strings.isNullOrEmpty(entry.getValue()))
				continue;

			builder.append('\t')//
					.append(entry.getKey())//
					.append(" = ")//
					.append(entry.getValue())//
					.append('\n');
		}
	}

	private void appendHeaders(StringBuilder builder, Map<String, List<String>> map) {
		builder.append("Request headers :\n");
		for (Entry<String, List<String>> entry : map.entrySet()) {

			if (Utils.isNullOrEmpty(entry.getValue()))
				continue;

			builder.append('\t')//
					.append(entry.getKey())//
					.append(" = ")//
					.append(entry.getValue())//
					.append('\n');
		}
	}

	private void appendContent(StringBuilder builder, String name, String body) throws JsonProcessingException {
		builder.append(name).append(" : ");

		if (Json8.isJson(body))
			body = Json8.toPrettyString(Json8.readNode(body));

		builder.append(body).append('\n');
	}
}