package io.spacedog.client.log;

import java.util.List;
import java.util.Set;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;

import io.spacedog.client.SpaceDog;
import io.spacedog.client.credentials.Credentials;
import io.spacedog.client.elastic.ESSearchSourceBuilder;
import io.spacedog.client.http.SpaceParams;

public class LogClient implements SpaceParams {

	private SpaceDog dog;

	public LogClient(SpaceDog session) {
		this.dog = session;
	}

	public LogSearchResults get() {
		return get(10);
	}

	public LogSearchResults get(int size) {
		return get(size, false);
	}

	public LogSearchResults get(int size, boolean refresh) {
		return dog.get("/1/log").size(size).refresh(refresh).go(200)//
				.asPojo(LogSearchResults.class);
	}

	public LogSearchResults search(ESSearchSourceBuilder builder) {
		return search(builder, false);
	}

	public LogSearchResults search(ESSearchSourceBuilder builder, boolean refresh) {
		return dog.post("/1/log/_search").refresh(refresh)//
				.bodyJson(builder.toString()).go(200)//
				.asPojo(LogSearchResults.class);
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
		public ObjectNode payload;
		public ObjectNode response;

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
		dog.delete("/1/log").queryParam("before", before).go(200);
	}

}
