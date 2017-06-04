package io.spacedog.services;

import net.codestory.http.Context;
import net.codestory.http.annotations.Post;
import net.codestory.http.payload.Payload;

public class CaremenResource2 extends Resource {

	//
	// Routes
	//

	@Post("/1/service/course/:id/_accept")
	@Post("/1/service/course/:id/_accept")
	public Payload postAccept(String id, String body, Context context) {

		// driver
		// body contains driverId
		return null;
	}

	@Post("/1/service/course/:id/_ready_to_load")
	@Post("/1/service/course/:id/_ready_to_load")
	public Payload postReadyToLoad(String id) {

		// driver
		return null;
	}

	@Post("/1/service/course/:id/_in_progress")
	@Post("/1/service/course/:id/_in_progress")
	public Payload postInProgress(String id) {

		// driver
		return null;
	}

	@Post("/1/service/course/:id/_completed")
	@Post("/1/service/course/:id/_completed")
	public Payload postCompleted(String id, String body) {

		// driver
		// body contains array de courselog
		return null;
	}

	@Post("/1/service/course/:id/_cancel")
	@Post("/1/service/course/:id/_cancel")
	public Payload postCancel(String id) {

		// client
		return null;
	}

	//
	// singleton
	//

	private static CaremenResource2 singleton = new CaremenResource2();

	static CaremenResource2 get() {
		return singleton;
	}

	private CaremenResource2() {
	}
}
