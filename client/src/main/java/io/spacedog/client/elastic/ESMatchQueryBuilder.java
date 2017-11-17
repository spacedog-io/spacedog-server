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
import java.util.Locale;

/**
 * Match query is a query that analyzes the text and constructs a query as the
 * result of the analysis. It can construct different queries based on the type
 * provided.
 */
public class ESMatchQueryBuilder extends ESQueryBuilder implements ESBoostableQueryBuilder<ESMatchQueryBuilder> {

	public enum Operator {
		OR, AND
	}

	public enum Type {
		/**
		 * The text is analyzed and terms are added to a boolean query.
		 */
		BOOLEAN,
		/**
		 * The text is analyzed and used as a phrase query.
		 */
		PHRASE,
		/**
		 * The text is analyzed and used in a phrase query, with the last term acting as
		 * a prefix.
		 */
		PHRASE_PREFIX
	}

	public enum ZeroTermsQuery {
		NONE, ALL
	}

	private final String name;

	private final Object text;

	private Type type;

	private Operator operator;

	private String analyzer;

	private Float boost;

	private Integer slop;

	private ESFuzziness fuzziness;

	private Integer prefixLength;

	private Integer maxExpansions;

	private String minimumShouldMatch;

	private String fuzzyRewrite = null;

	private Boolean lenient;

	private Boolean fuzzyTranspositions = null;

	private ZeroTermsQuery zeroTermsQuery;

	private Float cutoff_Frequency = null;

	private String queryName;

	/**
	 * Constructs a new text query.
	 */
	public ESMatchQueryBuilder(String name, Object text) {
		this.name = name;
		this.text = text;
	}

	/**
	 * Sets the type of the text query.
	 */
	public ESMatchQueryBuilder type(Type type) {
		this.type = type;
		return this;
	}

	/**
	 * Sets the operator to use when using a boolean query. Defaults to <tt>OR</tt>.
	 */
	public ESMatchQueryBuilder operator(Operator operator) {
		this.operator = operator;
		return this;
	}

	/**
	 * Explicitly set the analyzer to use. Defaults to use explicit mapping config
	 * for the field, or, if not set, the default search analyzer.
	 */
	public ESMatchQueryBuilder analyzer(String analyzer) {
		this.analyzer = analyzer;
		return this;
	}

	/**
	 * Set the boost to apply to the query.
	 */
	@Override
	public ESMatchQueryBuilder boost(float boost) {
		this.boost = boost;
		return this;
	}

	/**
	 * Set the phrase slop if evaluated to a phrase query type.
	 */
	public ESMatchQueryBuilder slop(int slop) {
		this.slop = slop;
		return this;
	}

	/**
	 * Sets the fuzziness used when evaluated to a fuzzy query type. Defaults to
	 * "AUTO".
	 */
	public ESMatchQueryBuilder fuzziness(Object fuzziness) {
		this.fuzziness = ESFuzziness.build(fuzziness);
		return this;
	}

	public ESMatchQueryBuilder prefixLength(int prefixLength) {
		this.prefixLength = prefixLength;
		return this;
	}

	/**
	 * When using fuzzy or prefix type query, the number of term expansions to use.
	 * Defaults to unbounded so its recommended to set it to a reasonable value for
	 * faster execution.
	 */
	public ESMatchQueryBuilder maxExpansions(int maxExpansions) {
		this.maxExpansions = maxExpansions;
		return this;
	}

	/**
	 * Set a cutoff value in [0..1] (or absolute number &gt;=1) representing the
	 * maximum threshold of a terms document frequency to be considered a low
	 * frequency term.
	 */
	public ESMatchQueryBuilder cutoffFrequency(float cutoff) {
		this.cutoff_Frequency = cutoff;
		return this;
	}

	public ESMatchQueryBuilder minimumShouldMatch(String minimumShouldMatch) {
		this.minimumShouldMatch = minimumShouldMatch;
		return this;
	}

	public ESMatchQueryBuilder fuzzyRewrite(String fuzzyRewrite) {
		this.fuzzyRewrite = fuzzyRewrite;
		return this;
	}

	public ESMatchQueryBuilder fuzzyTranspositions(boolean fuzzyTranspositions) {
		// LUCENE 4 UPGRADE add documentation
		this.fuzzyTranspositions = fuzzyTranspositions;
		return this;
	}

	/**
	 * Sets whether format based failures will be ignored.
	 */
	public ESMatchQueryBuilder setLenient(boolean lenient) {
		this.lenient = lenient;
		return this;
	}

	public ESMatchQueryBuilder zeroTermsQuery(ZeroTermsQuery zeroTermsQuery) {
		this.zeroTermsQuery = zeroTermsQuery;
		return this;
	}

	/**
	 * Sets the query name for the filter that can be used when searching for
	 * matched_filters per hit.
	 */
	public ESMatchQueryBuilder queryName(String queryName) {
		this.queryName = queryName;
		return this;
	}

	@Override
	public void doXContent(ESJsonContentBuilder builder) throws IOException {
		builder.startObject("match");
		builder.startObject(name);

		builder.field("query", text);
		if (type != null) {
			builder.field("type", type.toString().toLowerCase(Locale.ENGLISH));
		}
		if (operator != null) {
			builder.field("operator", operator.toString());
		}
		if (analyzer != null) {
			builder.field("analyzer", analyzer);
		}
		if (boost != null) {
			builder.field("boost", boost);
		}
		if (slop != null) {
			builder.field("slop", slop);
		}
		if (fuzziness != null) {
			fuzziness.toJsonContent(builder);
		}
		if (prefixLength != null) {
			builder.field("prefix_length", prefixLength);
		}
		if (maxExpansions != null) {
			builder.field("max_expansions", maxExpansions);
		}
		if (minimumShouldMatch != null) {
			builder.field("minimum_should_match", minimumShouldMatch);
		}
		if (fuzzyRewrite != null) {
			builder.field("fuzzy_rewrite", fuzzyRewrite);
		}
		if (fuzzyTranspositions != null) {
			// LUCENE 4 UPGRADE we need to document this & test this
			builder.field("fuzzy_transpositions", fuzzyTranspositions);
		}
		if (lenient != null) {
			builder.field("lenient", lenient);
		}
		if (zeroTermsQuery != null) {
			builder.field("zero_terms_query", zeroTermsQuery.toString());
		}
		if (cutoff_Frequency != null) {
			builder.field("cutoff_frequency", cutoff_Frequency);
		}
		if (queryName != null) {
			builder.field("_name", queryName);
		}

		builder.endObject();
		builder.endObject();
	}
}
