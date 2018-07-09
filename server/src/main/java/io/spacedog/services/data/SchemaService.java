package io.spacedog.services.data;

import java.util.Arrays;
import java.util.Map;

import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;

import com.carrotsearch.hppc.cursors.ObjectCursor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;

import io.spacedog.client.schema.Schema;
import io.spacedog.server.Index;
import io.spacedog.server.Services;
import io.spacedog.server.SpaceService;
import io.spacedog.services.push.PushService;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.Utils;

public class SchemaService extends SpaceService {

	public void reset(Schema schema) {
		delete(schema.name());
		set(schema);
	}

	public void delete(String name) {
		elastic().deleteIndex(Services.data().index(name));
	}

	public void deleteAll() {
		Arrays.stream(Services.data().indices())//
				.forEach(index -> elastic().deleteIndex(index));
	}

	public void set(Schema schema) {

		ObjectNode mapping = schema.enhance().mapping();

		Index index = Services.data().index(schema.name());
		boolean indexExists = elastic().exists(index);

		if (indexExists)
			elastic().putMapping(index, mapping.toString());
		else
			elastic().createIndex(index, mapping.toString(), false);
	}

	public Schema get(String name) {
		Schema.checkName(name);
		return get(Services.data().index(name)).get(name);
	}

	public void setDefault(String name) {
		if (PushService.DATA_TYPE.equals(name))
			set(Services.push().getDefaultInstallationSchema());
		else
			throw Exceptions.illegalArgument(//
					"no default schema for type [%s]", name);
	}

	public Map<String, Schema> getAll() {
		return get(Services.data().indices());
	}

	//
	// Implementation
	//

	private Map<String, Schema> get(Index... indices) {

		Map<String, Schema> schemas = Maps.newHashMap();

		if (Utils.isNullOrEmpty(indices))
			return schemas;

		GetMappingsResponse response = elastic().getMappings(indices);

		for (ObjectCursor<ImmutableOpenMap<String, MappingMetaData>> indexMappings //
		: response.mappings().values()) {

			MappingMetaData mapping = indexMappings.value.iterator().next().value;
			schemas.put(mapping.type(), new Schema(mapping.type(), //
					Json.readObject(mapping.source().toString())));
		}

		return schemas;
	}

}
