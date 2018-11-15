package io.spacedog.client.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import io.spacedog.client.http.SpaceFields;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;

public class DataExportFile<K> implements Iterator<DataWrap<K>>, Iterable<DataWrap<K>> {

	private String json;
	private BufferedReader reader;
	private Class<K> sourceClass;

	public static <K> DataExportFile<K> of(Class<K> sourceClass, Path path) {
		try {
			DataExportFile<K> iterator = new DataExportFile<K>();
			iterator.sourceClass = sourceClass;
			iterator.reader = Files.newBufferedReader(path);
			return iterator;
		} catch (IOException e) {
			throw Exceptions.runtime(e);
		}
	}

	@Override
	public boolean hasNext() {
		try {
			json = reader.readLine();
			return !Strings.isNullOrEmpty(json);
		} catch (IOException e) {
			throw Exceptions.runtime(e);
		}
	}

	@Override
	public DataWrap<K> next() {
		ObjectNode object = Json.readObject(json);
		K source = Json.toPojo(object.get("source"), sourceClass);
		return DataWrap.wrap(source)//
				.id(object.get(SpaceFields.ID_FIELD).asText())//
				.version(object.path(SpaceFields.VERSION_FIELD)//
						.asLong(DataWrap.MATCH_ANY_VERSIONS));
	}

	@Override
	public Iterator<DataWrap<K>> iterator() {
		return this;
	}
}