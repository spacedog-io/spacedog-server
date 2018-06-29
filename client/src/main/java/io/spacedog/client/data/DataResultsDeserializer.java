package io.spacedog.client.data;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import io.spacedog.utils.Exceptions;

public class DataResultsDeserializer extends JsonDeserializer<DataResults<Object>> {

	DataWrapDeserializer dataWrapDeserializer = new DataWrapDeserializer();

	@Override
	public DataResults<Object> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		return deserialize(p, ctxt, DataResults.of(Object.class));
	}

	@Override
	public DataResults<Object> deserialize(JsonParser p, DeserializationContext ctxt, DataResults<Object> results)
			throws IOException {

		if (p.isExpectedStartObjectToken()) {
			while (true) {
				JsonToken token = p.nextToken();
				if (token == JsonToken.FIELD_NAME)
					deserializeField(p, results);
				else if (token == JsonToken.END_OBJECT)
					return results;
			}
		}
		throw Exceptions.runtime("unable to deserialize data results: json not an object");

	}

	private void deserializeField(JsonParser p, DataResults<Object> results) throws IOException {
		String fieldName = p.getCurrentName();
		p.nextToken();
		if (fieldName.equals("total"))
			results.total = p.getValueAsLong();
		else if (fieldName.equals("next"))
			results.next = p.getValueAsString();
		else if (fieldName.equals("aggregations"))
			results.aggregations = p.readValueAs(ObjectNode.class);
		else if (fieldName.equals("objects")) {
			if (results.sourceClass != null)
				results.objects = deserializeObjects(p, results.sourceClass);
			else
				// TODO improve
				throw Exceptions.runtime("");
		}
	}

	private List<DataWrap<Object>> deserializeObjects(JsonParser p, Class<Object> sourceClass) throws IOException {
		JsonToken token = p.currentToken();
		List<DataWrap<Object>> objects = Lists.newArrayList();

		if (token.equals(JsonToken.START_ARRAY)) {
			token = p.nextToken();

			while (token != JsonToken.END_ARRAY) {
				DataWrap<Object> wrap = new DataWrap<Object>().sourceClass(sourceClass);
				objects.add(dataWrapDeserializer.deserialize(p, null, wrap));
				token = p.nextToken();
			}
		}

		return objects;
	}
}
