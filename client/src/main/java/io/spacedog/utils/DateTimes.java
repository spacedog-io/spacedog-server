package io.spacedog.utils;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class DateTimes {
	public final static DateTimeZone PARIS = DateTimeZone.forID("Europe/Paris");

	public static double toEpochSeconds(DateTime date) {
		return ((double) date.getMillis()) / 1000;
	}

	public static DateTime toDateTime(double epochSeconds) {
		return new DateTime((long) (epochSeconds * 1000));
	}

}