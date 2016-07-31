/**
 * © David Attias 2015
 */
package io.spacedog.services;

import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;

public class UserResource extends Resource {

	//
	// User constants
	//

	public static final String TYPE = "user";

	//
	// Routes
	//

	@Get("/1/user")
	@Get("/1/user/")
	public Payload getAll(Context context) {
		return DataResource.get().getByType(TYPE, context);
	}

	@Post("/1/user")
	@Post("/1/user/")
	public Payload post(String body, Context context) {
		return DataResource.get().post(TYPE, body, context);
	}

	@Get("/1/user/:username")
	@Get("/1/user/:username/")
	public Payload get(String username, Context context) {
		return DataResource.get().getById(TYPE, username, context);
	}

	@Put("/1/user/:username")
	@Put("/1/user/:username/")
	public Payload put(String username, String jsonBody, Context context) {
		return DataResource.get().put(TYPE, username, jsonBody, context);
	}

	@Delete("/1/user/:username")
	@Delete("/1/user/:username/")
	public Payload delete(String username, Context context) {
		return DataResource.get().deleteById(TYPE, username, context);
	}

	@Delete("/1/user/:username/password")
	@Delete("/1/user/:username/password/")
	public Payload deletePassword(String username, Context context) {
		return CredentialsResource.get().deletePassword(username, context);
	}

	@Post("/1/user/:username/password")
	@Post("/1/user/:username/password/")
	public Payload postPassword(String username, Context context) {
		return CredentialsResource.get().postPassword(username, context);
	}

	@Put("/1/user/:username/password")
	@Put("/1/user/:username/password/")
	public Payload putPassword(String username, Context context) {
		return CredentialsResource.get().putPassword(username, context);
	}

	//
	// singleton
	//

	private static UserResource singleton = new UserResource();

	static UserResource get() {
		return singleton;
	}

	private UserResource() {
	}
}
