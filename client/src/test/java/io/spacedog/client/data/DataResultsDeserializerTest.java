package io.spacedog.client.data;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;

import io.spacedog.utils.Json;

public class DataResultsDeserializerTest extends Assert {

	public static class Message {
		public String text;

		public Message() {
		}

		public Message(String message) {
			this.text = message;
		}

		@Override
		public boolean equals(Object obj) {
			return text.equals(((Message) obj).text);
		}

		@Override
		public String toString() {
			return text;
		}
	}

	@Test
	public void shouldDeserialize() {

		DataResults<Message> original = DataResults.of(Message.class);

		original.total = 25;
		original.next = "NEXT";
		original.objects = Lists.newArrayList(//
				DataWrap.wrap(new Message("hi")).id("1").type("message").version("2:3"), //
				DataWrap.wrap(new Message("hello")).id("2").type("message").version("3:4"));

		DataResults<Message> copy = DataResults.of(Message.class);
		copy = Json.updatePojo(Json.toString(original), copy);

		assertResultsEquals(original, copy);
	}

	private void assertResultsEquals(DataResults<Message> original, DataResults<Message> copy) {
		assertEquals(original.total, copy.total);
		assertEquals(original.next, copy.next);
		assertEquals(original.objects, copy.objects);
	}

}
