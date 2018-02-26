/**
 * © David Attias 2015
 */
package io.spacedog.client.sms;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SmsTemplate {

	public String name;
	public String from;
	public String to;
	public String body;
	public Map<String, String> model;
	public Set<String> roles;
}
