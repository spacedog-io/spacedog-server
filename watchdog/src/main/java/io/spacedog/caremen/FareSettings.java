package io.spacedog.caremen;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Settings;

public class FareSettings extends Settings {

	public static class VehiculeFareSettings {
		public int base;
		public int minimum;
		public double km;
		public double min;
	}

	public VehiculeFareSettings classic;
	public VehiculeFareSettings premium;
	public VehiculeFareSettings green;
	@JsonProperty("break")
	public VehiculeFareSettings breakk;
	public VehiculeFareSettings van;
	public double driverShare = 0.82f;

	public VehiculeFareSettings getFor(String type) {
		if ("classic".equals(type))
			return classic;
		if ("premium".equals(type))
			return premium;
		if ("green".equals(type))
			return green;
		if ("break".equals(type))
			return breakk;
		if ("van".equals(type))
			return van;

		throw Exceptions.illegalArgument("invalid vehicule type [%s]", type);
	}
}
