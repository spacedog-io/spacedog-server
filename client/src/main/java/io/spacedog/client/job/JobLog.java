package io.spacedog.client.job;

import org.joda.time.DateTime;

public class JobLog {

	public DateTime timestamp;
	public String message;

	public JobLog() {
	}

	public JobLog(long timestamp, String message) {
		this.timestamp = new DateTime(timestamp);
		this.message = message;
	}
}
