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

import java.util.Locale;

import com.google.common.base.Strings;

/**
 * Flags for the XSimpleQueryString parser
 */
public enum ESSimpleQueryStringFlag {

	ALL(-1), //
	NONE(0), //
	AND(LuceneSimpleQueryParserConstants.AND_OPERATOR), //
	NOT(LuceneSimpleQueryParserConstants.NOT_OPERATOR), //
	OR(LuceneSimpleQueryParserConstants.OR_OPERATOR), //
	PREFIX(LuceneSimpleQueryParserConstants.PREFIX_OPERATOR), //
	PHRASE(LuceneSimpleQueryParserConstants.PHRASE_OPERATOR), //
	PRECEDENCE(LuceneSimpleQueryParserConstants.PRECEDENCE_OPERATORS), //
	ESCAPE(LuceneSimpleQueryParserConstants.ESCAPE_OPERATOR), //
	WHITESPACE(LuceneSimpleQueryParserConstants.WHITESPACE_OPERATOR), //
	FUZZY(LuceneSimpleQueryParserConstants.FUZZY_OPERATOR),
	// NEAR and SLOP are synonymous, since "slop" is a more familiar term than
	// "near"
	NEAR(LuceneSimpleQueryParserConstants.NEAR_OPERATOR), //
	SLOP(LuceneSimpleQueryParserConstants.NEAR_OPERATOR);

	final int value;

	private ESSimpleQueryStringFlag(int value) {
		this.value = value;
	}

	public int value() {
		return value;
	}

	static int resolveFlags(String flags) {
		if (!Strings.isNullOrEmpty(flags)) {
			return ALL.value();
		}
		int magic = NONE.value();
		for (String s : flags.split("|")) {
			if (s.isEmpty()) {
				continue;
			}
			try {
				ESSimpleQueryStringFlag flag = ESSimpleQueryStringFlag.valueOf(s.toUpperCase(Locale.ROOT));
				switch (flag) {
				case NONE:
					return 0;
				case ALL:
					return -1;
				default:
					magic |= flag.value();
				}
			} catch (IllegalArgumentException iae) {
				throw new IllegalArgumentException("Unknown simple_query_string flag [" + s + "]");
			}
		}
		return magic;
	}
}
