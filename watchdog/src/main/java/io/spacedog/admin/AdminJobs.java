package io.spacedog.admin;

import com.google.common.base.Throwables;

import io.spacedog.client.SpaceRequestConfiguration;
import io.spacedog.utils.Internals;

public class AdminJobs {

	public static void ok(Object context) {
		notify(context, " is up and runing", "Everything is working properly.");
	}

	public static void ok(Object context, String message) {
		notify(context, " is up and runing", message);
	}

	public static void error(Object context, Throwable t) {
		error(context, Throwables.getStackTraceAsString(t));
	}

	public static void error(Object context, String message, Throwable t) {
		error(context, message + "\n\n" + Throwables.getStackTraceAsString(t));
	}

	public static void error(Object context, String message) {
		System.err.println(message);
		notify(context, " is DOWN DOWN DOWN", message);
		System.exit(-1);
	}

	static void notify(Object context, String titleSuffix, String message) {
		Internals.get().notify(//
				SpaceRequestConfiguration.get().superdogNotificationTopic(), //
				new StringBuilder(context.getClass().getSimpleName())//
						.append(" (")//
						.append(SpaceRequestConfiguration.get().target().host())//
						.append(")")//
						.append(titleSuffix)//
						.toString(), //
				message);
	}

}
