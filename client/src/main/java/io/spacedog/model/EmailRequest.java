package io.spacedog.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, //
		include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({ //
		@Type(value = EmailBasicRequest.class, name = "basic"), //
		@Type(value = EmailTemplateRequest.class, name = "template")//
})
public abstract class EmailRequest {
}
