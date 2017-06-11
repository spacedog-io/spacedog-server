package io.spacedog.services.caremen;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.spacedog.model.Settings;

@JsonIgnoreProperties(ignoreUnknown = true)
class AppConfigurationSettings extends Settings {
	public String operatorPhoneNumber;
	public int newCourseRequestDriverPushRadiusInMeters;
}