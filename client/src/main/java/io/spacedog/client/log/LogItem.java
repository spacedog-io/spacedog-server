package io.spacedog.client.log;

import java.util.List;
import java.util.Set;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class LogItem {
	public String method;
	public String path;
	public DateTime receivedAt;
	public int processedIn;
	public int status;
	public CredentialsResume credentials;
	public List<String> headers;
	public List<String> parameters;
	public JsonNode payload;
	public JsonNode response;

	public String getParameter(String name) {
		for (String string : parameters) {
			String[] parts = string.split(": ", 2);
			if (parts[0].equals(name))
				return parts[1];
		}
		return null;
	}

	public Set<String> getHeader(String name) {
		Set<String> values = Sets.newHashSet();
		for (String string : headers) {
			String[] parts = string.split(": ", 2);
			if (parts[0].equals(name))
				values.add(parts[1]);
		}
		return values;
	}

	public static class CredentialsResume {
		public String id;
		public String username;
		public Set<String> roles;
	}
}