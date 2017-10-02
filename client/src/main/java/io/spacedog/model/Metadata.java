package io.spacedog.model;

import org.joda.time.DateTime;

public interface Metadata {

	public String owner();

	public void owner(String owner);

	public String group();

	public void group(String group);

	public DateTime createdAt();

	public void createdAt(DateTime createdAt);

	public DateTime updatedAt();

	public void updatedAt(DateTime updatedAt);
}