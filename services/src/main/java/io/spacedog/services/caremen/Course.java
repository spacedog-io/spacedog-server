package io.spacedog.services.caremen;

import org.elasticsearch.common.Strings;
import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.spacedog.model.GeoPoint;
import io.spacedog.services.DataStore.Meta;
import io.spacedog.services.DataStore.Metable;
import io.spacedog.utils.Exceptions;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Course implements Metable {

	public final static String TYPE = "course";

	public Meta meta;
	public String status;
	public String requestedVehiculeType;
	public DateTime requestedPickupTimestamp;
	public DateTime driverIsComingTimestamp;
	public DateTime driverIsReadyToLoadTimestamp;
	public DateTime cancelledTimestamp;
	public DateTime pickupTimestamp;
	public DateTime dropoffTimestamp;
	public String noteForDriver;
	public Float fare;
	public Long time;
	public Integer distance;

	public Course.CourseCustomer customer;
	public Payment payment;
	public Course.Location from;
	public Course.Location to;
	public Course.CourseDriver driver;

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Location {
		public String address;
		public String flatRateCode;
		public GeoPoint geopoint;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class DriverCompany {
		public String name;
		public String address;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class CourseDriver {
		public String driverId;
		public String credentialsId;
		public float gain;
		public String firstname;
		public String lastname;
		public String phone;
		public String photo;
		public Vehicule vehicule;
		public DriverCompany company;

		public CourseDriver() {
		}

		public CourseDriver(Driver driver) {
			driverId = driver.id;
			credentialsId = driver.credentialsId;
			firstname = driver.firstname;
			lastname = driver.lastname;
			phone = driver.phone;
			photo = driver.photo;
			vehicule = driver.vehicule;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class CourseCustomer {
		public String id;
		public String credentialsId;
		public String firstname;
		public String lastname;
		public String phone;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Payment {
		public String companyId;
		public String companyName;
		public Stripe stripe;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Stripe {
		public String customerId;
		public String cardId;
		public Stripe paymentId;
	}

	public void check(String... validStatuses) {

		if (customer == null || customer.credentialsId == null)
			throw Exceptions.illegalState("course has invalid customer data");

		if (Strings.isNullOrEmpty(status))
			throw Exceptions.illegalState("course has no status");

		for (String validStatus : validStatuses)
			if (validStatus.equals(status))
				return;

		throw Exceptions.illegalState("incompatible course status [%s]", status);
	}

	public void checkDriver(String driverCredentialsId) {
		if (driver == null //
				|| !driverCredentialsId.equals(driver.credentialsId))
			throw Exceptions.forbidden(//
					"your are not the driver of course [%s]", meta.id);
	}

	@Override
	public Meta meta() {
		return meta;
	}

	@Override
	public void meta(Meta meta) {
		this.meta = meta;
	}
}