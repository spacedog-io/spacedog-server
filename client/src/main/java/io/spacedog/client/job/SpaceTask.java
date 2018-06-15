package io.spacedog.client.job;

import org.joda.time.DateTime;

import io.spacedog.client.batch.SpaceCall;

public class SpaceTask {

	public enum Status {
		schedulled, ready, in_progress, completed, cancelled
	}

	public String jobName;
	public SpaceCall request;
	public DateTime scheduled;
	public DateTime started;
	public DateTime stopped;
	public int tries = 0;
	public Status status;
	public Object response;

	public String id() {
		return jobName + '-' + scheduled.getMillis();
	}

	public long delay() {
		return scheduled.getMillis() - DateTime.now().getMillis();
	}
}
