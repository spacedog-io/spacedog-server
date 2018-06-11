package io.spacedog.client.job;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, //
		include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({ //
		@Type(value = BasicSpaceJob.class, name = "basic"), //
		@Type(value = LambdaSpaceJob.class, name = "lambda")//
})
public abstract class SpaceJob {
	String when;
	Object payload;
}