package io.spacedog.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.node.ObjectNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EmailTemplateRequest {
	public String templateName;
	public ObjectNode parameters;
}