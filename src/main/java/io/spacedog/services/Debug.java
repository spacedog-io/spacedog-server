package io.spacedog.services;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class Debug {

	private static ThreadLocal<Integer> batchDebug;

	static {
		batchDebug = new ThreadLocal<>();
		resetBatchDebug();
	}

	public static void resetBatchDebug() {
		batchDebug.set(Integer.valueOf(0));
	}

	public static void credentialCheck() {
		Integer value = batchDebug.get();
		batchDebug.set(value == null ? 1 : Integer.valueOf(value + 1));
	}

	public static int batchCrendentialChecks() {
		return batchDebug.get();
	}

	public static ObjectNode buildDebugObjectNode() {
		return Json.objectBuilder().put("batchCredentialChecks", batchCrendentialChecks()).build();
	}

}
