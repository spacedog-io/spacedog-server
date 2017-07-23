package io.spacedog.utils;

import com.google.common.io.Resources;

public class ClassResources {

	public static String loadToString(Object context, String resourceName) {
		try {
			Class<?> contextClass = context instanceof Class<?> //
					? (Class<?>) context
					: context.getClass();

			return Resources.toString(//
					Resources.getResource(contextClass, resourceName), //
					Utils.UTF8);

		} catch (Exception e) {
			throw Exceptions.runtime(e, "error loading resource [%s]", resourceName);
		}
	}

}
