/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Optional;
import java.util.UUID;

import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.payload.Payload;

@Prefix("/v1/share")
public class ShareResource extends S3Resource {

	private static final String SHARE_BUCKET_SUFFIX = "shared";

	//
	// Routes
	//

	@Get("")
	@Get("/")
	public Object getAll(Context context) {
		Credentials credentials = SpaceContext.checkAdminCredentials();
		return doGet(SHARE_BUCKET_SUFFIX, credentials.backendId(), Optional.empty(), context);
	}

	@Get("/:uuid/:fileName")
	@Get("/:uuid/:fileName/")
	public Object get(String uuid, String fileName, Context context) {
		// TODO better check ACL
		Credentials credentials = SpaceContext.checkCredentials();
		return doGet(SHARE_BUCKET_SUFFIX, credentials.backendId(), Optional.of(String.join(SLASH, uuid, fileName)),
				context);
	}

	@Put("/:fileName")
	@Put("/:fileName/")
	public Payload put(String fileName, byte[] bytes, Context context) {
		Credentials credentials = SpaceContext.checkUserCredentials();
		String uuid = UUID.randomUUID().toString();
		return doUpload(SHARE_BUCKET_SUFFIX, "/v1/share", credentials, uuid, fileName, bytes, context);
	}

	@Delete("")
	@Delete("/")
	public Payload deleteAll() {
		Credentials credentials = SpaceContext.checkAdminCredentials();
		return doDelete(SHARE_BUCKET_SUFFIX, credentials, Optional.empty());
	}

	@Delete("/:uuid/:fileName")
	@Delete("/:uuid/:fileName/")
	public Payload delete(String uuid, String fileName, Context context) {
		Credentials credentials = SpaceContext.checkUserOrAdminCredentials();
		return doDelete(SHARE_BUCKET_SUFFIX, credentials, Optional.of(String.join(SLASH, uuid, fileName)));
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
