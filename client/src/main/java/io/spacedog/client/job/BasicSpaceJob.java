package io.spacedog.client.job;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.spacedog.client.email.EmailBasicRequest;
import io.spacedog.client.email.EmailTemplateRequest;
import io.spacedog.utils.KeyValue;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, //
		include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({ //
		@Type(value = EmailBasicRequest.class, name = "basic"), //
		@Type(value = EmailTemplateRequest.class, name = "template")//
})
public class BasicSpaceJob {
	String method;
	String path;
	String when;
	Set<KeyValue> headers;
	Set<KeyValue> params;
	Object payload;
}