/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.Optional;
import java.util.UUID;

import io.spacedog.utils.Credentials;
import io.spacedog.utils.Uris;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;

@Prefix("/1/share")
public class ShareResource extends S3Resource {

	private static final String SHARE_BUCKET_SUFFIX = "shared";

	//
	// Routes
	//

	@Get("")
	@Get("/")
	public Object getAll(Context context) {
		Credentials credentials = SpaceContext.checkAdminCredentials();
		return doList(SHARE_BUCKET_SUFFIX, credentials.backendId(), Uris.ROOT, context);
	}

	@Get("/:uuid/:fileName")
	@Get("/:uuid/:fileName/")
	public Payload get(String uuid, String fileName, Context context) {
		// TODO better check ACL
		Credentials credentials = SpaceContext.checkCredentials();
		Optional<Payload> payload = doGet(SHARE_BUCKET_SUFFIX, credentials.backendId(), //
				Uris.toPath(uuid, fileName), context);
		return payload.isPresent() ? payload.get() : JsonPayload.error(HttpStatus.NOT_FOUND);
	}

	@Put("/:fileName")
	@Put("/:fileName/")
	public Payload put(String fileName, byte[] bytes, Context context) {
		Credentials credentials = SpaceContext.checkUserCredentials();
		String uuid = UUID.randomUUID().toString();
		return doUpload(SHARE_BUCKET_SUFFIX, "/1/share", credentials, Uris.toPath(uuid, fileName), bytes, context);
	}

	@Delete("")
	@Delete("/")
	public Payload deleteAll() {
		Credentials credentials = SpaceContext.checkAdminCredentials();
		return doDelete(SHARE_BUCKET_SUFFIX, credentials, Uris.ROOT);
	}

	@Delete("/:uuid/:fileName")
	@Delete("/:uuid/:fileName/")
	public Payload delete(String uuid, String fileName, Context context) {
		Credentials credentials = SpaceContext.checkUserOrAdminCredentials();
		return doDelete(SHARE_BUCKET_SUFFIX, credentials, Uris.toPath(uuid, fileName));
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
