package io.spacedog.utils;

public enum SchemaType {

	OBJECT, ARRAY, TEXT, STRING, BOOLEAN, GEOPOINT, INTEGER, FLOAT, LONG, //
	DOUBLE, DECIMAL, DATE, TIME, TIMESTAMP, ENUM, STASH;

	@Override
	public String toString() {
		return name().toLowerCase();
	}

	public boolean equals(String string) {
		return this.toString().equals(string);
	}

	public static SchemaType valueOfNoCase(String value) {
		return valueOf(value.toUpperCase());
	}

	public static boolean isValid(String value) {
		try {
			valueOfNoCase(value);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}