package io.spacedog.client.email;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EmailTemplateRequest extends EmailRequest {
	public String templateName;
	public Map<String, Object> parameters;
}