package io.spacedog.client.data;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.utils.Json;

public class DataWrapDeserializerTest extends Assert {

	private static class Message {
		public boolean isPublic;
		public String text;
		public long priority;
	}

	@Test
	public void shouldDeserialize() {

		Message message = new Message();
		message.text = "hi";
		message.priority = 2;
		message.isPublic = true;

		DataWrap<Message> original = DataWrap.wrap(message)//
				.id("123").type("message").version(2).score(2.3f)//
				.sort(new Object[] { 1.2d, "toto" });

		DataWrap<Message> copy = DataWrap.wrap(new Message());
		copy = Json.updatePojo(Json.toString(original), copy);

		assertWrapEquals(original, copy);
	}

	@Test
	public void shouldDeserializeEmptyObject() {

		DataWrap<Message> original = DataWrap.wrap(new Message());

		DataWrap<Message> copy = DataWrap.wrap(new Message());
		copy = Json.updatePojo(Json.toString(original), copy);

		assertWrapEquals(original, copy);
	}

	@Test
	public void shouldDeserializeWithUniqueSortValue() {

		DataWrap<Message> original = DataWrap.wrap(new Message());
		ObjectNode node = Json.toObjectNode(original);
		node.put("sort", 1.234d);

		DataWrap<Message> copy = DataWrap.wrap(new Message());
		copy = Json.updatePojo(node.toString(), copy);

		assertEquals(1.234d, (double) copy.sort()[0], 0.00001d);
	}

	private void assertWrapEquals(DataWrap<Message> original, DataWrap<Message> copy) {
		assertEquals(original.id(), copy.id());
		assertEquals(original.type(), copy.type());
		assertEquals(original.version(), copy.version());
		assertEquals(original.score(), copy.score(), 0.00001);
		assertArrayEquals(original.sort(), copy.sort());
		assertEquals(original.source().text, copy.source().text);
		assertEquals(original.source().priority, copy.source().priority);
		assertEquals(original.source().isPublic, copy.source().isPublic);
	}

	@Test
	public void shouldDeserializeToObjectNode() {

		Message message = new Message();
		message.text = "hi";
		message.priority = 2;
		message.isPublic = true;

		DataWrap<Message> original = DataWrap.wrap(message)//
				.id("123").type("message").version(2).score(2.3f)//
				.sort(new Object[] { 1.2d, "toto" });

		DataWrap<ObjectNode> copy = Json.updatePojo(Json.toString(original), DataWrap.wrap(ObjectNode.class));

		assertEquals(original.id(), copy.id());
		assertEquals(original.type(), copy.type());
		assertEquals(original.version(), copy.version());
		assertEquals(original.score(), copy.score(), 0.00001);
		assertArrayEquals(original.sort(), copy.sort());
		assertEquals(original.source().text, copy.source().get("text").asText());
		assertEquals(original.source().priority, copy.source().get("priority").asLong());
		assertEquals(original.source().isPublic, copy.source().get("isPublic").asBoolean());
	}

}
