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
import io.spacedog.client.schema.GeoPoint;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class Driver extends MetadataBase {

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

	public static class DriverDataObject extends DataObjectAbstract<Driver> {

		private Driver source;

		@Override
		public Class<Driver> sourceClass() {
			return Driver.class;
		}

		@Override
		public Driver source() {
			return source;
		}

		@Override
		public DataObject<Driver> source(Driver source) {
			this.source = source;
			return this;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Results {

		public long total;
		public List<DriverDataObject> results;
		public ObjectNode aggregations;
	}

}
