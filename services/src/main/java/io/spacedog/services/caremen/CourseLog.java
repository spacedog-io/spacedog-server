package io.spacedog.services.caremen;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.spacedog.model.GeoPoint;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class CourseLog {

	public String courseId;
	public String driverId;
	public String status;
	public GeoPoint where;
	public DateTime when;
	public long index;
	public long distanceFromLastLog;
}