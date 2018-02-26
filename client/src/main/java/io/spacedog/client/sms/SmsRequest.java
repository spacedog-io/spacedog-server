package io.spacedog.client.sms;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, //
		include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({ //
		@Type(value = SmsBasicRequest.class, name = "basic"), //
		@Type(value = SmsTemplateRequest.class, name = "template")//
})
public abstract class SmsRequest {

}
