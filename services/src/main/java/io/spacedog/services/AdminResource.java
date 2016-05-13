/**
 * © David Attias 2015
 */
package io.spacedog.services;

import io.spacedog.utils.Exceptions;
import net.codestory.http.annotations.Get;
import net.codestory.http.payload.Payload;

public class AdminResource extends Resource {

	//
	// Routes
	//

	@Get("/v1/admin/login")
	@Get("/v1/admin/login/")
	@Get("/1/admin/login")
	@Get("/1/admin/login/")
	public Payload getLogin() {
		SpaceContext.checkAdminCredentials();
		return JsonPayload.success();
	}

	@Get("/1/admin/return500")
	@Get("/1/admin/return500")
	public Payload getLog() {
		throw Exceptions.runtime("this route always returns http code 500");
	}

	//
	// Singleton
	//

	private static AdminResource singleton = new AdminResource();

	static AdminResource get() {
		return singleton;
	}

	private AdminResource() {
	}
}
