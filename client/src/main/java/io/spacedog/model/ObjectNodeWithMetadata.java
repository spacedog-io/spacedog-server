package io.spacedog.model;

import java.io.IOException;
import java.util.Iterator;

import org.joda.time.DateTime;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.utils.Json;

public class ObjectNodeWithMetadata extends ObjectNode implements Metadata {

	public class MetaNode extends Meta {

		public String createdBy() {
			return Json.toPojo(ObjectNodeWithMetadata.this, "meta.createdBy", String.class);
		}

		public Meta createdBy(String createdBy) {
			Json.with(ObjectNodeWithMetadata.this, "meta.createdBy", createdBy);
			return this;
		}

		public DateTime createdAt() {
			return Json.toPojo(ObjectNodeWithMetadata.this, "meta.createdAt", DateTime.class);
		}

		public Meta createdAt(DateTime createdAt) {
			Json.with(ObjectNodeWithMetadata.this, "meta.createdAt", createdAt);
			return this;
		}

		public String updatedBy() {
			return Json.toPojo(ObjectNodeWithMetadata.this, "meta.updatedBy", String.class);
		}

		public Meta updatedBy(String updatedBy) {
			Json.with(ObjectNodeWithMetadata.this, "meta.updatedBy", updatedBy);
			return this;
		}

		public DateTime updatedAt() {
			return Json.toPojo(ObjectNodeWithMetadata.this, "meta.updatedAt", DateTime.class);
		}

		public Meta updatedAt(DateTime updatedAt) {
			Json.with(ObjectNodeWithMetadata.this, "meta.updatedAt", updatedAt);
			return this;
		}

	}

	public ObjectNodeWithMetadata() {
		super(JsonNodeFactory.instance);
	}

	public Meta meta() {
		return new MetaNode();
	}

	public void meta(Meta meta) {
		set("meta", Json.toNode(meta));
	}

	public static class ObjectNodeWithMetadataDeserializer //
			extends StdDeserializer<ObjectNodeWithMetadata> {

		private static final long serialVersionUID = 1L;

		public ObjectNodeWithMetadataDeserializer() {
			super(ObjectNodeWithMetadata.class);
		}

		@Override
		public ObjectNodeWithMetadata deserialize(JsonParser p, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {
			ObjectNode node = p.readValueAs(ObjectNode.class);
			ObjectNodeWithMetadata source = new ObjectNodeWithMetadata();
			Iterator<String> names = node.fieldNames();
			while (names.hasNext()) {
				String name = (String) names.next();
				source.set(name, node.get(name));
			}
			return source;
		}
	}
}