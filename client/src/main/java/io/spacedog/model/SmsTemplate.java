/**
 * © David Attias 2015
 */
package io.spacedog.model;

import java.util.Map;
import java.util.Set;

public class SmsTemplate {

	public String from;
	public String to;
	public String body;
	public Map<String, String> model;
	public Set<String> roles;
}
