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
 * A Query that matches documents containing a term.
 */
public class ESTermQueryBuilder extends ESQueryBuilder implements ESBoostableQueryBuilder<ESTermQueryBuilder> {

	private final String name;

	private final Object value;

	private float boost = -1;

	private String queryName;

	/**
	 * Constructs a new term query.
	 *
	 * @param name
	 *            The name of the field
	 * @param value
	 *            The value of the term
	 */
	public ESTermQueryBuilder(String name, String value) {
		this(name, (Object) value);
	}

	/**
	 * Constructs a new term query.
	 *
	 * @param name
	 *            The name of the field
	 * @param value
	 *            The value of the term
	 */
	public ESTermQueryBuilder(String name, int value) {
		this(name, (Object) value);
	}

	/**
	 * Constructs a new term query.
	 *
	 * @param name
	 *            The name of the field
	 * @param value
	 *            The value of the term
	 */
	public ESTermQueryBuilder(String name, long value) {
		this(name, (Object) value);
	}

	/**
	 * Constructs a new term query.
	 *
	 * @param name
	 *            The name of the field
	 * @param value
	 *            The value of the term
	 */
	public ESTermQueryBuilder(String name, float value) {
		this(name, (Object) value);
	}

	/**
	 * Constructs a new term query.
	 *
	 * @param name
	 *            The name of the field
	 * @param value
	 *            The value of the term
	 */
	public ESTermQueryBuilder(String name, double value) {
		this(name, (Object) value);
	}

	/**
	 * Constructs a new term query.
	 *
	 * @param name
	 *            The name of the field
	 * @param value
	 *            The value of the term
	 */
	public ESTermQueryBuilder(String name, boolean value) {
		this(name, (Object) value);
	}

	/**
	 * Constructs a new term query.
	 *
	 * @param name
	 *            The name of the field
	 * @param value
	 *            The value of the term
	 */
	public ESTermQueryBuilder(String name, Object value) {
		this.name = name;
		this.value = value;
	}

	/**
	 * Sets the boost for this query. Documents matching this query will (in
	 * addition to the normal weightings) have their score multiplied by the
	 * boost provided.
	 */
	@Override
	public ESTermQueryBuilder boost(float boost) {
		this.boost = boost;
		return this;
	}

	/**
	 * Sets the query name for the filter that can be used when searching for
	 * matched_filters per hit.
	 */
	public ESTermQueryBuilder queryName(String queryName) {
		this.queryName = queryName;
		return this;
	}

	@Override
	public void doXContent(ESJsonContentBuilder builder) throws IOException {
		builder.startObject("term");
		if (boost == -1 && queryName == null) {
			builder.field(name, value);
		} else {
			builder.startObject(name);
			builder.field("value", value);
			if (boost != -1) {
				builder.field("boost", boost);
			}
			if (queryName != null) {
				builder.field("_name", queryName);
			}
			builder.endObject();
		}
		builder.endObject();
	}
}
