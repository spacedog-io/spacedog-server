package io.spacedog.services.caremen;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Vehicule {
	public String type;
	public String brand;
	public String model;
	public String color;
	public String licencePlate;
}