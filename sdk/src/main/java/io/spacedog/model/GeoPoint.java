/**
 * © David Attias 2015
 */
package io.spacedog.model;

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
}
