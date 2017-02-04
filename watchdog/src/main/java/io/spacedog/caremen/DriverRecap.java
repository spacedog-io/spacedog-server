package io.spacedog.caremen;

import java.text.DecimalFormat;
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
	private static DateTimeFormatter dropoffFormatter = DateTimeFormat.//
			forPattern("dd/MM' à 'HH'h'mm").withLocale(Locale.FRANCE)//
			.withZone(DateTimeZone.forID("Europe/Paris"));

	@JsonIgnore
	private static DecimalFormat decimalFormatter = new DecimalFormat("#.##");

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
	public String totalRevenuLocalized;
	public String totalGainLocalized;
	public String caremenShare;
	public String serviceFees;
	public String otherFees = "0";
	public String gainLocalized;

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class CourseRecap {
		public String dropoffTimestamp;
		public String to;
		public String from;
		public String fare;
		public String gain;
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
		courseRecap.to = course.to.address;
		courseRecap.from = course.from.address;
		courseRecap.dropoffTimestamp = dropoffFormatter.print(course.dropoffTimestamp);
		courseRecap.fare = decimalFormatter.format(course.fare);
		courseRecap.gain = decimalFormatter.format(course.driver.gain);

		if (courses == null)
			courses = Lists.newArrayList();

		courses.add(courseRecap);

		totalRevenu += course.fare;
		totalGain += course.driver.gain;
	}

	public void shakeIt() {
		serviceFees = decimalFormatter.format(totalRevenu - totalGain);
		totalRevenuLocalized = decimalFormatter.format(totalRevenu);
		totalGainLocalized = decimalFormatter.format(totalGain);
		DateTime lastWeek = DateTime.now().minusWeeks(1);
		monday = shortDateFormatter.print(lastWeek.withDayOfWeek(1));
		sunday = shortDateFormatter.print(lastWeek.withDayOfWeek(7));
	}

	public void setCaremenShare(double driverShare) {
		this.caremenShare = Integer.toString(100 - (int) (driverShare * 100));
	}

}
