/**
 * © David Attias 2015
 */
package io.spacedog.client.email;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EmailTemplate {

	public String name;
	public String from;
	public List<String> to;
	public List<String> cc;
	public List<String> bcc;
	public String subject;
	public String text;
	public String html;
	public Map<String, String> model;
	public Set<String> authorizedRoles;
}
