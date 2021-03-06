/**
 * © David Attias 2015
 */
package io.spacedog.services.admin;

import io.spacedog.services.Server;
import io.spacedog.services.SpaceResty;
import io.spacedog.utils.Exceptions;
import net.codestory.http.Context;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.payload.Payload;

@Prefix("/2/admin")
public class AdminResty extends SpaceResty {

	@Post("/_return_500")
	@Post("/_return_500/")
	public Payload getLog() {
		throw Exceptions.runtime("this route always returns http code 500");
	}

	@Post("/_clear")
	@Post("/_clear/")
	public void postClear(Context context) {
		Server.context().credentials().checkSuperDog();
		Server.get().clear();
	}
}