package io.spacedog.client.sms;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SmsBasicRequest extends SmsRequest {
	public String from;
	public String to;
	public String body;

	public SmsBasicRequest from(String from) {
		this.from = from;
		return this;
	}

	public SmsBasicRequest to(String to) {
		this.to = to;
		return this;
	}

	public SmsBasicRequest body(String body) {
		this.body = body;
		return this;
	}
}