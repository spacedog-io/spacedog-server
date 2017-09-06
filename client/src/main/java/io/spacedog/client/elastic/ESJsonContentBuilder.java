/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.spacedog.client.elastic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.joda.time.ReadableInstant;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.google.common.base.Charsets;

/**
 *
 */
public final class ESJsonContentBuilder {

	public final static DateTimeFormatter defaultDatePrinter = ISODateTimeFormat.dateTime();

	private ESJsonContentGenerator generator;

	private final ByteArrayOutputStream bos;

	private boolean humanReadable = false;

	/**
	 * Constructs a new builder using the provided xcontent and an OutputStream.
	 * Make sure to call {@link #close()} when the builder is done with.
	 */
	public ESJsonContentBuilder() throws IOException {
		this.bos = new ByteArrayOutputStream();
		this.generator = new ESJsonContentGenerator(bos);
	}

	public ESJsonContentBuilder prettyPrint() {
		generator.usePrettyPrint();
		return this;
	}

	public ESJsonContentBuilder lfAtEnd() {
		generator.usePrintLineFeedAtEnd();
		return this;
	}

	public ESJsonContentBuilder humanReadable(boolean humanReadable) {
		this.humanReadable = humanReadable;
		return this;
	}

	public boolean humanReadable() {
		return this.humanReadable;
	}

	public ESJsonContentBuilder startObject(String name) throws IOException {
		field(name);
		startObject();
		return this;
	}

	public ESJsonContentBuilder startObject() throws IOException {
		generator.writeStartObject();
		return this;
	}

	public ESJsonContentBuilder endObject() throws IOException {
		generator.writeEndObject();
		return this;
	}

	public ESJsonContentBuilder array(String name, String... values) throws IOException {
		startArray(name);
		for (String value : values) {
			value(value);
		}
		endArray();
		return this;
	}

	public ESJsonContentBuilder array(String name, Object... values) throws IOException {
		startArray(name);
		for (Object value : values) {
			value(value);
		}
		endArray();
		return this;
	}

	public ESJsonContentBuilder startArray(String name) throws IOException {
		field(name);
		startArray();
		return this;
	}

	public ESJsonContentBuilder startArray() throws IOException {
		generator.writeStartArray();
		return this;
	}

	public ESJsonContentBuilder endArray() throws IOException {
		generator.writeEndArray();
		return this;
	}

	public ESJsonContentBuilder field(String name) throws IOException {
		if (name == null) {
			throw new IllegalArgumentException("field name cannot be null");
		}
		generator.writeFieldName(name);
		return this;
	}

	public ESJsonContentBuilder field(String name, char[] value, int offset, int length) throws IOException {
		field(name);
		if (value == null) {
			generator.writeNull();
		} else {
			generator.writeString(value, offset, length);
		}
		return this;
	}

	public ESJsonContentBuilder field(String name, String value) throws IOException {
		field(name);
		if (value == null) {
			generator.writeNull();
		} else {
			generator.writeString(value);
		}
		return this;
	}

	public ESJsonContentBuilder field(String name, Integer value) throws IOException {
		field(name);
		if (value == null) {
			generator.writeNull();
		} else {
			generator.writeNumber(value.intValue());
		}
		return this;
	}

	public ESJsonContentBuilder field(String name, int value) throws IOException {
		field(name);
		generator.writeNumber(value);
		return this;
	}

	public ESJsonContentBuilder field(String name, Long value) throws IOException {
		field(name);
		if (value == null) {
			generator.writeNull();
		} else {
			generator.writeNumber(value.longValue());
		}
		return this;
	}

	public ESJsonContentBuilder field(String name, long value) throws IOException {
		field(name);
		generator.writeNumber(value);
		return this;
	}

	public ESJsonContentBuilder field(String name, Float value) throws IOException {
		field(name);
		if (value == null) {
			generator.writeNull();
		} else {
			generator.writeNumber(value.floatValue());
		}
		return this;
	}

	public ESJsonContentBuilder field(String name, float value) throws IOException {
		field(name);
		generator.writeNumber(value);
		return this;
	}

	public ESJsonContentBuilder field(String name, Double value) throws IOException {
		field(name);
		if (value == null) {
			generator.writeNull();
		} else {
			generator.writeNumber(value);
		}
		return this;
	}

	public ESJsonContentBuilder field(String name, double value) throws IOException {
		field(name);
		generator.writeNumber(value);
		return this;
	}

	public ESJsonContentBuilder field(String name, BigDecimal value) throws IOException {
		return field(name, value, value.scale(), RoundingMode.HALF_UP, true);
	}

	public ESJsonContentBuilder field(String name, BigDecimal value, int scale, RoundingMode rounding, boolean toDouble)
			throws IOException {
		field(name);
		if (value == null) {
			generator.writeNull();
		} else {
			if (toDouble) {
				try {
					generator.writeNumber(value.setScale(scale, rounding).doubleValue());
				} catch (ArithmeticException e) {
					generator.writeString(value.toEngineeringString());
				}
			} else {
				generator.writeString(value.toEngineeringString());
			}
		}
		return this;
	}

	public ESJsonContentBuilder field(String name, Map<String, Object> value) throws IOException {
		field(name);
		value(value);
		return this;
	}

	public ESJsonContentBuilder field(String name, Iterable<?> value) throws IOException {
		startArray(name);
		for (Object o : value) {
			value(o);
		}
		endArray();
		return this;
	}

	public ESJsonContentBuilder field(String name, boolean... value) throws IOException {
		startArray(name);
		for (boolean o : value) {
			value(o);
		}
		endArray();
		return this;
	}

	public ESJsonContentBuilder field(String name, String... value) throws IOException {
		startArray(name);
		for (String o : value) {
			value(o);
		}
		endArray();
		return this;
	}

	public ESJsonContentBuilder field(String name, Object... value) throws IOException {
		startArray(name);
		for (Object o : value) {
			value(o);
		}
		endArray();
		return this;
	}

	public ESJsonContentBuilder field(String name, int... value) throws IOException {
		startArray(name);
		for (Object o : value) {
			value(o);
		}
		endArray();
		return this;
	}

	public ESJsonContentBuilder field(String name, long... value) throws IOException {
		startArray(name);
		for (Object o : value) {
			value(o);
		}
		endArray();
		return this;
	}

	public ESJsonContentBuilder field(String name, float... value) throws IOException {
		startArray(name);
		for (Object o : value) {
			value(o);
		}
		endArray();
		return this;
	}

	public ESJsonContentBuilder field(String name, double... value) throws IOException {
		startArray(name);
		for (Object o : value) {
			value(o);
		}
		endArray();
		return this;
	}

	public ESJsonContentBuilder field(String name, Object value) throws IOException {
		field(name);
		writeValue(value);
		return this;
	}

	public ESJsonContentBuilder value(Object value) throws IOException {
		writeValue(value);
		return this;
	}

	public ESJsonContentBuilder field(String name, boolean value) throws IOException {
		field(name);
		generator.writeBoolean(value);
		return this;
	}

	public ESJsonContentBuilder field(String name, ReadableInstant date) throws IOException {
		field(name);
		return value(date);
	}

	public ESJsonContentBuilder field(String name, ReadableInstant date, DateTimeFormatter formatter)
			throws IOException {
		field(name);
		return value(date, formatter);
	}

	public ESJsonContentBuilder field(String name, Date date) throws IOException {
		field(name);
		return value(date);
	}

	public ESJsonContentBuilder field(String name, Date date, DateTimeFormatter formatter) throws IOException {
		field(name);
		return value(date, formatter);
	}

	public ESJsonContentBuilder nullField(String name) throws IOException {
		generator.writeNullField(name);
		return this;
	}

	public ESJsonContentBuilder nullValue() throws IOException {
		generator.writeNull();
		return this;
	}

	public ESJsonContentBuilder value(Boolean value) throws IOException {
		if (value == null) {
			return nullValue();
		}
		return value(value.booleanValue());
	}

	public ESJsonContentBuilder value(boolean value) throws IOException {
		generator.writeBoolean(value);
		return this;
	}

	public ESJsonContentBuilder value(ReadableInstant date) throws IOException {
		return value(date, defaultDatePrinter);
	}

	public ESJsonContentBuilder value(ReadableInstant date, DateTimeFormatter dateTimeFormatter) throws IOException {
		if (date == null) {
			return nullValue();
		}
		return value(dateTimeFormatter.print(date));
	}

	public ESJsonContentBuilder value(Date date) throws IOException {
		return value(date, defaultDatePrinter);
	}

	public ESJsonContentBuilder value(Date date, DateTimeFormatter dateTimeFormatter) throws IOException {
		if (date == null) {
			return nullValue();
		}
		return value(dateTimeFormatter.print(date.getTime()));
	}

	public ESJsonContentBuilder value(Integer value) throws IOException {
		if (value == null) {
			return nullValue();
		}
		return value(value.intValue());
	}

	public ESJsonContentBuilder value(int value) throws IOException {
		generator.writeNumber(value);
		return this;
	}

	public ESJsonContentBuilder value(Long value) throws IOException {
		if (value == null) {
			return nullValue();
		}
		return value(value.longValue());
	}

	public ESJsonContentBuilder value(long value) throws IOException {
		generator.writeNumber(value);
		return this;
	}

	public ESJsonContentBuilder value(Float value) throws IOException {
		if (value == null) {
			return nullValue();
		}
		return value(value.floatValue());
	}

	public ESJsonContentBuilder value(float value) throws IOException {
		generator.writeNumber(value);
		return this;
	}

	public ESJsonContentBuilder value(Double value) throws IOException {
		if (value == null) {
			return nullValue();
		}
		return value(value.doubleValue());
	}

	public ESJsonContentBuilder value(double value) throws IOException {
		generator.writeNumber(value);
		return this;
	}

	public ESJsonContentBuilder value(String value) throws IOException {
		if (value == null) {
			return nullValue();
		}
		generator.writeString(value);
		return this;
	}

	public ESJsonContentBuilder map(Map<String, ?> map) throws IOException {
		if (map == null) {
			return nullValue();
		}
		writeMap(map);
		return this;
	}

	public ESJsonContentBuilder value(Map<String, Object> map) throws IOException {
		if (map == null) {
			return nullValue();
		}
		writeMap(map);
		return this;
	}

	public ESJsonContentBuilder value(Iterable<?> value) throws IOException {
		if (value == null)
			return nullValue();

		startArray();
		for (Object o : value)
			value(o);

		endArray();
		return this;
	}

	public ESJsonContentBuilder latlon(String name, double lat, double lon) throws IOException {
		return startObject(name).field("lat", lat).field("lon", lon).endObject();
	}

	public ESJsonContentBuilder latlon(double lat, double lon) throws IOException {
		return startObject().field("lat", lat).field("lon", lon).endObject();
	}

	public ESJsonContentBuilder flush() throws IOException {
		generator.flush();
		return this;
	}

	// @Override
	public void close() {
		try {
			generator.close();
		} catch (IOException e) {
			// ignore
		}
	}

	/**
	 * Returns a string representation of the builder (only applicable for text
	 * based xcontent).
	 */
	public String string() throws IOException {
		close();
		byte[] byteArray = bos.toByteArray();
		return new String(byteArray, Charsets.UTF_8);
	}

	private void writeMap(Map<String, ?> map) throws IOException {
		generator.writeStartObject();

		for (Map.Entry<String, ?> entry : map.entrySet()) {
			field(entry.getKey());
			Object value = entry.getValue();
			if (value == null) {
				generator.writeNull();
			} else {
				writeValue(value);
			}
		}
		generator.writeEndObject();
	}

	@SuppressWarnings("unchecked")
	private void writeValue(Object value) throws IOException {
		if (value == null) {
			generator.writeNull();
			return;
		}
		Class<?> type = value.getClass();
		if (type == String.class) {
			generator.writeString((String) value);
		} else if (type == Integer.class) {
			generator.writeNumber(((Integer) value).intValue());
		} else if (type == Long.class) {
			generator.writeNumber(((Long) value).longValue());
		} else if (type == Float.class) {
			generator.writeNumber(((Float) value).floatValue());
		} else if (type == Double.class) {
			generator.writeNumber(((Double) value).doubleValue());
		} else if (type == Byte.class) {
			generator.writeNumber(((Byte) value).byteValue());
		} else if (type == Short.class) {
			generator.writeNumber(((Short) value).shortValue());
		} else if (type == Boolean.class) {
			generator.writeBoolean(((Boolean) value).booleanValue());
		} else if (type == ESGeoPoint.class) {
			generator.writeStartObject();
			generator.writeNumberField("lat", ((ESGeoPoint) value).lat());
			generator.writeNumberField("lon", ((ESGeoPoint) value).lon());
			generator.writeEndObject();
		} else if (value instanceof Map) {
			writeMap((Map<String, ?>) value);

			// Path implements Iterable<Path> and causes endless recursion
			// and a StackOverFlow if treated as an Iterable here
			// } else if (value instanceof Path) {
			// generator.writeString(value.toString());

		} else if (value instanceof Iterable) {
			generator.writeStartArray();
			for (Object v : (Iterable<?>) value) {
				writeValue(v);
			}
			generator.writeEndArray();
		} else if (value instanceof Object[]) {
			generator.writeStartArray();
			for (Object v : (Object[]) value) {
				writeValue(v);
			}
			generator.writeEndArray();
			// } else if (type == byte[].class) {
			// generator.writeBinary((byte[]) value);
		} else if (value instanceof Date) {
			generator.writeString(ESJsonContentBuilder.defaultDatePrinter.print(((Date) value).getTime()));
		} else if (value instanceof Calendar) {
			generator
					.writeString(ESJsonContentBuilder.defaultDatePrinter.print((((Calendar) value)).getTimeInMillis()));
		} else if (value instanceof ReadableInstant) {
			generator.writeString(
					ESJsonContentBuilder.defaultDatePrinter.print((((ReadableInstant) value)).getMillis()));
		} else if (value instanceof ESJsonContent) {
			((ESJsonContent) value).toJsonContent(this);
		} else if (value instanceof double[]) {
			generator.writeStartArray();
			for (double v : (double[]) value) {
				generator.writeNumber(v);
			}
			generator.writeEndArray();
		} else if (value instanceof long[]) {
			generator.writeStartArray();
			for (long v : (long[]) value) {
				generator.writeNumber(v);
			}
			generator.writeEndArray();
		} else if (value instanceof int[]) {
			generator.writeStartArray();
			for (int v : (int[]) value) {
				generator.writeNumber(v);
			}
			generator.writeEndArray();
		} else if (value instanceof float[]) {
			generator.writeStartArray();
			for (float v : (float[]) value) {
				generator.writeNumber(v);
			}
			generator.writeEndArray();
		} else if (value instanceof short[]) {
			generator.writeStartArray();
			for (short v : (short[]) value) {
				generator.writeNumber(v);
			}
			generator.writeEndArray();
		} else {
			// if this is a "value" object, like enum, DistanceUnit, ..., just
			// toString it
			// yea, it can be misleading when toString a Java class, but really,
			// jackson should be used in that case
			generator.writeString(value.toString());
			// throw new ElasticsearchIllegalArgumentException("type not
			// supported for generic value conversion: " + type);
		}
	}
}
