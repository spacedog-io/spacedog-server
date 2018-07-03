package io.spacedog.utils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;

public class Utils {

	public static final Charset UTF8 = Charset.forName("UTF-8");
	public static final char UTF8_BOM = '\ufeff';

	//
	// Object utils
	//

	public static <K> K instantiate(Class<K> objectClass) {
		try {
			return objectClass.newInstance();

		} catch (InstantiationException | IllegalAccessException e) {
			throw Exceptions.runtime(e, "error instantiating [%s] object class", //
					objectClass.getSimpleName());
		}
	}

	@SuppressWarnings("unchecked")
	public static <K> K instantiate(TypeReference<K> typeRef) {
		Type type = typeRef.getType();

		if (type instanceof Class<?>)
			return instantiate((Class<K>) type);

		if (type instanceof ParameterizedType)
			return instantiate((Class<K>) ((ParameterizedType) type).getRawType());

		throw Exceptions.illegalArgument(//
				"type reference [%s] is not instanciable", type);
	}

	//
	// Collections utils
	//

	public static boolean containsIgnoreCase(Collection<String> strings, String value) {
		for (String string : strings)
			if (value.equalsIgnoreCase(string))
				return true;
		return false;
	}

	public static boolean equalsIgnoreCase(List<String> list1, List<String> list2) {
		if (list1.size() != list2.size())
			return false;
		for (int i = 0; i < list1.size(); i++)
			if (!list1.get(i).equalsIgnoreCase(list2.get(i)))
				return false;
		return true;
	}

	//
	// Exceptions utils
	//

	public static StackTraceElement getStackTraceElement() {
		return new Exception().getStackTrace()[1];
	}

	public static StackTraceElement getParentStackTraceElement() {
		return new Exception().getStackTrace()[2];
	}

	public static StackTraceElement getGrandParentStackTraceElement() {
		return new Exception().getStackTrace()[3];
	}

	//
	// Strings utils
	//

	public static String join(CharSequence delimiter, CharSequence... elements) {
		Objects.requireNonNull(delimiter);
		Objects.requireNonNull(elements);
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (CharSequence cs : elements) {
			if (!first)
				builder.append(delimiter);
			builder.append(cs);
			first = false;
		}
		return builder.toString();
	}

	public static String join(CharSequence delimiter, Iterable<? extends CharSequence> elements) {
		Objects.requireNonNull(delimiter);
		Objects.requireNonNull(elements);
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (CharSequence cs : elements) {
			if (!first)
				builder.append(delimiter);
			builder.append(cs);
			first = false;
		}
		return builder.toString();
	}

	public static String concat(Object... objects) {
		StringBuilder builder = new StringBuilder();
		for (Object object : objects)
			builder.append(object.toString());
		return builder.toString();
	}

	public static String[] splitByDot(String s) {
		return split(s, "\\.");
	}

	public static String[] splitBySlash(String s) {
		return split(s, "/");
	}

	public static String[] splitByDash(String s) {
		return split(s, "-");
	}

	public static String[] split(String s, String regex) {
		String unEscapedRegex = regex.replace("\\", "");
		s = s.trim();
		if (s.startsWith(unEscapedRegex))
			s = s.substring(1);
		if (s.endsWith(unEscapedRegex))
			s = s.substring(0, s.length() - 1);
		return s.split(regex);
	}

	public static String removePreffix(String s, String preffix) {
		if (Strings.isNullOrEmpty(preffix))
			return s;
		return s.startsWith(preffix) ? s.substring(preffix.length()) : s;
	}

	public static String removeSuffix(String s, String suffix) {
		if (Strings.isNullOrEmpty(suffix))
			return s;
		return s.endsWith(suffix) ? s.substring(0, s.length() - suffix.length()) : s;
	}

	public static String replaceTagDelimiters(String s) {
		return s.replace('<', '(').replace('>', ')');
	}

	private static final Pattern NONLATIN = Pattern.compile("[^\\w_-]");
	private static final Pattern SEPARATORS = Pattern.compile("[\\s\\p{Punct}&&[^-]]");

	public static String slugify(String input) {
		String noseparators = SEPARATORS.matcher(input).replaceAll("-");
		String normalized = Normalizer.normalize(noseparators, Form.NFD);
		String slug = NONLATIN.matcher(normalized).replaceAll("");
		return slug.toLowerCase(Locale.ENGLISH)//
				.replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
	}

	//
	// Others
	//

	public static String toUri(String[] uriTerms) {
		StringBuilder builder = new StringBuilder();
		for (String term : uriTerms)
			builder.append('/').append(term);
		return builder.toString();
	}

	public static boolean isDigit(char c) {
		return c == '1' || c == '2' || c == '3' || c == '4' || c == '5' //
				|| c == '6' || c == '7' || c == '8' || c == '9';
	}

	public static boolean isNullOrEmpty(Collection<?> collection) {
		return collection == null || collection.isEmpty();
	}

	public static boolean isNullOrEmpty(Map<?, ?> map) {
		return map == null || map.isEmpty();
	}

	public static <T> boolean isNullOrEmpty(T[] array) {
		return array == null || array.length == 0;
	}

	public static void info() {
		System.out.println();
	}

	public static void info(String message, Object... arguments) {
		System.out.println(String.format(message, arguments));
	}

	public static void infoNoLn(String message, Object... arguments) {
		System.out.print(String.format(message, arguments));
	}

	public static void info(String nodeName, JsonNode node) {
		Utils.info("%s = %s", nodeName, Json.toPrettyString(node));
	}

	public static void warn(String message, Object... arguments) {
		System.err.println("[SpaceDog][Warning] " + String.format(message, arguments));
	}

	public static void warn(String message, Throwable t) {
		System.err.println("[SpaceDog][Warning] " + message);
		t.printStackTrace();
	}

	//
	// Array utils
	//

	public static String[] toArray(String... parts) {
		return parts;
	}

	public static Object[] toArray(Object... parts) {
		return parts;
	}

	//
	// Resource Utils
	//

	public static void closeSilently(Closeable closeable) {
		try {
			if (closeable != null)
				closeable.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//
	// Streams
	//

	public static byte[] toByteArray(InputStream stream) {
		try {
			return ByteStreams.toByteArray(stream);
		} catch (IOException e) {
			throw Exceptions.runtime(e, "unable to read input stream");
		} finally {
			Utils.closeSilently(stream);
		}
	}

}
