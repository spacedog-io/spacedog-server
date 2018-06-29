package io.spacedog.server;
/**
 * © David Attias 2015
 */

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;

import io.spacedog.utils.Exceptions;

public class J8 {

	public static List<String> toStrings(JsonNode node) {
		if (node.isArray())
			return Lists.newArrayList(node.elements())//
					.stream().map(element -> element.asText())//
					.collect(Collectors.toList());

		if (node.isObject())
			return Lists.newArrayList(node.elements())//
					.stream().map(element -> element.asText())//
					.collect(Collectors.toList());

		if (node.isValueNode())
			return Collections.singletonList(node.asText());

		throw Exceptions.illegalArgument(//
				"can not convert this json node [%s] to a list of strings", node);
	}

	public static <T, R> R map(T t, Function<T, R> f) {
		return map(t, f, null);
	}

	public static <T, R> R map(T t, Function<T, R> f, R defaultValue) {
		return t == null ? defaultValue : f.apply(t);
	}

}
