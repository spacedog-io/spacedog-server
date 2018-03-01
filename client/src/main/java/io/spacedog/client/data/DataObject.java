package io.spacedog.client.data;

import org.joda.time.DateTime;

public interface DataObject {

	public String owner();

	public void owner(String owner);

	public String group();

	public void group(String group);

	public DateTime createdAt();

	public void createdAt(DateTime createdAt);

	public DateTime updatedAt();

	public void updatedAt(DateTime updatedAt);
}