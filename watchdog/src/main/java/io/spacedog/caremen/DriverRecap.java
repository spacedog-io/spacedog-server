package io.spacedog.caremen;

import java.util.List;
import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Lists;

//ignore unknown fields
@JsonIgnoreProperties(ignoreUnknown = true)
// only map to private fields
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
getterVisibility = Visibility.NONE, //
isGetterVisibility = Visibility.NONE, //
setterVisibility = Visibility.NONE)
public class DriverRecap {

	@JsonIgnore
	private static DateTimeFormatter shortDateFormatter = DateTimeFormat.//
			forPattern("dd/MM").withLocale(Locale.FRANCE);

	@JsonIgnore
	private static DateTimeFormatter longDateFormatter = DateTimeFormat.//
			forPattern("dd/MM/yy' à 'HH'h'mm").withLocale(Locale.FRANCE)//
			.withZone(DateTimeZone.forID("Europe/Paris"));

	public String driverId;
	public String credentialsId;
	public String monday;
	public String sunday;
	public String companyName;
	public String firstname;
	public String lastname;
	public String siret;
	public String address;
	public List<CourseRecap> courses;
	public double totalRevenu;
	public double totalGain;
	public double caremenShare;
	public double serviceFees;
	public double otherFees;
	public String gainLocalized;

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class CourseRecap {
		public String dropoffTimestamp;
		public String to;
		public String from;
		public double fare;
		public double gain;
	}

	public DriverRecap() {
	}

	public void setDriver(Driver driver) {
		driverId = driver.id();
		credentialsId = driver.credentialsId;
		companyName = driver.companyName;
		siret = driver.siret;
		firstname = driver.firstname;
		lastname = driver.lastname;
		address = driver.homeAddress;
	}

	public void addCourse(Course course) {

		CourseRecap courseRecap = new CourseRecap();
		courseRecap.dropoffTimestamp = longDateFormatter.print(course.dropoffTimestamp);
		courseRecap.to = course.to.address;
		courseRecap.from = course.from.address;
		courseRecap.fare = course.fare;
		courseRecap.gain = course.driver.gain;

		if (courses == null)
			courses = Lists.newArrayList();

		courses.add(courseRecap);

		totalRevenu += course.fare;
		totalGain += course.driver.gain;
	}

	public void shakeIt() {
		serviceFees = totalRevenu - totalGain;
		DateTime lastWeek = DateTime.now().minusWeeks(1);
		monday = shortDateFormatter.print(lastWeek.withDayOfWeek(1));
		sunday = shortDateFormatter.print(lastWeek.withDayOfWeek(7));
	}

}
