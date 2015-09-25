package io.spacedog.services;

import net.codestory.http.Context;
import net.codestory.http.constants.Headers;
import net.codestory.http.filters.Filter;
import net.codestory.http.filters.PayloadSupplier;
import net.codestory.http.payload.Payload;

@SuppressWarnings("serial")
public class CrossOriginFilter implements Filter {

	public static final String ALLOW_METHODS = "GET, POST, PUT, DELETE, HEAD";

	public Payload apply(String uri, Context context, PayloadSupplier nextFilter)
			throws Exception {

		Payload payload = null;

		if (context.request().isPreflight()) {
			payload = Payload.ok() //
					.withAllowOrigin(context.request().header(Headers.ORIGIN)) //
					.withMaxAge(31536000); // one year
		} else {
			payload = nextFilter.get().withAllowOrigin("*");
		}

		return payload.withAllowMethods(ALLOW_METHODS) //
				.withAllowHeaders(AccountResource.ACCOUNT_ID_HEADER,
						Headers.AUTHORIZATION, Headers.CONTENT_TYPE);
	}
}
