/**
 * © David Attias 2015
 */
package io.spacedog.services;

import net.codestory.http.Context;
import net.codestory.http.constants.Headers;
import net.codestory.http.filters.PayloadSupplier;
import net.codestory.http.payload.Payload;

@SuppressWarnings("serial")
public class CrossOriginFilter implements SpaceFilter {

	public static final String ALLOW_METHODS = "GET, POST, PUT, DELETE, HEAD";

	@Override
	public Payload apply(String uri, Context context, PayloadSupplier nextFilter) throws Exception {

		// uri is already checked by SpaceFilter default matches method

		Payload payload = null;

		if (context.request().isPreflight()) {
			payload = Payload.ok() //
					.withAllowOrigin(context.request().header(Headers.ORIGIN)) //
					.withMaxAge(31536000); // one year
		} else {
			payload = nextFilter.get().withAllowOrigin("*");
		}

		return payload.withAllowMethods(ALLOW_METHODS) //
				.withAllowHeaders(SpaceContext.BACKEND_KEY_HEADER, Headers.AUTHORIZATION, Headers.CONTENT_TYPE);
	}
}
