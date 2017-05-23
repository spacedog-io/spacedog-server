/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.spacedog.sdk.elastic;

/**
 * The DistanceUnit enumerates several units for measuring distances. These
 * units provide methods for converting strings and methods to convert units
 * among each others. Some methods like
 * {@link ESDistanceUnit#getEarthCircumference} refer to the earth ellipsoid
 * defined in {@link GeoUtils}. The default unit used within this project is
 * <code>METERS</code> which is defined by <code>DEFAULT</code>
 */
public enum ESDistanceUnit {
	INCH(0.0254, "in", "inch"), //
	YARD(0.9144, "yd", "yards"), //
	FEET(0.3048, "ft", "feet"), //
	KILOMETERS(1000.0, "km", "kilometers"), //
	NAUTICALMILES(1852.0, "NM", "nmi", "nauticalmiles"), //
	MILLIMETERS(0.001, "mm", "millimeters"), //
	CENTIMETERS(0.01, "cm", "centimeters"), //
	// 'm' is a suffix of 'nmi' so it must follow 'nmi'
	MILES(1609.344, "mi", "miles"), //
	// since 'm' is suffix of other unit
	// it must be the last entry of unit
	// names ending with 'm'. otherwise
	// parsing would fail
	METERS(1, "m", "meters");

	public static final ESDistanceUnit DEFAULT = METERS;

	private double meters;
	private final String[] names;

	ESDistanceUnit(double meters, String... names) {
		this.meters = meters;
		this.names = names;
	}

	/**
	 * Convert a value to a distance string
	 * 
	 * @param distance
	 *            value to convert
	 * @return String representation of the distance
	 */
	public String toString(double distance) {
		return distance + toString();
	}

	@Override
	public String toString() {
		return names[0];
	}

	/**
	 * Convert a String to a {@link ESDistanceUnit}
	 * 
	 * @param unit
	 *            name of the unit
	 * @return unit matching the given name
	 * @throws IllegalArgumentException
	 *             if no unit matches the given name
	 */
	public static ESDistanceUnit fromString(String unit) {
		for (ESDistanceUnit dunit : values()) {
			for (String name : dunit.names) {
				if (name.equals(unit)) {
					return dunit;
				}
			}
		}
		throw new IllegalArgumentException("No distance unit match [" + unit + "]");
	}

	public double meters() {
		return meters;
	}
}
