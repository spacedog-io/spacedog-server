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

/**
 * A Query that matches documents within an range of terms.
 */
public class ESRangeQueryBuilder extends ESMultiTermQueryBuilder implements ESBoostableQueryBuilder<ESRangeQueryBuilder> {

	private final String name;
	private Object from;
	private Object to;
	private String timeZone;
	private boolean includeLower = true;
	private boolean includeUpper = true;
	private float boost = -1;
	private String queryName;
	private String format;

	/**
	 * A Query that matches documents within an range of terms.
	 *
	 * @param name
	 *            The field name
	 */
	public ESRangeQueryBuilder(String name) {
		this.name = name;
	}

	/**
	 * The from part of the range query. Null indicates unbounded.
	 */
	public ESRangeQueryBuilder from(Object from) {
		this.from = from;
		return this;
	}

	/**
	 * The from part of the range query. Null indicates unbounded.
	 */
	public ESRangeQueryBuilder from(String from) {
		this.from = from;
		return this;
	}

	/**
	 * The from part of the range query. Null indicates unbounded.
	 */
	public ESRangeQueryBuilder from(int from) {
		this.from = from;
		return this;
	}

	/**
	 * The from part of the range query. Null indicates unbounded.
	 */
	public ESRangeQueryBuilder from(long from) {
		this.from = from;
		return this;
	}

	/**
	 * The from part of the range query. Null indicates unbounded.
	 */
	public ESRangeQueryBuilder from(float from) {
		this.from = from;
		return this;
	}

	/**
	 * The from part of the range query. Null indicates unbounded.
	 */
	public ESRangeQueryBuilder from(double from) {
		this.from = from;
		return this;
	}

	/**
	 * The from part of the range query. Null indicates unbounded.
	 */
	public ESRangeQueryBuilder gt(String from) {
		this.from = from;
		this.includeLower = false;
		return this;
	}

	/**
	 * The from part of the range query. Null indicates unbounded.
	 */
	public ESRangeQueryBuilder gt(Object from) {
		this.from = from;
		this.includeLower = false;
		return this;
	}

	/**
	 * The from part of the range query. Null indicates unbounded.
	 */
	public ESRangeQueryBuilder gt(int from) {
		this.from = from;
		this.includeLower = false;
		return this;
	}

	/**
	 * The from part of the range query. Null indicates unbounded.
	 */
	public ESRangeQueryBuilder gt(long from) {
		this.from = from;
		this.includeLower = false;
		return this;
	}

	/**
	 * The from part of the range query. Null indicates unbounded.
	 */
	public ESRangeQueryBuilder gt(float from) {
		this.from = from;
		this.includeLower = false;
		return this;
	}

	/**
	 * The from part of the range query. Null indicates unbounded.
	 */
	public ESRangeQueryBuilder gt(double from) {
		this.from = from;
		this.includeLower = false;
		return this;
	}

	/**
	 * The from part of the range query. Null indicates unbounded.
	 */
	public ESRangeQueryBuilder gte(String from) {
		this.from = from;
		this.includeLower = true;
		return this;
	}

	/**
	 * The from part of the range query. Null indicates unbounded.
	 */
	public ESRangeQueryBuilder gte(Object from) {
		this.from = from;
		this.includeLower = true;
		return this;
	}

	/**
	 * The from part of the range query. Null indicates unbounded.
	 */
	public ESRangeQueryBuilder gte(int from) {
		this.from = from;
		this.includeLower = true;
		return this;
	}

	/**
	 * The from part of the range query. Null indicates unbounded.
	 */
	public ESRangeQueryBuilder gte(long from) {
		this.from = from;
		this.includeLower = true;
		return this;
	}

	/**
	 * The from part of the range query. Null indicates unbounded.
	 */
	public ESRangeQueryBuilder gte(float from) {
		this.from = from;
		this.includeLower = true;
		return this;
	}

	/**
	 * The from part of the range query. Null indicates unbounded.
	 */
	public ESRangeQueryBuilder gte(double from) {
		this.from = from;
		this.includeLower = true;
		return this;
	}

	/**
	 * The to part of the range query. Null indicates unbounded.
	 */
	public ESRangeQueryBuilder to(Object to) {
		this.to = to;
		return this;
	}

	/**
	 * The to part of the range query. Null indicates unbounded.
	 */
	public ESRangeQueryBuilder to(String to) {
		this.to = to;
		return this;
	}

	/**
	 * The to part of the range query. Null indicates unbounded.
	 */
	public ESRangeQueryBuilder to(int to) {
		this.to = to;
		return this;
	}

	/**
	 * The to part of the range query. Null indicates unbounded.
	 */
	public ESRangeQueryBuilder to(long to) {
		this.to = to;
		return this;
	}

	/**
	 * The to part of the range query. Null indicates unbounded.
	 */
	public ESRangeQueryBuilder to(float to) {
		this.to = to;
		return this;
	}

	/**
	 * The to part of the range query. Null indicates unbounded.
	 */
	public ESRangeQueryBuilder to(double to) {
		this.to = to;
		return this;
	}

	/**
	 * The to part of the range query. Null indicates unbounded.
	 */
	public ESRangeQueryBuilder lt(String to) {
		this.to = to;
		this.includeUpper = false;
		return this;
	}

	/**
	 * The to part of the range query. Null indicates unbounded.
	 */
	public ESRangeQueryBuilder lt(Object to) {
		this.to = to;
		this.includeUpper = false;
		return this;
	}

	/**
	 * The to part of the range query. Null indicates unbounded.
	 */
	public ESRangeQueryBuilder lt(int to) {
		this.to = to;
		this.includeUpper = false;
		return this;
	}

	/**
	 * The to part of the range query. Null indicates unbounded.
	 */
	public ESRangeQueryBuilder lt(long to) {
		this.to = to;
		this.includeUpper = false;
		return this;
	}

	/**
	 * The to part of the range query. Null indicates unbounded.
	 */
	public ESRangeQueryBuilder lt(float to) {
		this.to = to;
		this.includeUpper = false;
		return this;
	}

	/**
	 * The to part of the range query. Null indicates unbounded.
	 */
	public ESRangeQueryBuilder lt(double to) {
		this.to = to;
		this.includeUpper = false;
		return this;
	}

	/**
	 * The to part of the range query. Null indicates unbounded.
	 */
	public ESRangeQueryBuilder lte(String to) {
		this.to = to;
		this.includeUpper = true;
		return this;
	}

	/**
	 * The to part of the range query. Null indicates unbounded.
	 */
	public ESRangeQueryBuilder lte(Object to) {
		this.to = to;
		this.includeUpper = true;
		return this;
	}

	/**
	 * The to part of the range query. Null indicates unbounded.
	 */
	public ESRangeQueryBuilder lte(int to) {
		this.to = to;
		this.includeUpper = true;
		return this;
	}

	/**
	 * The to part of the range query. Null indicates unbounded.
	 */
	public ESRangeQueryBuilder lte(long to) {
		this.to = to;
		this.includeUpper = true;
		return this;
	}

	/**
	 * The to part of the range query. Null indicates unbounded.
	 */
	public ESRangeQueryBuilder lte(float to) {
		this.to = to;
		this.includeUpper = true;
		return this;
	}

	/**
	 * The to part of the range query. Null indicates unbounded.
	 */
	public ESRangeQueryBuilder lte(double to) {
		this.to = to;
		this.includeUpper = true;
		return this;
	}

	/**
	 * Should the lower bound be included or not. Defaults to <tt>true</tt>.
	 */
	public ESRangeQueryBuilder includeLower(boolean includeLower) {
		this.includeLower = includeLower;
		return this;
	}

	/**
	 * Should the upper bound be included or not. Defaults to <tt>true</tt>.
	 */
	public ESRangeQueryBuilder includeUpper(boolean includeUpper) {
		this.includeUpper = includeUpper;
		return this;
	}

	/**
	 * Sets the boost for this query. Documents matching this query will (in
	 * addition to the normal weightings) have their score multiplied by the
	 * boost provided.
	 */
	@Override
	public ESRangeQueryBuilder boost(float boost) {
		this.boost = boost;
		return this;
	}

	/**
	 * Sets the query name for the filter that can be used when searching for
	 * matched_filters per hit.
	 */
	public ESRangeQueryBuilder queryName(String queryName) {
		this.queryName = queryName;
		return this;
	}

	/**
	 * In case of date field, we can adjust the from/to fields using a timezone
	 */
	public ESRangeQueryBuilder timeZone(String timezone) {
		this.timeZone = timezone;
		return this;
	}

	/**
	 * In case of date field, we can set the format to be used instead of the
	 * mapper format
	 */
	public ESRangeQueryBuilder format(String format) {
		this.format = format;
		return this;
	}

	@Override
	protected void doXContent(ESJsonContentBuilder builder) throws IOException {
		builder.startObject("range");
		builder.startObject(name);
		builder.field("from", from);
		builder.field("to", to);
		if (timeZone != null) {
			builder.field("time_zone", timeZone);
		}
		if (format != null) {
			builder.field("format", format);
		}
		builder.field("include_lower", includeLower);
		builder.field("include_upper", includeUpper);
		if (boost != -1) {
			builder.field("boost", boost);
		}
		builder.endObject();
		if (queryName != null) {
			builder.field("_name", queryName);
		}
		builder.endObject();
	}
}
