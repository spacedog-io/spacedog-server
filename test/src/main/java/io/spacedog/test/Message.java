package io.spacedog.test;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.spacedog.client.data.DataObjectBase;
import io.spacedog.client.data.DataWrap;
import io.spacedog.client.data.DataWrapAbstract;
import io.spacedog.client.schema.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Message extends DataObjectBase {

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

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Wrap extends DataWrapAbstract<Message> {

		private Message source;

		@Override
		public Class<Message> sourceClass() {
			return Message.class;
		}

		@Override
		public Message source() {
			return this.source;
		}

		@Override
		public DataWrap<Message> source(Message source) {
			this.source = source;
			return this;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Results {
		public long total;
		public List<Wrap> results;
	}
}