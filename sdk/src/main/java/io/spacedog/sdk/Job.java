package io.spacedog.sdk;

import java.util.List;
import java.util.Optional;

import org.joda.time.DateTime;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

import io.spacedog.client.SpaceEnv;
import io.spacedog.utils.Internals;

public class Job {

	private List<String> description;

	public void addToDescription(String string) {
		if (description == null)
			description = Lists.newArrayList(string);
		else
			description.add(string);
	}

	public void addToDescription(Context context) {
		addToDescription(context.getFunctionName());
		addToDescription(context.getFunctionVersion());
	}

	public String description() {
		if (description == null)
			return "[unknown]";

		StringBuilder builder = new StringBuilder();
		for (Object string : description)
			builder.append('[').append(string).append(']');

		return builder.toString();
	}

	public String okOncePerDay() {
		int hourOfDay = DateTime.now().hourOfDay().get();
		if (3 <= hourOfDay && hourOfDay < 4)
			ok();
		return "OK";
	}

	public String ok() {
		notify(" is OK OK OK", "Everything is working properly.");
		return "OK";
	}

	public String ok(String message) {
		notify(" is OK OK OK", message);
		return "OK";
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
				Optional.ofNullable(env.superdogNotificationTopic()), //
				description() + titleSuffix, //
				message);
	}
}
