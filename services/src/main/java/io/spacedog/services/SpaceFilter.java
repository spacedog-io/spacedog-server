package io.spacedog.services;

import net.codestory.http.Context;
import net.codestory.http.filters.Filter;

@FunctionalInterface
public interface SpaceFilter extends Filter {

	@Override
	default boolean matches(String uri, Context context) {
		return uri.startsWith("/1/") || uri.startsWith("/v1/");
	}
}
