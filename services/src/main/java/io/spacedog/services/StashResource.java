package io.spacedog.services;

import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.payload.Payload;

@Prefix("/1/stash")
@Deprecated
public class StashResource {

	//
	// Routes
	//

	@Get("/:id")
	@Get("/:id/")
	@Deprecated
	public Payload getById(String id) {
		return SettingsResource.get().get(id);
	}

	//
	// singleton
	//

	private static StashResource singleton = new StashResource();

	static StashResource get() {
		return singleton;
	}

	private StashResource() {
	}

}
