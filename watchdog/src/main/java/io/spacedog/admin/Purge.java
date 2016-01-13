package io.spacedog.admin;

import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Throwables;

import io.spacedog.admin.Cloud.SuperdogConfiguration;
import io.spacedog.client.SpaceRequest;

public class Purge {

	public void lambda() {

		try {
			SuperdogConfiguration configuration = Cloud.get().getSuperdogConfiguration();

			SpaceRequest.setLogDebug(configuration.debug());
			SpaceRequest.setTarget(configuration.target());

			int from = 0;
			int size = 1;
			int total = 101;

			while (from + size < total) {
				ObjectNode accounts;
				accounts = SpaceRequest.get("/v1/admin/account")//
						.queryString("from", String.valueOf(from))//
						.queryString("size", String.valueOf(size))//
						.basicAuth(configuration.username(), configuration.password())//
						.go(200)//
						.objectNode();

				total = accounts.get("total").asInt();
				from = from + size;

				Iterator<JsonNode> elements = accounts.get("results").elements();
				while (elements.hasNext()) {
					String backendId = elements.next().get("backendId").asText();
					SpaceRequest.delete("/v1/admin/log/" + backendId)//
							.basicAuth(configuration.username(), configuration.password())//
							.go(200);
				}
			}

			String message = SpaceRequest.getTargetHost() + " log purge OK";
			Cloud.get().sendNotification(message, message);

		} catch (Exception e) {
			Cloud.get().sendNotification(//
					SpaceRequest.getTargetHost() + " log purge ERROR", //
					Throwables.getStackTraceAsString(e));
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		new Purge().lambda();
	}
}
