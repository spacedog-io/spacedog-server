package io.spacedog.services;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.utils.Json;

public class Debug {

	private boolean debug = false;
	private int batchCredentialChecks = 0;

	public Debug(boolean debug) {
		this.debug = debug;
	}

	public boolean isTrue() {
		return debug;
	}

	public void credentialCheck() {
		batchCredentialChecks++;
	}

	public ObjectNode toNode() {
		return Json.object("batchCredentialChecks", batchCredentialChecks);
	}
}
