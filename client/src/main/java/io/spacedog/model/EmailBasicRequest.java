package io.spacedog.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EmailBasicRequest extends EmailRequest {
	public String from;
	public List<String> to;
	public List<String> cc;
	public List<String> bcc;
	public String subject;
	public String text;
	public String html;

	public EmailBasicRequest to(List<String> to) {
		this.to = to;
		return this;
	}

	public EmailBasicRequest html(String html) {
		this.html = html;
		return this;
	}
}