/**
 * © David Attias 2015
 */
package io.spacedog.services;

import java.util.UUID;

import io.spacedog.model.DataPermission;
import io.spacedog.model.ShareSettings;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.WebPath;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
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
		checkPermission(DataPermission.search);
		return doList(SHARE_BUCKET_SUFFIX, WebPath.ROOT, context);
	}

	@Get("/:uuid/:fileName")
	@Get("/:uuid/:fileName/")
	public Payload get(String uuid, String fileName, Context context) {

		boolean checkOwnership = checkPermissionAndIsOwnershipRequired(//
				DataPermission.read, DataPermission.read_all);

		return doGet(SHARE_BUCKET_SUFFIX, WebPath.newPath(uuid, fileName), //
				context, checkOwnership);
	}

	@Post("/:fileName")
	@Post("/:fileName/")
	// deprecated since not idempotent
	@Put("/:fileName")
	@Put("/:fileName/")
	public Payload post(String fileName, byte[] bytes, Context context) {
		Credentials credentials = checkPermission(DataPermission.create);
		ShareSettings settings = shareSettings();
		String uuid = UUID.randomUUID().toString();
		return doUpload(SHARE_BUCKET_SUFFIX, "/1/share", credentials, //
				WebPath.newPath(uuid, fileName), bytes, context, settings.enableS3Location);
	}

	@Delete("")
	@Delete("/")
	public Payload deleteAll() {
		checkPermission(DataPermission.delete_all);
		return doDelete(SHARE_BUCKET_SUFFIX, WebPath.ROOT, false, false);
	}

	@Delete("/:uuid/:fileName")
	@Delete("/:uuid/:fileName/")
	public Payload delete(String uuid, String fileName, Context context) {
		boolean checkOwnership = checkPermissionAndIsOwnershipRequired(//
				DataPermission.delete, DataPermission.delete_all);

		return doDelete(SHARE_BUCKET_SUFFIX, WebPath.newPath(uuid, fileName), //
				true, checkOwnership);
	}

	//
	// Implementation
	//

	private Credentials checkPermission(DataPermission... permissions) {
		Credentials credentials = SpaceContext.credentials();
		ShareSettings settings = shareSettings();

		if (settings.check(credentials, permissions))
			return credentials;

		throw Exceptions.insufficientCredentials(credentials);
	}

	private boolean checkPermissionAndIsOwnershipRequired(//
			DataPermission ownerchipRequiredPermission, //
			DataPermission ownerchipNotRequiredPermission) {

		Credentials credentials = SpaceContext.credentials();
		ShareSettings settings = shareSettings();

		if (settings.check(credentials, ownerchipNotRequiredPermission))
			return false;

		if (settings.check(credentials, ownerchipRequiredPermission))
			return true;

		throw Exceptions.insufficientCredentials(credentials);
	}

	private ShareSettings shareSettings() {
		return SettingsResource.get().getAsObject(ShareSettings.class);
	}

	//
	// singleton
	//

	private static ShareResource singleton = new ShareResource();

	static ShareResource get() {
		return singleton;
	}

	private ShareResource() {
		SettingsResource.get().registerSettings(ShareSettings.class);
	}
}
