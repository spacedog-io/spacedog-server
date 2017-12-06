package io.spacedog.utils;

import javax.activation.MimetypesFileTypeMap;

public class ContentTypes {

	private static MimetypesFileTypeMap typeMap = new MimetypesFileTypeMap();

	public static String parseFileExtension(String fileName) {
		return typeMap.getContentType(fileName);
	}
}
