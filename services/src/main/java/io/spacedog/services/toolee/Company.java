package io.spacedog.services.toolee;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Company {
	public String id;
	public String name;
	public int notifications;
	public long docsToAnalyze;
	public long docsToProcess;
}
