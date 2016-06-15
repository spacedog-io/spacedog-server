/**
 * © David Attias 2015
 */
package io.spacedog.services;

import io.spacedog.utils.SpaceHeaders;
import net.codestory.http.Context;
import net.codestory.http.filters.PayloadSupplier;
import net.codestory.http.payload.Payload;

@SuppressWarnings("serial")
public class CrossOriginFilter implements SpaceFilter {

	@Override
	public Payload apply(String uri, Context context, PayloadSupplier nextFilter) throws Exception {

		// uri is already checked by SpaceFilter default matches method

		Payload payload = null;

		if (context.request().isPreflight()) {
			payload = Payload.ok() //
					.withAllowOrigin(context.request().header(SpaceHeaders.ORIGIN)) //
					.withMaxAge(31536000); // one year
		} else {
			payload = nextFilter.get().withAllowOrigin("*");
		}

		return payload.withAllowMethods(SpaceHeaders.ALLOW_METHODS) //
				.withAllowHeaders(SpaceHeaders.AUTHORIZATION, SpaceHeaders.CONTENT_TYPE, SpaceHeaders.SPACEDOG_DEBUG);
	}
}
