package io.spacedog.model;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class MetadataBase implements Metadata {

	private String owner;
	private String group;
	private DateTime createdAt;
	private DateTime updatedAt;

	public MetadataBase() {
	}

	public String owner() {
		return owner;
	}

	public void owner(String owner) {
		this.owner = owner;
	}

	public String group() {
		return group;
	}

	public void group(String group) {
		this.group = group;
	}

	public DateTime createdAt() {
		return createdAt;
	}

	public void createdAt(DateTime createdAt) {
		this.createdAt = createdAt;
	}

	public DateTime updatedAt() {
		return updatedAt;
	}

	public void updatedAt(DateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

}