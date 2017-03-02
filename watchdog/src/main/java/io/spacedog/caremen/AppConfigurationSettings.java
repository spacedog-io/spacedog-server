package io.spacedog.caremen;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.spacedog.utils.Settings;

//ignore unknown fields
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfigurationSettings extends Settings {
	public double rateVAT;
	public int scheduledCourseMinimumNoticeTimeInMinutes;
	public int driverAverageSpeedKmPerHour;
	public int courseLogIntervalMeters;
	public int customerWaitingForDriverMaxDurationMinutes;
	public int operatorRefreshTimeoutSeconds;
	public String backendVersion;
}
