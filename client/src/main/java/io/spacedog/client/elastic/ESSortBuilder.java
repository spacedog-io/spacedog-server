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

import io.spacedog.utils.Exceptions;

/**
 *
 */
public abstract class ESSortBuilder implements ESJsonContent {

	/**
	 * The order of sorting. Defaults to {@link ESSortOrder#ASC}.
	 */
	public abstract ESSortBuilder order(ESSortOrder order);

	/**
	 * Sets the value when a field is missing in a doc. Can also be set to
	 * <tt>_last</tt> or <tt>_first</tt> to sort missing last or first
	 * respectively.
	 */
	public abstract ESSortBuilder missing(Object missing);

	@Override
	public abstract ESJsonContentBuilder toJsonContent(ESJsonContentBuilder builder) throws IOException;

	@Override
	public String toString() {
		try {
			ESJsonContentBuilder builder = new ESJsonContentBuilder();
			builder.prettyPrint();
			toJsonContent(builder);
			return builder.string();
		} catch (Exception e) {
			throw Exceptions.runtime(e);
		}
	}
}
