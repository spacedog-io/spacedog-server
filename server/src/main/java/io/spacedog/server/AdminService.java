/**
 * © David Attias 2015
 */
package io.spacedog.server;

import io.spacedog.utils.Exceptions;
import net.codestory.http.annotations.Get;
import net.codestory.http.payload.Payload;

public class AdminService extends SpaceService {

	//
	// Routes
	//

	@Get("/1/admin/return500")
	@Get("/1/admin/return500")
	public Payload getLog() {
		throw Exceptions.runtime("this route always returns http code 500");
	}

	//
	// Singleton
	//

	private static AdminService singleton = new AdminService();

	static AdminService get() {
		return singleton;
	}

	private AdminService() {
	}
}