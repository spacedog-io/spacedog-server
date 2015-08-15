package com.magiclabs.restapi;

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

		if (context.request().isPreflight()) {
			return Payload.ok() //
					.withAllowOrigin(context.request().header(Headers.ORIGIN)) //
					.withAllowMethods(ALLOW_METHODS) //
					.withMaxAge(31536000); // one year
		}
		return nextFilter.get().withAllowOrigin("*")
				.withAllowMethods(ALLOW_METHODS);
	}
}
