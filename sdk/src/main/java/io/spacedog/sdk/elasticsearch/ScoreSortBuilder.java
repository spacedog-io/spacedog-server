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

import java.io.IOException;

/**
 * A sort builder allowing to sort by score.
 *
 *
 */
public class ScoreSortBuilder extends SortBuilder {

	private SortOrder order;

	/**
	 * The order of sort scoring. By default, its {@link SortOrder#DESC}.
	 */
	@Override
	public ScoreSortBuilder order(SortOrder order) {
		this.order = order;
		return this;
	}

	@Override
	public SortBuilder missing(Object missing) {
		return this;
	}

	@Override
	public JsonContentBuilder toJsonContent(JsonContentBuilder builder) throws IOException {
		builder.startObject("_score");
		if (order == SortOrder.ASC) {
			builder.field("reverse", true);
		}
		builder.endObject();
		return builder;
	}
}