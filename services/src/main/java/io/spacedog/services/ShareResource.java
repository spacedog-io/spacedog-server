/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;

@Prefix("/v1/share")
public class ShareResource extends AbstractS3Resource {

	private static final String SHARE_BUCKET = "spacedog-shared";

	//
	// Routes
	//

	@Get("")
	@Get("/")
	public Object getAll(Context context) //
			throws JsonParseException, JsonMappingException, IOException {

		Credentials credentials = SpaceContext.checkAdminCredentials();
		return doGet(SHARE_BUCKET, credentials.backendId(), Optional.empty(), context);
	}

	@Get("/:uuid/:fileName")
	@Get("/:uuid/:fileName/")
	public Object get(String uuid, String fileName, Context context) //
			throws JsonParseException, JsonMappingException, IOException {

		// TODO better check ACL
		Credentials credentials = SpaceContext.checkCredentials();
		return doGet(SHARE_BUCKET, credentials.backendId(), Optional.of(String.join(SLASH, uuid, fileName)), context);
	}

	@Put("/:fileName")
	@Put("/:fileName/")
	public Payload put(String fileName, byte[] bytes, Context context) //
			throws JsonParseException, JsonMappingException, IOException {

		Credentials credentials = SpaceContext.checkUserCredentials();
		String uuid = UUID.randomUUID().toString();
		return doUpload(SHARE_BUCKET, "/v1/share", credentials, uuid, fileName, bytes, context);
	}

	@Delete("")
	@Delete("/")
	public Payload deleteAll() throws JsonParseException, JsonMappingException, IOException {

		Credentials credentials = SpaceContext.checkAdminCredentials();
		return doDelete(SHARE_BUCKET, credentials, Optional.empty());
	}

	@Delete("/:uuid/:fileName")
	@Delete("/:uuid/:fileName/")
	public Payload delete(String uuid, String fileName, Context context)
			throws JsonParseException, JsonMappingException, IOException {

		Credentials credentials = SpaceContext.checkUserOrAdminCredentials();
		return doDelete(SHARE_BUCKET, credentials, Optional.of(String.join(SLASH, uuid, fileName)));
	}

	//
	// singleton
	//

	private static ShareResource singleton = new ShareResource();

	static ShareResource get() {
		return singleton;
	}

	private ShareResource() {
	}
}
