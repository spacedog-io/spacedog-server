package io.spacedog.client.log;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class LogSearchResults {
	public long total;
	public List<LogItem> results;
}