package io.spacedog.test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.spacedog.model.MetadataBase;
import io.spacedog.model.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Message extends MetadataBase {
	public final static String TYPE = "message";

	public String text;

	public Message() {
	}

	public Message(String text) {
		this.text = text;
	}

	public static Schema schema() {
		return Schema.builder(TYPE).text("text").build();
	}
}