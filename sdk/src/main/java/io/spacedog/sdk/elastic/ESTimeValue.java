/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.spacedog.sdk.elastic;

import java.util.concurrent.TimeUnit;

import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.format.PeriodFormat;
import org.joda.time.format.PeriodFormatter;

public class ESTimeValue {// implements Streamable {

	/** How many nano-seconds in one milli-second */
	public static final long NSEC_PER_MSEC = 1000000;

	public static ESTimeValue timeValueNanos(long nanos) {
		return new ESTimeValue(nanos, TimeUnit.NANOSECONDS);
	}

	public static ESTimeValue timeValueMillis(long millis) {
		return new ESTimeValue(millis, TimeUnit.MILLISECONDS);
	}

	public static ESTimeValue timeValueSeconds(long seconds) {
		return new ESTimeValue(seconds, TimeUnit.SECONDS);
	}

	public static ESTimeValue timeValueMinutes(long minutes) {
		return new ESTimeValue(minutes, TimeUnit.MINUTES);
	}

	public static ESTimeValue timeValueHours(long hours) {
		return new ESTimeValue(hours, TimeUnit.HOURS);
	}

	private long duration;

	private TimeUnit timeUnit;

	public ESTimeValue(long millis) {
		this(millis, TimeUnit.MILLISECONDS);
	}

	public ESTimeValue(long duration, TimeUnit timeUnit) {
		this.duration = duration;
		this.timeUnit = timeUnit;
	}

	public long nanos() {
		return timeUnit.toNanos(duration);
	}

	public long getNanos() {
		return nanos();
	}

	public long micros() {
		return timeUnit.toMicros(duration);
	}

	public long getMicros() {
		return micros();
	}

	public long millis() {
		return timeUnit.toMillis(duration);
	}

	public long getMillis() {
		return millis();
	}

	public long seconds() {
		return timeUnit.toSeconds(duration);
	}

	public long getSeconds() {
		return seconds();
	}

	public long minutes() {
		return timeUnit.toMinutes(duration);
	}

	public long getMinutes() {
		return minutes();
	}

	public long hours() {
		return timeUnit.toHours(duration);
	}

	public long getHours() {
		return hours();
	}

	public long days() {
		return timeUnit.toDays(duration);
	}

	public long getDays() {
		return days();
	}

	public double microsFrac() {
		return ((double) nanos()) / C1;
	}

	public double getMicrosFrac() {
		return microsFrac();
	}

	public double millisFrac() {
		return ((double) nanos()) / C2;
	}

	public double getMillisFrac() {
		return millisFrac();
	}

	public double secondsFrac() {
		return ((double) nanos()) / C3;
	}

	public double getSecondsFrac() {
		return secondsFrac();
	}

	public double minutesFrac() {
		return ((double) nanos()) / C4;
	}

	public double getMinutesFrac() {
		return minutesFrac();
	}

	public double hoursFrac() {
		return ((double) nanos()) / C5;
	}

	public double getHoursFrac() {
		return hoursFrac();
	}

	public double daysFrac() {
		return ((double) nanos()) / C6;
	}

	public double getDaysFrac() {
		return daysFrac();
	}

	private final PeriodFormatter defaultFormatter = PeriodFormat.getDefault().withParseType(PeriodType.standard());

	public String format() {
		Period period = new Period(millis());
		return defaultFormatter.print(period);
	}

	public String format(PeriodType type) {
		Period period = new Period(millis());
		return PeriodFormat.getDefault().withParseType(type).print(period);
	}

	@Override
	public String toString() {
		if (duration < 0) {
			return Long.toString(duration);
		}
		long nanos = nanos();
		if (nanos == 0) {
			return "0s";
		}
		double value = nanos;
		String suffix = "nanos";
		if (nanos >= C6) {
			value = daysFrac();
			suffix = "d";
		} else if (nanos >= C5) {
			value = hoursFrac();
			suffix = "h";
		} else if (nanos >= C4) {
			value = minutesFrac();
			suffix = "m";
		} else if (nanos >= C3) {
			value = secondsFrac();
			suffix = "s";
		} else if (nanos >= C2) {
			value = millisFrac();
			suffix = "ms";
		} else if (nanos >= C1) {
			value = microsFrac();
			suffix = "micros";
		}
		return ESStrings.format1Decimals(value, suffix);
	}

	static final long C0 = 1L;
	static final long C1 = C0 * 1000L;
	static final long C2 = C1 * 1000L;
	static final long C3 = C2 * 1000L;
	static final long C4 = C3 * 60L;
	static final long C5 = C4 * 60L;
	static final long C6 = C5 * 24L;

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		ESTimeValue timeValue = (ESTimeValue) o;
		return timeUnit.toNanos(duration) == timeValue.timeUnit.toNanos(timeValue.duration);
	}

	@Override
	public int hashCode() {
		long normalized = timeUnit.toNanos(duration);
		return (int) (normalized ^ (normalized >>> 32));
	}

	public static long nsecToMSec(long ns) {
		return ns / NSEC_PER_MSEC;
	}
}
