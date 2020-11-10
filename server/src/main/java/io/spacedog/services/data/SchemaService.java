package io.spacedog.services.data;

import java.util.Arrays;
import java.util.Map;

import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.Settings;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;

import io.spacedog.client.schema.Schema;
import io.spacedog.services.Services;
import io.spacedog.services.SpaceService;
import io.spacedog.services.elastic.ElasticIndex;
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
		elastic().deleteIndices(Services.data().index(name));
	}

	public void deleteAll() {
		Arrays.stream(Services.data().indices())//
				.forEach(index -> elastic().deleteIndices(index));
	}

	public void set(Schema schema) {

		schema.enhance();

		ElasticIndex index = Services.data().index(schema.name());
		boolean indexExists = elastic().exists(index);

		if (indexExists) {
			// TODO
			// lots of settings can't be updated
			// we need to implement a function to trim not updatable settings
			// elastic().putSettings(index, schema.settings(true));
			elastic().putMapping(index, schema.mapping());
		} else
			elastic().createIndex(index, schema, false);
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

	private Map<String, Schema> get(ElasticIndex... indices) {

		Map<String, Schema> schemas = Maps.newHashMap();

		if (Utils.isNullOrEmpty(indices))
			return schemas;

		Map<String, MappingMetadata> mappingMap = elastic().getMappings(indices).mappings();

		ImmutableOpenMap<String, Settings> settingsMap = //
				elastic().getSettings(indices).getIndexToSettings();

		for (ElasticIndex index : indices) {
			MappingMetadata mapping = mappingMap.get(index.toString());
			Settings settings = settingsMap.get(index.toString());
			JsonNode node = Json.readObject(mapping.source().toString()).get(mapping.type());
			schemas.put(mapping.type(), new Schema(index.type(), Json.checkObject(node), //
					Json.readObject(settings.toString())));
		}

		return schemas;
	}

}
