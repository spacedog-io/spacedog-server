package io.spacedog.client.stripe;

import java.util.Map;

import com.google.common.collect.Maps;

public class PaymentRequest {

	public String customerId;
	public long amountInCents;
	public String currency;
	public String sourceId;
	public String statement;
	public String description;

	public Map<String, String> toStripParameters() {
		Map<String, String> params = Maps.newHashMap();
		params.put("amount", Long.toString(amountInCents));
		params.put("currency", currency);
		params.put("customer", customerId);
		params.put("source", sourceId);
		params.put("statement_descriptor", statement);
		params.put("description", description);
		return params;
	}

}
