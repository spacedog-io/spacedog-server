package io.spacedog.client.job;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.spacedog.client.email.EmailBasicRequest;
import io.spacedog.client.email.EmailTemplateRequest;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, //
		include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({ //
		@Type(value = EmailBasicRequest.class, name = "basic"), //
		@Type(value = EmailTemplateRequest.class, name = "template")//
})
public class LambdaSpaceJob {
	String lambdaId;
	Object payload;
}