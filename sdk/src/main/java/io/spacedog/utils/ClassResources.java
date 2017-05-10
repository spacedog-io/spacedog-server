package io.spacedog.utils;

import com.google.common.io.Resources;

public class ClassResources {

	public static String loadToString(Object context, String resourceName) {
		try {
			return Resources.toString(//
					Resources.getResource(context.getClass(), resourceName), //
					Utils.UTF8);

		} catch (Exception e) {
			throw Exceptions.runtime(e, "error loading resource [%s]", resourceName);
		}
	}

}
