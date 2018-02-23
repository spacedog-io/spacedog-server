package io.spacedog.client.elastic;

import java.io.IOException;

public class ESMatchPhrasePrefixQueryBuilder extends ESQueryBuilder
		implements ESBoostableQueryBuilder<ESMatchPhrasePrefixQueryBuilder> {

	private Float boost;
	private String analyzer;
	private String fieldName;
	private Object value;
	private Integer slop;
	private Integer maxExpansions;

	public ESMatchPhrasePrefixQueryBuilder(String fieldName, Object text) {
		this.fieldName = fieldName;
		this.value = text;
	}

	@Override
	public ESMatchPhrasePrefixQueryBuilder boost(float boost) {
		this.boost = boost;
		return this;
	}

	/** Sets a slop factor for phrase queries */
	public ESMatchPhrasePrefixQueryBuilder slop(int slop) {
		if (slop < 0) {
			throw new IllegalArgumentException("No negative slop allowed.");
		}
		this.slop = slop;
		return this;
	}

	/**
	 * The number of term expansions to use.
	 */
	public ESMatchPhrasePrefixQueryBuilder maxExpansions(int maxExpansions) {
		if (maxExpansions < 0) {
			throw new IllegalArgumentException("No negative maxExpansions allowed.");
		}
		this.maxExpansions = maxExpansions;
		return this;
	}

	@Override
	protected void doXContent(ESJsonContentBuilder builder) throws IOException {
		builder.startObject("match_phrase_prefix");
		builder.startObject(fieldName);

		builder.field("query", value);

		if (analyzer != null)
			builder.field("analyzer", analyzer);
		if (boost != null)
			builder.field("boost", boost);
		if (maxExpansions != null)
			builder.field("max_expansions", maxExpansions);
		if (slop != null)
			builder.field("slop", slop);

		builder.endObject();
		builder.endObject();
	}

}
