package io.spacedog.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface Settings {

	long MATCH_ANY_VERSIONS = -3L;

	@JsonIgnore
	public String id();

	@JsonIgnore
	public long version();

	public void version(long version);
}
