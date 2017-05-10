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

package io.spacedog.sdk.elasticsearch;

import java.util.Collection;

/**
 * A static factory for simple "import static" usage.
 */
public abstract class QueryBuilders {

	/**
	 * A query that match on all documents.
	 */
	public static MatchAllQueryBuilder matchAllQuery() {
		return new MatchAllQueryBuilder();
	}

	/**
	 * Constructs a query that will match only specific ids within types.
	 *
	 * @param types
	 *            The mapping/doc type
	 */
	public static IdsQueryBuilder idsQuery(String... types) {
		return new IdsQueryBuilder(types);
	}

	/**
	 * A Query that matches documents containing a term.
	 *
	 * @param name
	 *            The name of the field
	 * @param value
	 *            The value of the term
	 */
	public static TermQueryBuilder termQuery(String name, String value) {
		return new TermQueryBuilder(name, value);
	}

	/**
	 * A Query that matches documents containing a term.
	 *
	 * @param name
	 *            The name of the field
	 * @param value
	 *            The value of the term
	 */
	public static TermQueryBuilder termQuery(String name, int value) {
		return new TermQueryBuilder(name, value);
	}

	/**
	 * A Query that matches documents containing a term.
	 *
	 * @param name
	 *            The name of the field
	 * @param value
	 *            The value of the term
	 */
	public static TermQueryBuilder termQuery(String name, long value) {
		return new TermQueryBuilder(name, value);
	}

	/**
	 * A Query that matches documents containing a term.
	 *
	 * @param name
	 *            The name of the field
	 * @param value
	 *            The value of the term
	 */
	public static TermQueryBuilder termQuery(String name, float value) {
		return new TermQueryBuilder(name, value);
	}

	/**
	 * A Query that matches documents containing a term.
	 *
	 * @param name
	 *            The name of the field
	 * @param value
	 *            The value of the term
	 */
	public static TermQueryBuilder termQuery(String name, double value) {
		return new TermQueryBuilder(name, value);
	}

	/**
	 * A Query that matches documents containing a term.
	 *
	 * @param name
	 *            The name of the field
	 * @param value
	 *            The value of the term
	 */
	public static TermQueryBuilder termQuery(String name, boolean value) {
		return new TermQueryBuilder(name, value);
	}

	/**
	 * A Query that matches documents containing a term.
	 *
	 * @param name
	 *            The name of the field
	 * @param value
	 *            The value of the term
	 */
	public static TermQueryBuilder termQuery(String name, Object value) {
		return new TermQueryBuilder(name, value);
	}

	/**
	 * A Query that matches documents within an range of terms.
	 *
	 * @param name
	 *            The field name
	 */
	public static RangeQueryBuilder rangeQuery(String name) {
		return new RangeQueryBuilder(name);
	}

	/**
	 * A Query that matches documents matching boolean combinations of other
	 * queries.
	 */
	public static BoolQueryBuilder boolQuery() {
		return new BoolQueryBuilder();
	}

	/**
	 * A filer for a field based on several terms matching on any of them.
	 *
	 * @param name
	 *            The field name
	 * @param values
	 *            The terms
	 */
	public static TermsQueryBuilder termsQuery(String name, String... values) {
		return new TermsQueryBuilder(name, values);
	}

	/**
	 * A filer for a field based on several terms matching on any of them.
	 *
	 * @param name
	 *            The field name
	 * @param values
	 *            The terms
	 */
	public static TermsQueryBuilder termsQuery(String name, int... values) {
		return new TermsQueryBuilder(name, values);
	}

	/**
	 * A filer for a field based on several terms matching on any of them.
	 *
	 * @param name
	 *            The field name
	 * @param values
	 *            The terms
	 */
	public static TermsQueryBuilder termsQuery(String name, long... values) {
		return new TermsQueryBuilder(name, values);
	}

	/**
	 * A filer for a field based on several terms matching on any of them.
	 *
	 * @param name
	 *            The field name
	 * @param values
	 *            The terms
	 */
	public static TermsQueryBuilder termsQuery(String name, float... values) {
		return new TermsQueryBuilder(name, values);
	}

	/**
	 * A filer for a field based on several terms matching on any of them.
	 *
	 * @param name
	 *            The field name
	 * @param values
	 *            The terms
	 */
	public static TermsQueryBuilder termsQuery(String name, double... values) {
		return new TermsQueryBuilder(name, values);
	}

	/**
	 * A filer for a field based on several terms matching on any of them.
	 *
	 * @param name
	 *            The field name
	 * @param values
	 *            The terms
	 */
	public static TermsQueryBuilder termsQuery(String name, Object... values) {
		return new TermsQueryBuilder(name, values);
	}

	/**
	 * A filer for a field based on several terms matching on any of them.
	 *
	 * @param name
	 *            The field name
	 * @param values
	 *            The terms
	 */
	public static TermsQueryBuilder termsQuery(String name, Collection<?> values) {
		return new TermsQueryBuilder(name, values);
	}

	/**
	 * A filter to filter based on a specific distance from a specific geo
	 * location / point.
	 *
	 * @param name
	 *            The location field name.
	 */
	public static GeoDistanceQueryBuilder geoDistanceQuery(String name) {
		return new GeoDistanceQueryBuilder(name);
	}

	/**
	 * A filter to filter only documents where a field exists in them.
	 *
	 * @param name
	 *            The name of the field
	 */
	public static ExistsQueryBuilder existsQuery(String name) {
		return new ExistsQueryBuilder(name);
	}

	private QueryBuilders() {

	}
}
