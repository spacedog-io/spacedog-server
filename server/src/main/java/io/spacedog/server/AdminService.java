/**
 * © David Attias 2015
 */
package io.spacedog.server;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import net.codestory.http.Context;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.payload.Payload;

public class AdminService extends SpaceService {

	//
	// Routes
	//

	@Get("")
	@Get("/")
	public Payload getPing() {
		ObjectNode payload = (ObjectNode) Json.toJsonNode(Server.get().info());
		return JsonPayload.ok().withContent(payload).build();
	}

	@Get("/1/admin/return500")
	@Get("/1/admin/return500")
	public Payload getLog() {
		throw Exceptions.runtime("this route always returns http code 500");
	}

	@Post("/1/admin/_clear")
	@Post("/1/admin/_clear")
	public void postClear(Context context) {
		SpaceContext.credentials().checkSuperDog();

		if (!SpaceContext.backend().host().endsWith("lvh.me"))
			throw Exceptions.forbidden("only allowed for [*.lvh.me] env");

		Server.get().clear(context.query().getBoolean("files", false));
	}

	//
	// Singleton
	//

	private static AdminService singleton = new AdminService();

	public static AdminService get() {
		return singleton;
	}

	private AdminService() {
	}

}