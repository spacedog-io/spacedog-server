package io.spacedog.client.job;

import java.util.Map;

public class LambdaJob {
	public String name;
	public String handler;
	public String when;
	public Map<String, String> env;
	public String description;
	public int timeoutInSeconds = 60 * 5;
	public int memoryInMBytes = 128;
	public byte[] code;
}