package io.spacedog.services;

import io.spacedog.utils.Utils;
import net.codestory.http.Context;
import net.codestory.http.filters.Filter;

@FunctionalInterface
public interface SpaceFilter extends Filter {

	@Override
	default boolean matches(String uri, Context context) {
		return uri.length() > 3 && uri.startsWith("/v")//
				&& Utils.isDigit(uri.charAt(2)) && uri.charAt(3) == '/';
	}
}
