package io.spacedog.services.data;

import java.io.IOException;

import org.elasticsearch.common.Strings;
import org.elasticsearch.search.aggregations.Aggregation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import io.spacedog.utils.Json;

@SuppressWarnings("serial")
public class AggregationSerializer extends StdSerializer<Aggregation> {

	public AggregationSerializer() {
		super(Aggregation.class);
	}

	@Override
	public void serialize(Aggregation aggregation, JsonGenerator gen, SerializerProvider serializers)
			throws IOException, JsonProcessingException {
		//
		// TODO
		// Find a better way to serialize aggregations
		// gen.writeRaw(aggregation.toString) doesn't fit since this serializer
		// is also used to convert to a JsonTree.
		// We should try to integrate both elastic and jackson denerators.
		//
		String elasticJson = Strings.toString(aggregation);
		JsonNode node = Json.readObject(elasticJson).fields().next().getValue();		
		gen.writeTree(node);
	}

}
