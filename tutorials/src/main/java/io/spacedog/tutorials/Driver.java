package io.spacedog.tutorials;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.spacedog.client.data.DataObjectBase;
import io.spacedog.client.schema.GeoPoint;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class Driver extends DataObjectBase {

	public String status;
	public String firstname;
	public String lastname;
	public String phone;
	public String photo;
	public LastLocation lastLocation;
	public Vehicule vehicule;

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class LastLocation {
		public GeoPoint where;
		public DateTime when;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Vehicule {
		public String type;
		public String brand;
		public String model;
		public String color;
		public String licencePlate;
	}
}
