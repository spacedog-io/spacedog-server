/**
 * © David Attias 2015
 */
package io.spacedog.server;

import java.util.UUID;

import com.google.common.base.Strings;

import io.spacedog.model.Permission;
import io.spacedog.model.ShareSettings;
import io.spacedog.model.ZipRequest;
import io.spacedog.utils.Credentials;
import io.spacedog.utils.Exceptions;
import io.spacedog.utils.Json;
import io.spacedog.utils.WebPath;
import net.codestory.http.Context;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.payload.Payload;

@Prefix("/1/shares")
public class ShareService extends S3Service {

	private static final String SHARE_BUCKET_SUFFIX = "shared";

	//
	// Routes
	//

	@Get("")
	@Get("/")
	public Object getAll(Context context) {
		checkPermission(Permission.search);
		return doList(SHARE_BUCKET_SUFFIX, "/1/shares", WebPath.ROOT, context);
	}

	@Post("")
	@Post("/")
	public Payload post(byte[] bytes, Context context) {
		Credentials credentials = checkPermission(Permission.create);
		String fileName = context.get("fileName");
		ShareSettings settings = shareSettings();
		String id = UUID.randomUUID().toString();
		if (!Strings.isNullOrEmpty(fileName))
			id = id + '-' + fileName;
		return doUpload(SHARE_BUCKET_SUFFIX, "/1/shares", credentials, //
				WebPath.newPath(id), bytes, fileName, settings.enableS3Location, context);
	}

	@Delete("")
	@Delete("/")
	public Payload deleteAll() {
		checkPermission(Permission.deleteAll);
		return doDelete(SHARE_BUCKET_SUFFIX, WebPath.ROOT, false);
	}

	@Post("/zip")
	@Post("/zip/")
	public Payload postZip(String body, Context context) {
		ZipRequest request = Json.toPojo(body, ZipRequest.class);
		return doZip(SHARE_BUCKET_SUFFIX, request, shareSettings().sharePermissions);
	}

	@Get("/:id")
	@Get("/:id/")
	public Payload get(String id, Context context) {

		boolean checkOwnership = checkPermissionAndIsOwnershipRequired(//
				Permission.readMine, Permission.readAll);

		return doGet(SHARE_BUCKET_SUFFIX, WebPath.newPath(id), //
				context, checkOwnership);
	}

	@Delete("/:id")
	@Delete("/:id/")
	public Payload delete(String id, Context context) {
		boolean checkOwnership = checkPermissionAndIsOwnershipRequired(//
				Permission.deleteMine, Permission.deleteAll);

		return doDelete(SHARE_BUCKET_SUFFIX, WebPath.newPath(id), //
				checkOwnership);
	}

	//
	// Implementation
	//

	private Credentials checkPermission(Permission... permissions) {
		Credentials credentials = SpaceContext.credentials();
		ShareSettings settings = shareSettings();

		if (settings.sharePermissions.check(credentials, permissions))
			return credentials;

		throw Exceptions.insufficientCredentials(credentials);
	}

	private boolean checkPermissionAndIsOwnershipRequired(//
			Permission ownerchipRequiredPermission, //
			Permission ownerchipNotRequiredPermission) {

		Credentials credentials = SpaceContext.credentials();
		ShareSettings settings = shareSettings();

		if (settings.sharePermissions.check(credentials, ownerchipNotRequiredPermission))
			return false;

		if (settings.sharePermissions.check(credentials, ownerchipRequiredPermission))
			return true;

		throw Exceptions.insufficientCredentials(credentials);
	}

	private ShareSettings shareSettings() {
		return SettingsService.get().getAsObject(ShareSettings.class);
	}

	//
	// singleton
	//

	private static ShareService singleton = new ShareService();

	static ShareService get() {
		return singleton;
	}

	private ShareService() {
		SettingsService.get().registerSettings(ShareSettings.class);
	}
}
