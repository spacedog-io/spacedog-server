package io.spacedog.client.http;

import javax.activation.MimetypesFileTypeMap;

import com.google.common.base.Strings;

public class ContentTypes {

	public static final String JSON = "application/json";
	public static final String JSON_UTF8 = "application/json;charset=utf-8;";
	public static final String TEXT_PLAIN_UTF8 = "text/plain;charset=utf-8;";
	public static final String PDF = "application/pdf";
	public static final String OCTET_STREAM = "application/octet-stream";

	private static MimetypesFileTypeMap typeMap = new MimetypesFileTypeMap();

	public static String parseFileExtension(String fileName) {
		return Strings.isNullOrEmpty(fileName) //
				? OCTET_STREAM//
				: typeMap.getContentType(fileName.toLowerCase());
	}

	public static boolean isJsonContent(String contentType) {
		if (Strings.isNullOrEmpty(contentType))
			return false;
		return contentType.toLowerCase().startsWith(JSON);
	}

}