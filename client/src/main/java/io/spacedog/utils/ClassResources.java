package io.spacedog.utils;

import java.net.URL;

import com.google.common.io.Resources;

public class ClassResources {

	public static String loadAsString(Object context, String resourceName) {
		try {
			return Resources.toString(//
					toUrl(context, resourceName), Utils.UTF8);

		} catch (Exception e) {
			throw Exceptions.runtime(e, "error loading resource [%s]", resourceName);
		}
	}

	public static byte[] loadAsBytes(Object context, String resourceName) {
		try {
			return Resources.toByteArray(toUrl(context, resourceName));

		} catch (Exception e) {
			throw Exceptions.runtime(e, "error loading resource [%s]", resourceName);
		}
	}

	public static URL toUrl(Object context, String resourceName) {
		Class<?> contextClass = context instanceof Class<?> //
				? (Class<?>) context
				: context.getClass();

		return Resources.getResource(contextClass, resourceName);
	}
}
