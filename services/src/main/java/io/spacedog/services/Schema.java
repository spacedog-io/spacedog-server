/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.services.SchemaValidator.SchemaException;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;

public class Schema {

	private String name;
	private ObjectNode node;
	private Property[] properties;

	public Schema(String name, ObjectNode node) {
		this.name = name;
		this.node = node;
	}

	public ObjectNode rootNode() {
		return node;
	}

	public ObjectNode contentNode() {
		return (ObjectNode) node.get(name);
	}

	public boolean hasIdPath() {
		return contentNode().has("_id");
	}

	public String idPath() {
		return contentNode().get("_id").asText();
	}

	//
	// draft implementation for future use
	//

	public void check() {
		// checkNotNullOrEmpty(_id, "schema _id is null or empty");
		for (Property property : properties) {
			property.check();
		}
	}

	public void checkObject(ObjectNode object) {
		object.fields().forEachRemaining(member -> {
			property(member.getKey()).orElseThrow(() -> Exceptions.illegalArgument(""))//
					.checkValue(member.getValue());
		});
	}

	private Optional<Property> property(String name) {
		return null;
	}

	private void checkNotNullOrEmpty(String _id2, String string) {
	}

	private void checkMinMax(int _min, int _max) {
		if (_min > _max)
			throw new SchemaException("_min [%s] should not be greater than _max [%s]", _min, _max);
	}

	public abstract class Property {
		private String name;
		private boolean _required = false;

		public String getName() {
			return name;
		}

		public boolean isRequired() {
			return _required;
		}

		public void check() {
			checkNotNullOrEmpty(name, "property name is null or empty");
		}

		public void checkValue(JsonNode value) {
			if (Json.isNull(value)) {
				if (isRequired())
					throw Exceptions.illegalArgument("property [%s] is null but required", getName());
			}
		}
	}

	public class StringProperty extends Property {
		private String regex;
		private int _min = 0;
		private int _max = 1000;

		@Override
		public void check() {
			super.check();
			checkMinMax(_min, _max);
			// TODO check regex is well formed
		}

		@Override
		public void checkValue(JsonNode value) {
			super.checkValue(value);
			if (value.isTextual()) {
				int length = value.asText().length();
				if (_min > length || _max < length)
					throw Exceptions.illegalArgument(
							"property [%s] has a string value of length [%s], should be between [%s] and [%s]",
							getName(), length, _min, _max);
				if (regex != null) {
					// TODO
				}
			} else
				throw Exceptions.illegalArgument("", getName(), value);
		}
	}

	public class TextProperty extends Property {
		private String language = "english"; // TODO find the real java type
		private int _min = 0;
		private int _max = 10000;

		@Override
		public void check() {
			super.check();
			checkMinMax(_min, _max);
			// TODO check language is ISO
		}
	}

	public class BooleanProperty extends Property {
	}

	public class IntegerProperty extends Property {
		private Integer _gt = null;
		private boolean _gte = false;
		private Integer _lt = null;
		private boolean _lte = false;
		private int _values;

		@Override
		public void check() {
			super.check();
			// TODO check gt is lesser than lt
		}
	}

	public class ObjectProperty extends Property {
		private Property[] properties = null;

		@Override
		public void check() {
			super.check();
			for (Property property : properties) {
				property.check();
			}
		}
	}
}
