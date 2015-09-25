package io.spacedog.services;

import java.util.Date;
import java.util.UUID;

public class ApiKey {
	public String id;
	public String secret;
	public Date createdAt;

	public ApiKey(String id) {
		this.id = id;
		this.secret = UUID.randomUUID().toString();
		this.createdAt = new Date();
	}
}