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

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

import io.spacedog.utils.Exceptions;

/**
 *
 */
public class ESJsonContentGenerator {// implements XContentGenerator {

	/** Generator used to write content **/
	protected final JsonGenerator generator;

	/**
	 * Reference to base generator because writing raw values needs a specific
	 * method call.
	 */
	// private final GeneratorBase base;

	/**
	 * Reference to filtering generator because writing an empty object '{}'
	 * when everything is filtered out needs a specific treatment
	 */
	// private final FilteringGeneratorDelegate filter;

	private final ByteArrayOutputStream os;

	private boolean writeLineFeedAtEnd;
	private static final SerializedString LF = new SerializedString("\n");
	private static final DefaultPrettyPrinter.Indenter INDENTER = new DefaultIndenter("  ", LF.getValue());
	// private boolean prettyPrint = false;

	private final static JsonFactory jsonFactory;

	static {
		jsonFactory = new JsonFactory();
		jsonFactory.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
		jsonFactory.configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, true);
		jsonFactory.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
		jsonFactory.configure(JsonFactory.Feature.FAIL_ON_SYMBOL_HASH_OVERFLOW, false); // this
	}

	public ESJsonContentGenerator(ByteArrayOutputStream bos) {
		this.os = bos;
		try {
			this.generator = jsonFactory.createGenerator(os, JsonEncoding.UTF8);
		} catch (IOException e) {
			throw Exceptions.runtime(e);
		}
	}

	public final void usePrettyPrint() {
		generator.setPrettyPrinter(new DefaultPrettyPrinter().withObjectIndenter(INDENTER));
		// prettyPrint = true;
	}

	public void usePrintLineFeedAtEnd() {
		writeLineFeedAtEnd = true;
	}

	public void writeStartArray() throws IOException {
		generator.writeStartArray();
	}

	public void writeEndArray() throws IOException {
		generator.writeEndArray();
	}

	public void writeStartObject() throws IOException {
		// if (isFiltered() && inRoot()) {
		// // Bypass generator to always write the root start object
		// filter.getDelegate().writeStartObject();
		// return;
		// }
		generator.writeStartObject();
	}

	public void writeEndObject() throws IOException {
		// if (isFiltered() && inRoot()) {
		// // Bypass generator to always write the root end object
		// filter.getDelegate().writeEndObject();
		// return;
		// }
		generator.writeEndObject();
	}

	public void writeFieldName(String name) throws IOException {
		generator.writeFieldName(name);
	}

	public void writeString(String text) throws IOException {
		generator.writeString(text);
	}

	public void writeString(char[] text, int offset, int len) throws IOException {
		generator.writeString(text, offset, len);
	}

	public void writeUTF8String(byte[] text, int offset, int length) throws IOException {
		generator.writeUTF8String(text, offset, length);
	}

	public void writeNumber(int v) throws IOException {
		generator.writeNumber(v);
	}

	public void writeNumber(long v) throws IOException {
		generator.writeNumber(v);
	}

	public void writeNumber(double d) throws IOException {
		generator.writeNumber(d);
	}

	public void writeNumber(float f) throws IOException {
		generator.writeNumber(f);
	}

	public void writeBoolean(boolean state) throws IOException {
		generator.writeBoolean(state);
	}

	public void writeNull() throws IOException {
		generator.writeNull();
	}

	public void writeStringField(String fieldName, String value) throws IOException {
		generator.writeStringField(fieldName, value);
	}

	public void writeBooleanField(String fieldName, boolean value) throws IOException {
		generator.writeBooleanField(fieldName, value);
	}

	public void writeNullField(String fieldName) throws IOException {
		generator.writeNullField(fieldName);
	}

	public void writeNumberField(String fieldName, int value) throws IOException {
		generator.writeNumberField(fieldName, value);
	}

	public void writeNumberField(String fieldName, long value) throws IOException {
		generator.writeNumberField(fieldName, value);
	}

	public void writeNumberField(String fieldName, double value) throws IOException {
		generator.writeNumberField(fieldName, value);
	}

	public void writeNumberField(String fieldName, float value) throws IOException {
		generator.writeNumberField(fieldName, value);
	}

	public void writeBinaryField(String fieldName, byte[] data) throws IOException {
		generator.writeBinaryField(fieldName, data);
	}

	public void writeArrayFieldStart(String fieldName) throws IOException {
		generator.writeArrayFieldStart(fieldName);
	}

	public void writeObjectFieldStart(String fieldName) throws IOException {
		generator.writeObjectFieldStart(fieldName);
	}

	public void flush() throws IOException {
		generator.flush();
	}

	public void close() throws IOException {
		if (generator.isClosed()) {
			return;
		}
		if (writeLineFeedAtEnd) {
			flush();
			generator.writeRaw(LF);
		}
		generator.close();
	}

}
