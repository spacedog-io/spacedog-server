package io.spacedog.services;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;

import io.spacedog.client.http.ContentTypes;
import io.spacedog.client.http.SpaceHeaders;
import io.spacedog.jobs.Internals;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;
import net.codestory.http.Context;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.filters.PayloadSupplier;
import net.codestory.http.payload.Payload;

public class ErrorFilters {

	@SuppressWarnings("serial")
	public static SpaceFilter global() {
		return new SpaceFilter() {

			@Override
			public Payload apply(String uri, Context context, PayloadSupplier nextFilter) {
				Payload payload = null;

				try {
					payload = nextFilter.get();
				} catch (IllegalStateException e) {
					// Fluent wraps non runtime exceptions into IllegalStateException
					payload = JsonPayload.error(e.getCause() == null ? e : e.getCause()).build();
				} catch (Throwable t) {
					payload = JsonPayload.error(t).build();
				}

				payload = checkResponseIsNull(uri, context, payload);
				notifySuperdogsIfInternalError(uri, context, payload);
				return payload;
			}
		};
	}

	@SuppressWarnings("serial")
	public static SpaceFilter specific() {
		return new SpaceFilter() {

			@Override
			public Payload apply(String uri, Context context, PayloadSupplier nextFilter) throws Exception {
				Payload payload = nextFilter.get();
				payload = checkResponseIsNull(uri, context, payload);
				payload = checkErrorWithoutContent(uri, context, payload);
				return payload;
			}

		};
	}

	private static Payload checkResponseIsNull(String uri, Context context, Payload payload) {
		if (payload == null)
			payload = JsonPayload.error(HttpStatus.INTERNAL_SERVER_ERROR) //
					.withError("[%s][%s] response is null", //
							context.method(), uri)//
					.build();
		return payload;
	}

	/**
	 * To prevent fluent http default behavior (return default 404 page), error
	 * payloads without content are filled with minimal content.
	 */
	private static Payload checkErrorWithoutContent(String uri, Context context, Payload payload) {
		if (payload.isError() && payload.rawContent() == null) {

			if (payload.code() == HttpStatus.NOT_FOUND)
				throw Exceptions.objectNotFound("path", uri);

			if (payload.code() == HttpStatus.METHOD_NOT_ALLOWED)
				throw Exceptions.methodNotAllowed(context.method(), uri);

			return JsonPayload.error(payload.code()) //
					.withError("no details available")//
					.build();

		}
		return payload;
	}

	private static void notifySuperdogsIfInternalError(String uri, Context context, Payload payload) {

		if (payload.code() < HttpStatus.INTERNAL_SERVER_ERROR)
			return;

		try {
			StringBuilder builder = new StringBuilder();
			builder.append(context.request().method())//
					.append(' ').append(uri).append(" => 500\n");

			appendQueryParams(builder, context.query().keyValues());
			appendHeaders(builder, context.request().headers());

			if (ContentTypes.isJsonContent(context.header(SpaceHeaders.CONTENT_TYPE)))
				appendContent(builder, "Request body", context.request().content());

			if (ContentTypes.isJsonContent(payload.rawContentType()))
				appendContent(builder, "Response body", payload.rawContent());

			Internals.get().notify(//
					String.format("%s is 500 500 500", uri), //
					builder.toString());

		} catch (Throwable t) {
			Utils.warn("unable to notify superdogs of an internal error", t);
		}
	}

	private static void appendQueryParams(StringBuilder builder, Map<String, String> map) {
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

	private static void appendHeaders(StringBuilder builder, Map<String, List<String>> map) {
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

	private static void appendContent(StringBuilder builder, String name, Object body) {

		try {
			if (body instanceof String)
				body = Json.readNode((String) body);

			else if (body instanceof byte[])
				body = Json.readNode((byte[]) body);

		} catch (Exception ignore) {
		}

		String bodyString = body instanceof JsonNode ? Json.toString(body, true) : body.toString();

		builder.append(name).append(" : ").append(bodyString).append('\n');
	}
}