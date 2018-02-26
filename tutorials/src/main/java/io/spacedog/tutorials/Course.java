package io.spacedog.tutorials;

import java.util.List;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.client.data.DataObject;
import io.spacedog.client.data.DataObjectAbstract;
import io.spacedog.client.data.MetadataBase;
import io.spacedog.model.GeoPoint;
import io.spacedog.utils.Exceptions;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class Course extends MetadataBase {

	public String status;
	public String requestedVehiculeType;
	public DateTime requestedPickupTimestamp;
	public DateTime driverIsComingTimestamp;
	public DateTime driverIsReadyToLoadTimestamp;
	public DateTime cancelledTimestamp;
	public DateTime pickupTimestamp;
	public DateTime dropoffTimestamp;
	public Location to;
	public Double fare;
	public Long time; // in millis
	public Long distance; // in meters
	public Customer customer;
	public Payment payment;
	public Driver driver;
	public Location from;

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Location {
		public String flatRateCode;
		public String address;
		public GeoPoint geopoint;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Customer {
		public String id;
		public String credentialsId;
		public String firstname;
		public String lastname;
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
		public String paymentId;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Driver {
		public String driverId;
		public String credentialsId;
		public String firstname;
		public String lastname;
		public Double gain;
		public Vehicule vehicule;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Vehicule {
		public String type;

		public enum VehiculeType {
			CLASSIC("classic", 0), //
			PREMIUM("premium", 1), //
			GREEN("green", 2), //
			BREAK("break", 3), //
			VAN("van", 4);

			private String type;
			private int level;

			VehiculeType(String type, int level) {
				this.type = type;
				this.level = level;
			}

			public int getLevel() {
				return this.level;
			}

			public String getType() {
				return this.type;
			}

			public static VehiculeType init(String type) {
				switch (type) {
				case "classic":
					return CLASSIC;
				case "premium":
					return PREMIUM;
				case "green":
					return GREEN;
				case "break":
					return BREAK;
				case "van":
					return VAN;
				default:
					throw Exceptions.illegalArgument("invalid vehicule type [%s]", type);
				}
			}

			public static VehiculeType getVehiculeTypeWithMinLevel(String type, String type2) {
				VehiculeType vType = VehiculeType.init(type);
				VehiculeType vType2 = VehiculeType.init(type2);
				return (vType.getLevel() < vType2.getLevel()) ? vType : vType2;
			}

		}
	}

	public static class CourseDataObject extends DataObjectAbstract<Course> {

		private Course source;

		@Override
		public Class<Course> sourceClass() {
			return Course.class;
		}

		@Override
		public Course source() {
			return source;
		}

		@Override
		public DataObject<Course> source(Course source) {
			this.source = source;
			return this;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Results {

		public long total;
		public List<CourseDataObject> results;
		public ObjectNode aggregations;
	}

}
