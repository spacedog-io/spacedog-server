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

/**
 * A set of static factory methods for {@link SortBuilder}s.
 *
 *
 */
public class SortBuilders {

	/**
	 * Constructs a new score sort.
	 */
	public static ScoreSortBuilder scoreSort() {
		return new ScoreSortBuilder();
	}

	/**
	 * Constructs a new field based sort.
	 *
	 * @param field
	 *            The field name.
	 */
	public static FieldSortBuilder fieldSort(String field) {
		return new FieldSortBuilder(field);
	}

	/**
	 * A geo distance based sort.
	 *
	 * @param fieldName
	 *            The geo point like field name.
	 */
	public static GeoDistanceSortBuilder geoDistanceSort(String fieldName) {
		return new GeoDistanceSortBuilder(fieldName);
	}
}