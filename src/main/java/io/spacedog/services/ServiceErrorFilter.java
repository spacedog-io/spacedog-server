package io.spacedog.services;

import com.fasterxml.jackson.databind.node.ObjectNode;

import net.codestory.http.Context;
import net.codestory.http.filters.Filter;
import net.codestory.http.filters.PayloadSupplier;
import net.codestory.http.payload.Payload;

@SuppressWarnings("serial")
public class ServiceErrorFilter implements Filter {

	@Override
	public Payload apply(String uri, Context context, PayloadSupplier nextFilter) throws Exception {

		Payload payload = null;

		try {
			payload = nextFilter.get();
		} catch (IllegalStateException e) {
			// Fluent wraps non runtime exceptions into IllegalStateException
			// let's unwrap them
			payload = e.getCause() != null ? PayloadHelper.error(e.getCause())//
					: PayloadHelper.error(e);
		} catch (Throwable t) {
			payload = PayloadHelper.error(t);
		}

		if (uri.startsWith("/v") //
				&& uri.charAt(2) == '1' //
				&& payload.isError() //
				&& (payload.rawContentType() == null//
						|| !payload.rawContentType().startsWith("application/json"))) {

			JsonBuilder<ObjectNode> node = Json.objectBuilder().put("success", false)//
					.object("error");

			if (payload.code() == 404) {
				node.put("message", String.format("[%s] is not a valid SpaceDog route", uri));
				return new Payload(AbstractResource.JSON_CONTENT, node.toString(), payload.code());
			} else if (payload.code() == 405) {
				node.put("message", String.format("method [%s] not valid SpaceDog route [%s]", context.method(), uri));
				return new Payload(AbstractResource.JSON_CONTENT, node.toString(), payload.code());
			} else {
				node.put("message",
						String.format("sorry but no details available for this error code [%s]", payload.code()));
				return new Payload(AbstractResource.JSON_CONTENT, node.toString(), payload.code());
			}
		}
		return payload;
	}
}