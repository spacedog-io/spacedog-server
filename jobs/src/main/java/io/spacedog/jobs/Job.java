package io.spacedog.jobs;

import java.util.List;

import org.joda.time.DateTime;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

import io.spacedog.rest.SpaceEnv;
import io.spacedog.jobs.Internals;
import io.spacedog.utils.Utils;

public class Job {

	public static final String OK = "OK";

	private List<String> description;

	public void addToDescription(String string) {
		if (description == null)
			description = Lists.newArrayList();

		description.add(string);
	}

	public String description() {
		if (description == null)
			return "Unknown";

		// return String.join("/", description);
		return Utils.join("/", description);
	}

	public String okOncePerDay() {
		int hourOfDay = DateTime.now().hourOfDay().get();
		if (3 <= hourOfDay && hourOfDay < 4)
			ok();
		return OK;
	}

	public String ok() {
		notify(" is OK OK OK", "Everything is working properly.");
		return OK;
	}

	public String ok(String message) {
		notify(" is OK OK OK", message);
		return OK;
	}

	public String error(Throwable t) {
		return error(Throwables.getStackTraceAsString(t));
	}

	public String error(String message, Throwable t) {
		return error(message + "\n" + Throwables.getStackTraceAsString(t));
	}

	public String error(String message) {
		System.err.println();
		System.err.println(message);
		notify(" is DOWN DOWN DOWN", message);
		return message;
	}

	void notify(String titleSuffix, String message) {
		SpaceEnv env = SpaceEnv.defaultEnv();
		Internals.get().notify(//
				env.superdogNotificationTopic(), //
				description() + titleSuffix, //
				message);
	}
}
