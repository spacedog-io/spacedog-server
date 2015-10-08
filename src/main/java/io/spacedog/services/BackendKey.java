package io.spacedog.services;

import java.util.Date;
import java.util.UUID;

public class BackendKey {

	public static final String DEFAULT_BACKEND_KEY_NAME = "default";

	public String name;
	public String secret;
	public Date generatedAt;

	public BackendKey() {
		this(DEFAULT_BACKEND_KEY_NAME);
	}

	public BackendKey(String name) {
		this.name = name;
		this.secret = UUID.randomUUID().toString();
		this.generatedAt = new Date();
	}
}