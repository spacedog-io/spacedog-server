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

			JsonBuilder<ObjectNode> node = Json.startObject().put("success", false)//
					.startObject("error");

			if (payload.code() == 404) {
				node.put("message", String.format("[%s] is not a valid SpaceDog route", uri));
				return new Payload(AbstractResource.JSON_CONTENT, node.toString(), 404);
			} else {
				node.put("message", "severe internal server error, no details available");
				return new Payload(AbstractResource.JSON_CONTENT, node.toString(), payload.code());
			}
		}
		return payload;
	}
}