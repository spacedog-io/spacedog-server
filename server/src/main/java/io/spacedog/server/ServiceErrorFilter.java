package io.spacedog.server;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;

import io.spacedog.client.http.ContentTypes;
import io.spacedog.client.http.SpaceHeaders;
import io.spacedog.jobs.Internals;
import io.spacedog.utils.Json;
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
			payload = e.getCause() != null ? JsonPayload.error(e.getCause()).build()//
					: JsonPayload.error(e).build();
		} catch (Throwable t) {
			payload = JsonPayload.error(t).build();
		}

		if (payload == null)
			payload = JsonPayload.error(HttpStatus.INTERNAL_SERVER_ERROR) //
					.withError("[%s][%s] response payload is null", //
							context.method(), uri)//
					.build();

		if (payload.code() >= HttpStatus.INTERNAL_SERVER_ERROR)
			notifyInternalErrorToSuperdogs(uri, context, payload);

		// uri is already checked by SpaceFilter default matches method

		if (payload.isError() && payload.rawContent() == null) {

			if (payload.code() == HttpStatus.NOT_FOUND)
				return JsonPayload.error(payload.code()) //
						.withError("path [%s] invalid", uri).build();

			else if (payload.code() == HttpStatus.METHOD_NOT_ALLOWED)
				return JsonPayload.error(payload.code()) //
						.withError("method [%s] invalid for path [%s]", context.method(), uri)//
						.build();

			else
				return JsonPayload.error(payload.code()) //
						.withError("no details available for this error")//
						.build();

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

			if (ContentTypes.isJsonContent(context.header(SpaceHeaders.CONTENT_TYPE)))
				appendContent(builder, "Request body", context.request().content());
			if (ContentTypes.isJsonContent(payload.rawContentType()))
				appendContent(builder, "Response body", payload.rawContent().toString());

			Internals.get().notify(//
					ServerConfig.awsSuperdogNotificationTopic().orElse(null), //
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

		if (Json.isJson(body))
			body = Json.toPrettyString(Json.readNode(body));

		builder.append(body).append('\n');
	}
}