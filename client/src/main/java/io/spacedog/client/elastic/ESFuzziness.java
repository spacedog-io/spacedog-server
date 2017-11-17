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
package io.spacedog.client.elastic;

import java.io.IOException;

import com.google.common.base.Preconditions;

/**
 * A unit class that encapsulates all in-exact search parsing and conversion
 * from similarities to edit distances etc.
 */
public final class ESFuzziness implements ESJsonContent {

	// public static final XContentBuilderString X_FIELD_NAME = new
	// XContentBuilderString("fuzziness");
	public static final ESFuzziness ZERO = new ESFuzziness(0);
	public static final ESFuzziness ONE = new ESFuzziness(1);
	public static final ESFuzziness TWO = new ESFuzziness(2);
	public static final ESFuzziness AUTO = new ESFuzziness("AUTO");
	// public static final ParseField FIELD = new
	// ParseField(X_FIELD_NAME.camelCase().getValue());

	private final String fuzziness;

	private ESFuzziness(int fuzziness) {
		Preconditions.checkArgument(fuzziness >= 0 && fuzziness <= 2,
				"Valid edit distances are [0, 1, 2] but was [" + fuzziness + "]");
		this.fuzziness = Integer.toString(fuzziness);
	}

	private ESFuzziness(String fuzziness) {
		this.fuzziness = fuzziness;
	}

	/**
	 * Creates a {@link ESFuzziness} instance from an edit distance. The value must
	 * be one of <tt>[0, 1, 2]</tt>
	 */
	public static ESFuzziness fromEdits(int edits) {
		return new ESFuzziness(edits);
	}

	public static ESFuzziness build(Object fuzziness) {
		if (fuzziness instanceof ESFuzziness) {
			return (ESFuzziness) fuzziness;
		}
		String string = fuzziness.toString();
		if (AUTO.asString().equalsIgnoreCase(string)) {
			return AUTO;
		}
		return new ESFuzziness(string);
	}

	@Override
	public ESJsonContentBuilder toJsonContent(ESJsonContentBuilder builder) throws IOException {
		return builder.value(fuzziness);
	}

	public int asDistance() {
		return asDistance(null);
	}

	public int asDistance(String text) {
		if (this == AUTO) { // AUTO
			final int len = termLen(text);
			if (len <= 2) {
				return 0;
			} else if (len > 5) {
				return 2;
			} else {
				return 1;
			}
		}
		return Math.min(2, asInt());
	}

	public ESTimeValue asTimeValue() {
		if (this == AUTO) {
			return ESTimeValue.timeValueMillis(1);
		} else {
			return ESTimeValue.parseTimeValue(fuzziness.toString(), null);
		}
	}

	public long asLong() {
		if (this == AUTO) {
			return 1;
		}
		try {
			return Long.parseLong(fuzziness.toString());
		} catch (NumberFormatException ex) {
			return (long) Double.parseDouble(fuzziness.toString());
		}
	}

	public int asInt() {
		if (this == AUTO) {
			return 1;
		}
		try {
			return Integer.parseInt(fuzziness.toString());
		} catch (NumberFormatException ex) {
			return (int) Float.parseFloat(fuzziness.toString());
		}
	}

	public short asShort() {
		if (this == AUTO) {
			return 1;
		}
		try {
			return Short.parseShort(fuzziness.toString());
		} catch (NumberFormatException ex) {
			return (short) Float.parseFloat(fuzziness.toString());
		}
	}

	public byte asByte() {
		if (this == AUTO) {
			return 1;
		}
		try {
			return Byte.parseByte(fuzziness.toString());
		} catch (NumberFormatException ex) {
			return (byte) Float.parseFloat(fuzziness.toString());
		}
	}

	public double asDouble() {
		if (this == AUTO) {
			return 1d;
		}
		return Double.parseDouble(fuzziness.toString());
	}

	public float asFloat() {
		if (this == AUTO) {
			return 1f;
		}
		return Float.parseFloat(fuzziness.toString());
	}

	private int termLen(String text) {
		return text == null ? 5 : text.codePointCount(0, text.length()); // 5 avg term length in english
	}

	public String asString() {
		return fuzziness.toString();
	}
}
