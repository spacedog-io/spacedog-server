package io.spacedog.sdk;

import java.util.List;
import java.util.Set;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;

import io.spacedog.rest.SpaceRequest;
import io.spacedog.utils.Credentials;

public class LogEndpoint {

	SpaceDog dog;

	LogEndpoint(SpaceDog session) {
		this.dog = session;
	}

	public LogSearchResults get() {
		return get(10);
	}

	public LogSearchResults get(int size) {
		return get(size, false);
	}

	public LogSearchResults get(int size, boolean refresh) {
		SpaceRequest request = dog.get("/1/log").size(size);
		if (refresh)
			request.refresh();
		return request.go(200).toPojo(LogSearchResults.class);
	}

	// ignore unknown fields
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class LogSearchResults {
		public long total;
		public List<LogItem> results;
	}

	// ignore unknown fields
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class LogItem {
		public String method;
		public String path;
		public DateTime receivedAt;
		public int processedIn;
		public int status;
		public Credentials credentials;
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
	}

	public void delete(DateTime before) {
		dog.delete("/1/log").queryParam("before", before.toString()).go(200);
	}

}
