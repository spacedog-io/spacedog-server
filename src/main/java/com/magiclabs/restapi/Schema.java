package com.magiclabs.restapi;

import java.util.Optional;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.magiclabs.restapi.SchemaValidator.InvalidSchemaException;

public class Schema {

	@SuppressWarnings("serial")
	public static class InvalidDataObjectException extends RuntimeException {

		public InvalidDataObjectException(String message) {
			super(message);
		}

		public InvalidDataObjectException(String message, Object... args) {
			super(String.format(message, args));
		}

	}

	public static enum PropertyTypes {
		OBJECT, ARRAY, TEXT, STRING, BOOLEAN, GEOPOINT, NUMBER, //
		DATE, TIME, TIMESTAMP, ENUM, AMOUNT, STASH, REF, FILE;

		public String toString() {
			return super.toString().toLowerCase();
		}
	}

	private String _id;
	private Property[] properties;

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

		public void checkValue(JsonValue value) {
			if (Json.isNull(value)) {
				if (isRequired())
					throw new InvalidDataObjectException(
							"property [%s] is null but required", getName());
			}
		}

		protected InvalidDataObjectException invalidType(String name,
				JsonValue value) {
			// TODO Auto-generated method stub
			return null;
		}

	}

	public class StringProperty extends Property {
		private String regex;
		private int _min = 0;
		private int _max = 1000;

		public void check() {
			super.check();
			checkMinMax(_min, _max);
			// TODO check regex is well formed
		}

		public void checkValue(JsonValue value) {

			super.checkValue(value);

			if (value.isString()) {
				int length = value.asString().length();
				if (_min > length || _max < length)
					throw new InvalidDataObjectException(
							"property [%s] has a string value of length [%s], should be between [%s] and [%s]",
							getName(), length, _min, _max);
				if (regex != null) {
					// TODO
				}

			} else
				throw invalidType(getName(), value);

		}

	}

	public class TextProperty extends Property {
		private String language = "english"; // TODO find the real java type
		private int _min = 0;
		private int _max = 10000;

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

		public void check() {
			super.check();
			// TODO check gt is lesser than lt
		}
	}

	public class FloatProperty extends Property {
		private Float _gt = null;
		private boolean _gte = false;
		private Float _lt = null;
		private boolean _lte = false;
		private float _values;

		public void check() {
			super.check();
			// TODO check gt is lesser than lt
		}
	}

	public class ObjectProperty extends Property {
		private Property[] properties = null;

		public void check() {
			super.check();
			for (Property property : properties) {
				property.check();
			}
		}
	}

	public class DateProperty extends Property {
	}

	public class TimeProperty extends Property {
	}

	public class TimestampProperty extends Property {
	}

	public class EnumProperty extends Property {
		private String[] _values;
	}

	public class GeopointProperty extends Property {
	}

	public class StashProperty extends Property {
	}

	public class AmountProperty extends Property {
		private String[] currencies;
	}

	public class ReferenceProperty extends Property {
		private String _refType; // TODO or _refSchemaId
		// TODO revoir le nom : propriétés à recopier ici pour permettre les
		// recherches jointes
		private String[] _searchProperties;
	}

	public class FileProperty extends Property {
		private String[] _contentTypes;
	}

	public void check() {
		checkNotNullOrEmpty(_id, "schema _id is null or empty");
		for (Property property : properties) {
			property.check();
		}
	}

	public void checkObject(JsonObject object) {
		object.forEach(member -> {
			property(member.getName()).orElseThrow(
					() -> new InvalidDataObjectException("")).checkValue(
					member.getValue());
		});
	}

	private Optional<Property> property(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	private void checkNotNullOrEmpty(String _id2, String string) {
		// TODO Auto-generated method stub

	}

	private void checkMinMax(int _min, int _max) {
		if (_min > _max)
			throw new InvalidSchemaException(String.format(
					"_min [%s] should not be greater than _max [%s]", _min,
					_max));
	}

	public static Schema from(JsonValue json) {

		return null;
	}
}
