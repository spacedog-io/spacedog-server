package io.spacedog.model;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public class Meta {
	private String createdBy;
	private DateTime createdAt;
	private String updatedBy;
	private DateTime updatedAt;

	public String createdBy() {
		return createdBy;
	}

	public Meta createdBy(String createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public DateTime createdAt() {
		return createdAt;
	}

	public Meta createdAt(DateTime createdAt) {
		this.createdAt = createdAt;
		return this;
	}

	public String updatedBy() {
		return updatedBy;
	}

	public Meta updatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
		return this;
	}

	public DateTime updatedAt() {
		return updatedAt;
	}

	public Meta updatedAt(DateTime updatedAt) {
		this.updatedAt = updatedAt;
		return this;
	}

}