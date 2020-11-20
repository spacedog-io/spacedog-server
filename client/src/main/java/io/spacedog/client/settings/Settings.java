package io.spacedog.client.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface Settings {

	@JsonIgnore
	public String id();

	@JsonIgnore
	public String version();

	public void version(String version);
}
