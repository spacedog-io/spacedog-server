package io.spacedog.client.elastic;

import java.io.IOException;

import io.spacedog.utils.Check;

public class ESMatchPhraseQueryBuilder extends ESQueryBuilder
		implements ESBoostableQueryBuilder<ESMatchPhraseQueryBuilder> {

	private String fieldName;
	private Object value;
	private String analyzer;
	private Float boost;
	private Integer slop;

	public ESMatchPhraseQueryBuilder(String fieldName, Object value) {
		Check.notNullOrEmpty(fieldName, "fieldName");
		Check.notNull(value, "value");
		this.fieldName = fieldName;
		this.value = value;
	}

	/**
	 * Set the boost to apply to the query.
	 */
	@Override
	public ESMatchPhraseQueryBuilder boost(float boost) {
		this.boost = boost;
		return this;
	}

	/** Sets a slop factor for phrase queries */
	public ESMatchPhraseQueryBuilder slop(int slop) {
		if (slop < 0)
			throw new IllegalArgumentException("No negative slop allowed.");

		this.slop = slop;
		return this;
	}

	@Override
	protected void doXContent(ESJsonContentBuilder builder) throws IOException {
		builder.startObject("match_phrase");
		builder.startObject(fieldName);

		builder.field("query", value);

		if (analyzer != null)
			builder.field("analyzer", analyzer);
		if (boost != null)
			builder.field("boost", boost);
		if (slop != null)
			builder.field("slop", slop);

		builder.endObject();
		builder.endObject();
	}

}
