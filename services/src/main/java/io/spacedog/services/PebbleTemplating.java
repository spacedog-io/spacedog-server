/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.google.common.collect.Maps;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.loader.StringLoader;

import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;

public class PebbleTemplating {

	private PebbleEngine pebble;

	@SuppressWarnings("unchecked")
	Map<String, Object> createContext(Map<String, String> model, String body) {

		Map<String, Object> context = Maps.newHashMap();

		if (model == null || model.isEmpty())
			return context;

		String backendId = SpaceContext.target();

		try {
			Map<String, Object> parameters = Json.mapper().readValue(body, Map.class);

			for (Entry<String, Object> parameter : parameters.entrySet()) {
				String name = parameter.getKey();
				Object value = parameter.getValue();
				String type = model.get(name);

				if (type == null)
					throw Exceptions.illegalArgument("parameter [%s] is not authorized", name);

				if (checkValueSimpleAndValid(name, value, type)) {
					context.put(name, value);
					continue;
				}

				if (DataStore.get().isType(backendId, type)) {

					if (value != null && value instanceof String) {
						value = DataStore.get().getObject(backendId, type, value.toString());
						value = Json.mapper().convertValue(value, Map.class);
						context.put(name, value);
						continue;
					}

					throw Exceptions.illegalArgument("parameter value [%s][%s] is invalid", //
							name, value);
				}

				throw Exceptions.illegalArgument("parameter type [%s][%s] not found", //
						name, type);
			}
		} catch (IOException e) {
			throw Exceptions.illegalArgument(e, "error deserializing request payload");
		}

		return context;
	}

	String render(String propertyName, String propertyValue, Map<String, Object> context) {

		if (propertyValue == null)
			return null;

		try {
			StringWriter writer = new StringWriter();
			pebble.getTemplate(propertyValue).evaluate(writer, context);
			return writer.toString();

		} catch (Exception e) {
			throw Exceptions.illegalArgument(e, //
					"error rendering template property [%s]", propertyName);
		}
	}

	List<String> render(String propertyName, List<String> propertyValue, Map<String, Object> context) {

		if (propertyValue == null)
			return null;

		return propertyValue.stream()//
				.map(value -> render(propertyName, value, context))//
				.collect(Collectors.toList());
	}

	static boolean checkValueSimpleAndValid(String name, Object value, String type) {
		if ("string".equals(type))
			return checkValueType(name, value, String.class);
		if ("integer".equals(type))
			return checkValueType(name, value, Integer.class);
		if ("long".equals(type))
			return checkValueType(name, value, Long.class);
		if ("float".equals(type))
			return checkValueType(name, value, Float.class);
		if ("double".equals(type))
			return checkValueType(name, value, Double.class);
		if ("boolean".equals(type))
			return checkValueType(name, value, Boolean.class);
		if ("array".equals(type))
			return checkValueType(name, value, List.class);
		if ("object".equals(type))
			return checkValueType(name, value, Map.class);

		return false;
	}

	private static <T> boolean checkValueType(String name, Object value, Class<T> type) {
		if (!type.isAssignableFrom(value.getClass()))
			throw Exceptions.illegalArgument("parameter value type [%s][%s] invalid", //
					name, value.getClass().getSimpleName());

		return true;
	}

	//
	// singleton
	//

	private static PebbleTemplating singleton = new PebbleTemplating();

	static PebbleTemplating get() {
		return singleton;
	}

	private PebbleTemplating() {
		pebble = new PebbleEngine.Builder().loader(new StringLoader()).build();
	}
}
