package io.spacedog.admin;

import com.google.common.base.Throwables;

import io.spacedog.client.SpaceEnv;
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
		return error(context, Throwables.getStackTraceAsString(t));
	}

	public static String error(Object context, String message, Throwable t) {
		return error(context, message + "\n" + Throwables.getStackTraceAsString(t));
	}

	public static String error(Object context, String message) {
		System.err.println();
		System.err.println(message);
		notify(context, " is DOWN DOWN DOWN", message);
		return message;
	}

	static void notify(Object context, String titleSuffix, String message) {
		Internals.get().notify(//
				SpaceEnv.defaultEnv().superdogNotificationTopic(), //
				new StringBuilder(context.getClass().getSimpleName())//
						.append(titleSuffix)//
						.append(" (")//
						.append(SpaceEnv.defaultEnv().target().host())//
						.append(")")//
						.toString(), //
				message);
	}

}
