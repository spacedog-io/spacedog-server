/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.google.common.collect.Maps;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.loader.StringLoader;

import io.spacedog.client.TemplateParameterTypes;
import io.spacedog.client.credentials.Credentials;
import io.spacedog.server.Services;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;

public class PebbleTemplating implements TemplateParameterTypes {

	private PebbleEngine pebble;

	public Map<String, Object> createContext(Map<String, String> model, Map<String, Object> parameters) {

		Map<String, Object> context = Maps.newHashMap();

		if (parameters == null || parameters.isEmpty())
			return context;

		if (model == null || model.isEmpty())
			throw Exceptions.illegalArgument("no parameter authorized for this template");

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

			if (type.equals(Credentials.TYPE)) {
				if (value != null && value instanceof String) {
					value = Services.credentials().get(value.toString());
					value = Json.mapper().convertValue(value, Map.class);
					context.put(name, value);
					continue;
				}

				throw Exceptions.illegalArgument("parameter value [%s][%s] is invalid", //
						name, value);
			}

			if (Services.data().isType(type)) {

				if (value != null && value instanceof String) {
					context.put(name, Services.data()//
							.get(type, value.toString(), Map.class));
					continue;
				}

				throw Exceptions.illegalArgument("parameter value [%s][%s] is invalid", //
						name, value);
			}

			throw Exceptions.illegalArgument("parameter type [%s][%s] not found", //
					name, type);
		}

		return context;
	}

	public String render(String propertyName, String propertyValue, Map<String, Object> context) {

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

	public List<String> render(String propertyName, List<String> propertyValue, Map<String, Object> context) {

		if (propertyValue == null)
			return null;

		return propertyValue.stream()//
				.map(value -> render(propertyName, value, context))//
				.collect(Collectors.toList());
	}

	static boolean checkValueSimpleAndValid(String name, Object value, String type) {
		if (string.equals(type))
			return checkValueType(name, value, String.class);
		if (integer.equals(type))
			return checkValueType(name, value, Integer.class);
		if (longg.equals(type))
			return checkValueType(name, value, Long.class);
		if (floatt.equals(type))
			return checkValueType(name, value, Float.class);
		if (doublee.equals(type))
			return checkValueType(name, value, Double.class);
		if (booleann.equals(type))
			return checkValueType(name, value, Boolean.class);
		if (array.equals(type))
			return checkValueType(name, value, List.class);
		if (object.equals(type))
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

	public static PebbleTemplating get() {
		return singleton;
	}

	private PebbleTemplating() {
		pebble = new PebbleEngine.Builder().loader(new StringLoader()).build();
	}
}
