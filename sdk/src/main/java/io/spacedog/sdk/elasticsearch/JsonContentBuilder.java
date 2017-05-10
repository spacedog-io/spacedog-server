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

package io.spacedog.sdk.elasticsearch;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.joda.time.DateTimeZone;
import org.joda.time.ReadableInstant;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.google.common.base.Charsets;

/**
 *
 */
public final class JsonContentBuilder {// implements , Releasable {

	public final static DateTimeFormatter defaultDatePrinter = ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC);

	private JsonContentGenerator generator;

	private final ByteArrayOutputStream bos;

	private boolean humanReadable = false;

	/**
	 * Constructs a new builder using the provided xcontent and an OutputStream.
	 * Make sure to call {@link #close()} when the builder is done with.
	 */
	public JsonContentBuilder() throws IOException {
		this.bos = new ByteArrayOutputStream();
		this.generator = new JsonContentGenerator(bos);
	}

	public JsonContentBuilder prettyPrint() {
		generator.usePrettyPrint();
		return this;
	}

	public JsonContentBuilder lfAtEnd() {
		generator.usePrintLineFeedAtEnd();
		return this;
	}

	public JsonContentBuilder humanReadable(boolean humanReadable) {
		this.humanReadable = humanReadable;
		return this;
	}

	public boolean humanReadable() {
		return this.humanReadable;
	}

	public JsonContentBuilder startObject(String name) throws IOException {
		field(name);
		startObject();
		return this;
	}

	// public XContentBuilder startObject(String name, FieldCaseConversion
	// conversion) throws IOException {
	// field(name, conversion);
	// startObject();
	// return this;
	// }

	public JsonContentBuilder startObject() throws IOException {
		generator.writeStartObject();
		return this;
	}

	public JsonContentBuilder endObject() throws IOException {
		generator.writeEndObject();
		return this;
	}

	public JsonContentBuilder array(String name, String... values) throws IOException {
		startArray(name);
		for (String value : values) {
			value(value);
		}
		endArray();
		return this;
	}

	public JsonContentBuilder array(String name, Object... values) throws IOException {
		startArray(name);
		for (Object value : values) {
			value(value);
		}
		endArray();
		return this;
	}

	// public XContentBuilder startArray(String name, FieldCaseConversion
	// conversion) throws IOException {
	// field(name, conversion);
	// startArray();
	// return this;
	// }

	public JsonContentBuilder startArray(String name) throws IOException {
		field(name);
		startArray();
		return this;
	}

	public JsonContentBuilder startArray() throws IOException {
		generator.writeStartArray();
		return this;
	}

	public JsonContentBuilder endArray() throws IOException {
		generator.writeEndArray();
		return this;
	}

	public JsonContentBuilder field(String name) throws IOException {
		if (name == null) {
			throw new IllegalArgumentException("field name cannot be null");
		}
		generator.writeFieldName(name);
		return this;
	}

	// public XContentBuilder field(String name, char[] value, int offset, int
	// length) throws IOException {
	// field(name);
	// if (value == null) {
	// generator.writeNull();
	// } else {
	// generator.writeString(value, offset, length);
	// }
	// return this;
	// }

	public JsonContentBuilder value(Object value) throws IOException {
		writeValue(value);
		return this;
	}

	public JsonContentBuilder field(String name, String value) throws IOException {
		field(name);
		if (value == null) {
			generator.writeNull();
		} else {
			generator.writeString(value);
		}
		return this;
	}
	//
	// public XContentBuilder field(String name, String value,
	// FieldCaseConversion conversion) throws IOException {
	// field(name, conversion);
	// if (value == null) {
	// generator.writeNull();
	// } else {
	// generator.writeString(value);
	// }
	// return this;
	// }

	public JsonContentBuilder field(String name, Integer value) throws IOException {
		field(name);
		if (value == null) {
			generator.writeNull();
		} else {
			generator.writeNumber(value.intValue());
		}
		return this;
	}

	public JsonContentBuilder field(String name, int value) throws IOException {
		field(name);
		generator.writeNumber(value);
		return this;
	}

	public JsonContentBuilder field(String name, Long value) throws IOException {
		field(name);
		if (value == null) {
			generator.writeNull();
		} else {
			generator.writeNumber(value.longValue());
		}
		return this;
	}

	public JsonContentBuilder field(String name, long value) throws IOException {
		field(name);
		generator.writeNumber(value);
		return this;
	}

	public JsonContentBuilder field(String name, Float value) throws IOException {
		field(name);
		if (value == null) {
			generator.writeNull();
		} else {
			generator.writeNumber(value.floatValue());
		}
		return this;
	}

	public JsonContentBuilder field(String name, float value) throws IOException {
		field(name);
		generator.writeNumber(value);
		return this;
	}

	public JsonContentBuilder field(String name, Double value) throws IOException {
		field(name);
		if (value == null) {
			generator.writeNull();
		} else {
			generator.writeNumber(value);
		}
		return this;
	}

	public JsonContentBuilder field(String name, double value) throws IOException {
		field(name);
		generator.writeNumber(value);
		return this;
	}

	public JsonContentBuilder field(String name, BigDecimal value) throws IOException {
		return field(name, value, value.scale(), RoundingMode.HALF_UP, true);
	}

	public JsonContentBuilder field(String name, BigDecimal value, int scale, RoundingMode rounding, boolean toDouble)
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
	//
	// public XContentBuilder field(String name, byte[] value, int offset, int
	// length) throws IOException {
	// field(name);
	// generator.writeBinary(value, offset, length);
	// return this;
	// }

	public JsonContentBuilder field(String name, Map<String, Object> value) throws IOException {
		field(name);
		value(value);
		return this;
	}

	public JsonContentBuilder field(String name, Iterable<?> value) throws IOException {
		if (value instanceof Path) {
			// treat Paths as single value
			field(name);
			value(value);
		} else {
			startArray(name);
			for (Object o : value) {
				value(o);
			}
			endArray();
		}
		return this;
	}

	public JsonContentBuilder field(String name, boolean... value) throws IOException {
		startArray(name);
		for (boolean o : value) {
			value(o);
		}
		endArray();
		return this;
	}

	public JsonContentBuilder field(String name, String... value) throws IOException {
		startArray(name);
		for (String o : value) {
			value(o);
		}
		endArray();
		return this;
	}

	public JsonContentBuilder field(String name, Object... value) throws IOException {
		startArray(name);
		for (Object o : value) {
			value(o);
		}
		endArray();
		return this;
	}

	public JsonContentBuilder field(String name, int... value) throws IOException {
		startArray(name);
		for (Object o : value) {
			value(o);
		}
		endArray();
		return this;
	}

	public JsonContentBuilder field(String name, long... value) throws IOException {
		startArray(name);
		for (Object o : value) {
			value(o);
		}
		endArray();
		return this;
	}

	public JsonContentBuilder field(String name, float... value) throws IOException {
		startArray(name);
		for (Object o : value) {
			value(o);
		}
		endArray();
		return this;
	}

	public JsonContentBuilder field(String name, double... value) throws IOException {
		startArray(name);
		for (Object o : value) {
			value(o);
		}
		endArray();
		return this;
	}

	// public XContentBuilder field(String name, Object value) throws
	// IOException {
	// field(name);
	// writeValue(value);
	// return this;
	// }

	// public XContentBuilder value(Object value) throws IOException {
	// writeValue(value);
	// return this;
	// }

	public JsonContentBuilder field(String name, boolean value) throws IOException {
		field(name);
		generator.writeBoolean(value);
		return this;
	}
	//
	// public XContentBuilder field(String name, byte[] value) throws
	// IOException {
	// field(name);
	// if (value == null) {
	// generator.writeNull();
	// } else {
	// generator.writeBinary(value);
	// }
	// return this;
	// }

	public JsonContentBuilder field(String name, ReadableInstant date) throws IOException {
		field(name);
		return value(date);
	}

	public JsonContentBuilder field(String name, ReadableInstant date, DateTimeFormatter formatter) throws IOException {
		field(name);
		return value(date, formatter);
	}

	public JsonContentBuilder field(String name, Date date) throws IOException {
		field(name);
		return value(date);
	}

	public JsonContentBuilder field(String name, Date date, DateTimeFormatter formatter) throws IOException {
		field(name);
		return value(date, formatter);
	}

	public JsonContentBuilder nullField(String name) throws IOException {
		generator.writeNullField(name);
		return this;
	}

	public JsonContentBuilder nullValue() throws IOException {
		generator.writeNull();
		return this;
	}
	//
	// public XContentBuilder rawField(String fieldName, InputStream content)
	// throws IOException {
	// generator.writeRawField(fieldName, content);
	// return this;
	// }

	public JsonContentBuilder value(Boolean value) throws IOException {
		if (value == null) {
			return nullValue();
		}
		return value(value.booleanValue());
	}

	public JsonContentBuilder value(boolean value) throws IOException {
		generator.writeBoolean(value);
		return this;
	}

	public JsonContentBuilder value(ReadableInstant date) throws IOException {
		return value(date, defaultDatePrinter);
	}

	public JsonContentBuilder value(ReadableInstant date, DateTimeFormatter dateTimeFormatter) throws IOException {
		if (date == null) {
			return nullValue();
		}
		return value(dateTimeFormatter.print(date));
	}

	public JsonContentBuilder value(Date date) throws IOException {
		return value(date, defaultDatePrinter);
	}

	public JsonContentBuilder value(Date date, DateTimeFormatter dateTimeFormatter) throws IOException {
		if (date == null) {
			return nullValue();
		}
		return value(dateTimeFormatter.print(date.getTime()));
	}

	public JsonContentBuilder value(Integer value) throws IOException {
		if (value == null) {
			return nullValue();
		}
		return value(value.intValue());
	}

	public JsonContentBuilder value(int value) throws IOException {
		generator.writeNumber(value);
		return this;
	}

	public JsonContentBuilder value(Long value) throws IOException {
		if (value == null) {
			return nullValue();
		}
		return value(value.longValue());
	}

	public JsonContentBuilder value(long value) throws IOException {
		generator.writeNumber(value);
		return this;
	}

	public JsonContentBuilder value(Float value) throws IOException {
		if (value == null) {
			return nullValue();
		}
		return value(value.floatValue());
	}

	public JsonContentBuilder value(float value) throws IOException {
		generator.writeNumber(value);
		return this;
	}

	public JsonContentBuilder value(Double value) throws IOException {
		if (value == null) {
			return nullValue();
		}
		return value(value.doubleValue());
	}

	public JsonContentBuilder value(double value) throws IOException {
		generator.writeNumber(value);
		return this;
	}

	public JsonContentBuilder value(String value) throws IOException {
		if (value == null) {
			return nullValue();
		}
		generator.writeString(value);
		return this;
	}
	//
	// public XContentBuilder value(byte[] value) throws IOException {
	// if (value == null) {
	// return nullValue();
	// }
	// generator.writeBinary(value);
	// return this;
	// }
	//
	// public XContentBuilder value(byte[] value, int offset, int length) throws
	// IOException {
	// if (value == null) {
	// return nullValue();
	// }
	// generator.writeBinary(value, offset, length);
	// return this;
	// }

	public JsonContentBuilder map(Map<String, ?> map) throws IOException {
		if (map == null) {
			return nullValue();
		}
		writeMap(map);
		return this;
	}

	public JsonContentBuilder value(Map<String, Object> map) throws IOException {
		if (map == null) {
			return nullValue();
		}
		writeMap(map);
		return this;
	}

	public JsonContentBuilder value(Iterable<?> value) throws IOException {
		if (value == null) {
			return nullValue();
		}
		if (value instanceof Path) {
			// treat as single value
			writeValue(value);
		} else {
			startArray();
			for (Object o : value) {
				value(o);
			}
			endArray();
		}
		return this;
	}

	public JsonContentBuilder latlon(String name, double lat, double lon) throws IOException {
		return startObject(name).field("lat", lat).field("lon", lon).endObject();
	}

	public JsonContentBuilder latlon(double lat, double lon) throws IOException {
		return startObject().field("lat", lat).field("lon", lon).endObject();
	}

	public JsonContentBuilder flush() throws IOException {
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
		} else if (type == GeoPoint.class) {
			generator.writeStartObject();
			generator.writeNumberField("lat", ((GeoPoint) value).lat());
			generator.writeNumberField("lon", ((GeoPoint) value).lon());
			generator.writeEndObject();
		} else if (value instanceof Map) {
			writeMap((Map) value);
		} else if (value instanceof Path) {
			// Path implements Iterable<Path> and causes endless recursion and a
			// StackOverFlow if treated as an Iterable here
			generator.writeString(value.toString());
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
			generator.writeString(JsonContentBuilder.defaultDatePrinter.print(((Date) value).getTime()));
		} else if (value instanceof Calendar) {
			generator.writeString(JsonContentBuilder.defaultDatePrinter.print((((Calendar) value)).getTimeInMillis()));
		} else if (value instanceof ReadableInstant) {
			generator.writeString(JsonContentBuilder.defaultDatePrinter.print((((ReadableInstant) value)).getMillis()));
		} else if (value instanceof JsonContent) {
			((JsonContent) value).toJsonContent(this);
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
