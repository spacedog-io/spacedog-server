package io.spacedog.server;
/**
 * © David Attias 2015
 */

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;

import io.spacedog.utils.Exceptions;

public class Json8 {

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

}
