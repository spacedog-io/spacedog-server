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

import io.spacedog.utils.Utils;

/**
 * A Query that matches documents containing terms with a specified prefix.
 */
public class ESPrefixQueryBuilder extends ESQueryBuilder {

	private final String fieldName;

	private final String value;

	/**
	 * A Query that matches documents containing terms with a specified prefix.
	 *
	 * @param fieldName
	 *            The name of the field
	 * @param value
	 *            The prefix query
	 */
	public ESPrefixQueryBuilder(String fieldName, String value) {
		if (Utils.isNullOrEmpty(fieldName)) {
			throw new IllegalArgumentException("field name is null or empty");
		}
		if (value == null) {
			throw new IllegalArgumentException("value cannot be null");
		}
		this.fieldName = fieldName;
		this.value = value;
	}

	@Override
	public void doXContent(ESJsonContentBuilder builder) throws IOException {
		builder.startObject("prefix");
		builder.startObject(this.fieldName);
		builder.field("value", this.value);
		builder.endObject();
		builder.endObject();
	}
}
