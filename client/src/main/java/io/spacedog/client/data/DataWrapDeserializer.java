package io.spacedog.client.data;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.google.common.collect.Lists;

import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;

public class DataWrapDeserializer extends JsonDeserializer<DataWrap<Object>> {

	@Override
	public DataWrap<Object> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		return deserialize(p, ctxt, new DataWrap<>().source(Json.object()));
	}

	@Override
	public DataWrap<Object> deserialize(JsonParser p, DeserializationContext ctxt, DataWrap<Object> wrap)
			throws IOException {

		if (p.isExpectedStartObjectToken()) {
			while (true) {
				JsonToken token = p.nextToken();
				if (token == JsonToken.FIELD_NAME)
					deserializeField(p, wrap);
				else if (token == JsonToken.END_OBJECT)
					return wrap;
			}
		}
		throw Exceptions.runtime("unable to deserialize data wrap: json not an object");

	}

	private void deserializeField(JsonParser p, DataWrap<Object> wrap) throws IOException {
		String fieldName = p.getCurrentName();
		p.nextToken();
		if (fieldName.equals("id"))
			wrap.id(readString(p));
		else if (fieldName.equals("type"))
			wrap.type(readString(p));
		else if (fieldName.equals("version"))
			wrap.version(readString(p));
		else if (fieldName.equals("score"))
			wrap.score(p.getFloatValue());
		else if (fieldName.equals("sort"))
			wrap.sort(deserializeSort(p, wrap));
		else if (fieldName.equals("source"))
			deserializeSource(p, wrap);
	}

	private void deserializeSource(JsonParser p, DataWrap<Object> wrap) throws IOException {
		if (wrap.source() != null)
			wrap.source(p.readValueAs(wrap.source().getClass()));
		else if (wrap.sourceClass() != null)
			wrap.source(p.readValueAs(wrap.sourceClass()));
		else
			throw Exceptions.illegalArgument(//
					"deserialize wrap failed: no source or source class");
	}

	private Object[] deserializeSort(JsonParser p, DataWrap<Object> wrap) throws IOException {
		JsonToken token = p.currentToken();

		if (token.equals(JsonToken.START_ARRAY)) {
			List<Object> values = Lists.newArrayList();
			token = p.nextToken();

			while (token != JsonToken.END_ARRAY) {
				values.add(readValue(p));
				token = p.nextToken();
			}
			return values.toArray();

		} else {
			Object value = readValue(p);
			return value == null ? null : new Object[] { value };
		}
	}

	private Object readValue(JsonParser p) throws IOException {
		return p.readValueAs(Object.class);
	}

	private String readString(JsonParser p) throws IOException {
		return p.readValueAs(String.class);
	}

}
