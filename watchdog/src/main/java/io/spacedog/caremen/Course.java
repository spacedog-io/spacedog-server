package io.spacedog.caremen;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.spacedog.sdk.DataObject;

//ignore unknown fields
@JsonIgnoreProperties(ignoreUnknown = true)
// only map to private fields
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
getterVisibility = Visibility.NONE, //
isGetterVisibility = Visibility.NONE, //
setterVisibility = Visibility.NONE)
public class Course extends DataObject {

	public String status;
	public String requestedVehiculeType;
	public DateTime requestedPickupTimestamp;
	public DateTime pickupTimestamp;
	public DateTime dropoffTimestamp;
	public To to;
	public Double fare;
	public Long time; // in millis
	public Long distance; // in meters
	public Customer customer;
	public Payment payment;
	public Driver driver;

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class To {
		public String address;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Customer {
		public String id;
		public String credentialsId;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Payment {
		public String companyId;
		public Stripe stripe;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Stripe {
		public String customerId;
		public String cardId;
		public String paymentId;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Driver {
		public String driverId;
		public Double gain;
		public Vehicule vehicule;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Vehicule {
		public String type;
	}
}
