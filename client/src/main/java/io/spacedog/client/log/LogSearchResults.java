package io.spacedog.client.log;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.node.ObjectNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class LogSearchResults {
	public long total;
	public List<LogItem> results;
	public ObjectNode aggregations;
}