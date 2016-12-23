package io.spacedog.admin;

import com.google.common.base.Throwables;

import io.spacedog.client.SpaceRequestConfiguration;
import io.spacedog.utils.Internals;

public class AdminJobs {

	public static String ok(Object context) {
		notify(context, " is OK OK OK", "Everything is working properly.");
		return "OK";
	}

	public static String ok(Object context, String message) {
		notify(context, " is OK OK OK", message);
		return "OK";
	}

	public static String error(Object context, Throwable t) {
		error(context, Throwables.getStackTraceAsString(t));
		return t.getMessage();
	}

	public static String error(Object context, String message, Throwable t) {
		error(context, message + "\n" + Throwables.getStackTraceAsString(t));
		return t.getMessage();
	}

	public static void error(Object context, String message) {
		System.err.println();
		System.err.println(message);
		notify(context, " is DOWN DOWN DOWN", message);
	}

	static void notify(Object context, String titleSuffix, String message) {
		Internals.get().notify(//
				SpaceRequestConfiguration.get().superdogNotificationTopic(), //
				new StringBuilder(context.getClass().getSimpleName())//
						.append(titleSuffix)//
						.append(" (")//
						.append(SpaceRequestConfiguration.get().target().host())//
						.append(")")//
						.toString(), //
				message);
	}

}
