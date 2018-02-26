package io.spacedog.test;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.spacedog.model.DataObject;
import io.spacedog.model.DataObjectAbstract;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageDataObject extends DataObjectAbstract<Message> {

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
	public DataObject<Message> source(Message source) {
		this.source = source;
		return this;
	}

	@Override
	public String type() {
		return Message.TYPE;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Results {
		public long total;
		public List<MessageDataObject> results;
	}

}