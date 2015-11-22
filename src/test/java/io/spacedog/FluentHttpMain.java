/**
 * © David Attias 2015
 */
package io.spacedog;

import net.codestory.http.WebServer;
import net.codestory.http.routes.Routes;

public class FluentHttpMain {

	private static void configure(Routes routes) {
		routes.any((context -> "Hello"));
	}

	public static void main(String[] args) {
		try {
			// Force Fluent HTTP to production mode
			System.setProperty("PROD_MOD", "true");

			new WebServer().configure(FluentHttpMain::configure).start();

		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(-1);
		}
	}
}
