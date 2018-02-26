package io.spacedog.client.sms;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SmsTemplateRequest extends SmsRequest {
	public String templateName;
	public Map<String, Object> parameters;
}