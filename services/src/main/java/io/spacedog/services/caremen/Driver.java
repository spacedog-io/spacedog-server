package io.spacedog.services.caremen;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.spacedog.model.GeoPoint;

@JsonIgnoreProperties(ignoreUnknown = true)
class Driver {
	@JsonIgnore
	public String id;
	public String status;
	public String credentialsId;
	public String firstname;
	public String lastname;
	public String homeAddress;
	public String companyName;
	public String siret;
	public String phone;
	public String photo;
	public Vehicule vehicule;
	public LastLocation lastLocation;

	@JsonIgnoreProperties(ignoreUnknown = true)
	public class LastLocation {
		public GeoPoint where;
		public DateTime when;
	}
}