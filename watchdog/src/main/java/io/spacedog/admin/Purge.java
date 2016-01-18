package io.spacedog.admin;

import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Throwables;

import io.spacedog.client.SpaceRequest;
import io.spacedog.client.SpaceRequestConfiguration;
import io.spacedog.utils.Internals;

public class Purge {

	public void run() {

		try {
			int from = 0;
			int size = 1;
			int total = 101;

			while (from + size < total) {
				ObjectNode accounts;
				accounts = SpaceRequest.get("/v1/account")//
						.queryString("from", String.valueOf(from))//
						.queryString("size", String.valueOf(size))//
						.superdogAuth()//
						.go(200)//
						.objectNode();

				total = accounts.get("total").asInt();
				from = from + size;

				Iterator<JsonNode> elements = accounts.get("results").elements();
				while (elements.hasNext()) {
					String backendId = elements.next().get("backendId").asText();
					SpaceRequest.delete("/v1/log/" + backendId)//
							.superdogAuth()//
							.go(200);
				}
			}

			String message = SpaceRequestConfiguration.get().target().host() + " log purge OK";
			Internals.get().notify(//
					SpaceRequestConfiguration.get().superdogNotificationTopic(), //
					message, message);

		} catch (Exception e) {

			e.printStackTrace();

			Internals.get().notify(//
					SpaceRequestConfiguration.get().superdogNotificationTopic(), //
					SpaceRequestConfiguration.get().target().host() + " log purge ERROR", //
					Throwables.getStackTraceAsString(e));
		}

	}

	public static void main(String[] args) {
		new Purge().run();
	}
}
