package io.spacedog.caremen;

import java.util.List;
import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Lists;

import io.spacedog.sdk.DataObject;

//ignore unknown fields
@JsonIgnoreProperties(ignoreUnknown = true)
// only map to private fields
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
getterVisibility = Visibility.NONE, //
isGetterVisibility = Visibility.NONE, //
setterVisibility = Visibility.NONE)
public class DriverRecap extends DataObject {

	private DateTimeFormatter monthFormatter = DateTimeFormat.//
			forPattern("MMMM yyyy").withLocale(Locale.FRANCE);

	public String driverId;
	public String credentialsId;
	public String month;
	public String firstname;
	public String lastname;
	public String homeAddress;
	public RIB rib;
	public List<CourseRecap> courses;
	public double gain;
	public String gainLocalized;

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class CourseRecap {
		public DateTime dropoffTimestamp;
		public String to;
		public long time;
		public long distance;
		public double gain;
	}

	public DriverRecap() {
		month = monthFormatter.print(DateTime.now().minusMonths(1));
	}

	public void setDriver(Driver driver) {
		driverId = driver.id();
		credentialsId = driver.credentialsId;
		firstname = driver.firstname;
		lastname = driver.lastname;
		homeAddress = driver.homeAddress;
		rib = driver.rib;
	}

	public void addCourse(Course course) {

		CourseRecap courseRecap = new CourseRecap();
		courseRecap.dropoffTimestamp = course.dropoffTimestamp;
		courseRecap.to = course.to.address;
		courseRecap.time = course.time;
		courseRecap.distance = course.distance;
		courseRecap.gain = course.driver.gain;

		if (courses == null)
			courses = Lists.newArrayList();

		courses.add(courseRecap);

		gain = gain + courseRecap.gain;
	}

}
