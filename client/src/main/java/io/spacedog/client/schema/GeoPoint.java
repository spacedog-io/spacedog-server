/**
 * © David Attias 2015
 */
package io.spacedog.client.schema;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class GeoPoint {
	public double lat;
	public double lon;

	public GeoPoint() {
	}

	public GeoPoint(double lat, double lon) {
		this.lat = lat;
		this.lon = lon;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj instanceof GeoPoint == false)
			return false;

		GeoPoint other = (GeoPoint) obj;
		return this.lat == other.lat//
				&& this.lon == other.lon;
	}

	@Override
	public String toString() {
		return "{" + lat + ", " + lon + "}";
	}

	public GeoPoint plus(double latDrift, double lonDrift) {
		return new GeoPoint(lat + latDrift, lon + lonDrift);
	}
}
