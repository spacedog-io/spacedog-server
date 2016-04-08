/**
 * © David Attias 2015
 */
package io.spacedog.services;

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
		return Payloads.success();
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
