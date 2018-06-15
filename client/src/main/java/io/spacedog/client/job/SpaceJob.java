package io.spacedog.client.job;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.spacedog.client.settings.Settings;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, //
		include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({ //
		@Type(value = BasicSpaceJob.class, name = "basic"), //
		@Type(value = LambdaSpaceJob.class, name = "lambda")//
})
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, //
		getterVisibility = Visibility.NONE, //
		isGetterVisibility = Visibility.NONE, //
		setterVisibility = Visibility.NONE)
public abstract class SpaceJob implements Settings {

	public String name;
	public int retries = 0;
	public String when;
	public Object payload;

	@JsonIgnore
	public long version;

	@Override
	public String id() {
		return internalSettingsId(name);
	}

	@Override
	public long version() {
		return version;
	}

	@Override
	public void version(long version) {
		this.version = version;
	}

	public static String internalSettingsId(String name) {
		return "internal-job-" + name;
	}
}